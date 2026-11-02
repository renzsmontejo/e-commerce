package com.example.ecommerce.payment.service;

import com.example.ecommerce.payment.gateway.PaymentGateway;
import com.example.ecommerce.payment.gateway.PaymentGateway.SessionStatus;
import com.example.ecommerce.payment.repository.PaymentsRepository;
import jakarta.persistence.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import static com.example.ecommerce.cart.service.CartService.conflict;

@Service
public class PaymentCancellationService {
    @PersistenceContext private EntityManager em;
    private final PaymentsRepository payments;
    private final ObjectProvider<PaymentGateway> gateways;
    public PaymentCancellationService(PaymentsRepository payments,ObjectProvider<PaymentGateway> gateways){this.payments=payments;this.gateways=gateways;}
    // Called inside OrderService's transaction while its order lock is held.
    public void prepare(Long orderId) {
        for(var payment:payments.findByOrder_IdOrderByIdAsc(orderId)) {
            em.refresh(payment,LockModeType.PESSIMISTIC_WRITE);
            if(java.util.Set.of("PAID","SUCCEEDED","PAID_REVIEW_REQUIRED").contains(payment.getStatus()))throw conflict("Paid orders require a refund before cancellation");
            if(!"PENDING".equals(payment.getStatus()))continue;
            var gateway=gateways.getIfAvailable();
            if(gateway==null || !gateway.name().equals(payment.getProvider()))throw conflict("Pending payment cannot be safely expired");
            var session=gateway.retrieve(payment.getProviderReference());
            if(session.status()==SessionStatus.OPEN)session=gateway.expire(payment.getProviderReference());
            if(session.status()!=SessionStatus.EXPIRED)throw conflict("Payment is paid or processing; cancellation cannot proceed");
            payment.setStatus("EXPIRED");
        }
    }
}
