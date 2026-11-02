package com.example.ecommerce.payment.service;

import com.example.ecommerce.payment.gateway.PaymentGateway;
import com.example.ecommerce.payment.gateway.PaymentGateway.*;
import com.example.ecommerce.payment.dto.PaymentDtos.*;
import com.example.ecommerce.payment.model.*;
import com.example.ecommerce.payment.repository.*;
import com.example.ecommerce.order.service.OrderService;
import java.time.LocalDateTime;
import java.util.*;
import jakarta.persistence.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static com.example.ecommerce.common.ApiErrors.*;
import static com.example.ecommerce.cart.service.CartService.conflict;

@Service
public class PaymentService {
    @PersistenceContext private EntityManager em;
    private final ObjectProvider<PaymentGateway> gateways;
    private final OrderService orders;
    private final PaymentsRepository payments;
    private final PaymentTransactionsRepository transactions;
    public PaymentService(ObjectProvider<PaymentGateway> gateways,OrderService orders,PaymentsRepository payments,PaymentTransactionsRepository transactions) {
        this.gateways=gateways; this.orders=orders; this.payments=payments; this.transactions=transactions;
    }
    private PaymentGateway gateway() {
        var gateway=gateways.getIfAvailable();
        if(gateway==null) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Online payment provider is not configured");
        return gateway;
    }
    @Transactional
    public SessionResponse checkout(Long userId,Long orderId) {
        var order=orders.lock(orderId); orders.own(order,userId);
        if(!"PENDING".equals(order.getStatus())) throw conflict("Only pending orders can start payment");
        if(order.getTotalAmount().signum()<=0) throw conflict("Online payments require a positive total");
        var gateway=gateway();
        for(var existing:payments.findByOrder_IdOrderByIdAsc(orderId)) {
            em.refresh(existing,LockModeType.PESSIMISTIC_WRITE);
            if(Set.of("PAID","SUCCEEDED","PAID_REVIEW_REQUIRED").contains(existing.getStatus())) throw conflict("Order already has a successful payment");
            if("PENDING".equals(existing.getStatus())) {
                if(!gateway.name().equals(existing.getProvider())) throw conflict("Order has a pending payment with another provider");
                var session=gateway.retrieve(existing.getProviderReference());
                if(session.status()==SessionStatus.PAID) throw conflict("Payment confirmation is pending");
                if(session.status()==SessionStatus.OPEN) return new SessionResponse(existing.getId(),orderId,gateway.name(),session.checkoutUrl());
                existing.setStatus("EXPIRED");
            }
        }
        long attempt=payments.countByOrder_Id(orderId)+1;
        var session=gateway.create(new Checkout(orderId,order.getOrderNumber(),order.getTotalAmount(),order.getCurrency(),"checkout-"+order.getOrderNumber()+"-"+attempt));
        if(session.status()!=SessionStatus.OPEN || session.reference()==null || session.reference().isBlank() || session.reference().length()>255
                || session.checkoutUrl()==null || session.checkoutUrl().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Invalid payment provider response");
        var payment=new Payments(); payment.setOrder(order); payment.setAmount(order.getTotalAmount()); payment.setCurrency(order.getCurrency());
        payment.setMethod("ONLINE"); payment.setProvider(gateway.name()); payment.setProviderReference(session.reference()); payment.setStatus("PENDING");
        payments.saveAndFlush(payment);
        return new SessionResponse(payment.getId(),orderId,gateway.name(),session.checkoutUrl());
    }
    @Transactional
    public WebhookResponse webhook(byte[] rawBody,HttpHeaders headers) {
        if(rawBody.length>1024*1024) throw new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE);
        var gateway=gateway(); var event=gateway.verify(rawBody,headers);
        if(event==null) return new WebhookResponse(true);
        if(event.eventId()==null || event.eventId().isBlank() || event.eventId().length()>255 || event.sessionReference()==null
                || event.type()==null || event.amount()==null || event.currency()==null) throw bad("Invalid payment event");
        var payment=payments.findByProviderReference(event.sessionReference()).orElseThrow(()->conflict("Payment session is not yet recorded; retry notification"));
        var order=orders.lock(payment.getOrder().getId()); em.refresh(payment,LockModeType.PESSIMISTIC_WRITE);
        if(!gateway.name().equals(payment.getProvider()) || payment.getAmount().compareTo(event.amount())!=0
                || !payment.getCurrency().equalsIgnoreCase(event.currency()) || payment.getAmount().compareTo(order.getTotalAmount())!=0
                || !payment.getCurrency().equalsIgnoreCase(order.getCurrency())) throw bad("Payment amount, currency or provider mismatch");
        if(transactions.findEventForUpdate(payment.getId(),event.eventId()).isPresent()) return new WebhookResponse(true);
        boolean paid=Set.of("PAID","SUCCEEDED","PAID_REVIEW_REQUIRED").contains(payment.getStatus());
        if(event.type()==EventType.SUCCEEDED && !paid) {
            boolean anotherPaid=payments.findByOrder_IdOrderByIdAsc(order.getId()).stream().anyMatch(p->!p.getId().equals(payment.getId())
                && Set.of("PAID","SUCCEEDED","PAID_REVIEW_REQUIRED").contains(p.getStatus()));
            boolean review="CANCELLED".equals(order.getStatus()) || anotherPaid;
            payment.setStatus(review?"PAID_REVIEW_REQUIRED":"PAID"); payment.setPaidAt(LocalDateTime.now());
            if("PENDING".equals(order.getStatus()) && !review) orders.change(order,"PAID",null,"Verified online payment received");
        } else if(!paid && event.type()!=EventType.SUCCEEDED) payment.setStatus(event.type()==EventType.EXPIRED?"EXPIRED":"FAILED");
        var transaction=new PaymentTransactions(); transaction.setPayment(payment); transaction.setProviderTransactionId(event.eventId());
        transaction.setTransactionType(event.type().name()); transaction.setAmount(event.amount()); transaction.setStatus(event.type().name());
        transactions.save(transaction); payments.flush(); return new WebhookResponse(true);
    }
}
