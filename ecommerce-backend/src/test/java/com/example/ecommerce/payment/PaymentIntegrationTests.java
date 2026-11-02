package com.example.ecommerce.payment;

import com.example.ecommerce.order.OrderFixtures;
import com.example.ecommerce.payment.service.PaymentService;
import com.example.ecommerce.payment.gateway.PaymentGateway;
import com.example.ecommerce.payment.gateway.PaymentGateway.*;
import com.example.ecommerce.payment.repository.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.*;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@TestPropertySource(properties={"app.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=","logging.level.org.springframework.security=WARN"})
@Transactional
class PaymentIntegrationTests {
    @Autowired PaymentService service;
    @Autowired com.example.ecommerce.order.service.OrderService orderService;
    @Autowired PaymentsRepository payments;
    @Autowired PaymentTransactionsRepository transactions;
    @Autowired JdbcTemplate jdbc;
    @PersistenceContext EntityManager em;
    @MockitoBean PaymentGateway gateway;
    OrderFixtures.Fixture f;
    String reference;
    byte[] raw="exact raw provider body".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    @BeforeEach void setup() {
        f=OrderFixtures.create(em,jdbc);reference="session-"+UUID.randomUUID();
        when(gateway.name()).thenReturn("TEST_PROVIDER");
        when(gateway.create(any())).thenReturn(new Session(reference,"https://payments.example/checkout",SessionStatus.OPEN));
        when(gateway.retrieve(reference)).thenReturn(new Session(reference,"https://payments.example/checkout",SessionStatus.OPEN));
    }
    VerifiedEvent event(String id,EventType type,String amount){return new VerifiedEvent(id,reference,type,new BigDecimal(amount),"PHP");}
    @Test void createsSessionFromServerTotalAndReusesOpenSession() {
        var response=service.checkout(f.owner().getId(),f.order().getId());
        assertEquals(response.paymentId(),service.checkout(f.owner().getId(),f.order().getId()).paymentId());
        var request=org.mockito.ArgumentCaptor.forClass(Checkout.class);verify(gateway).create(request.capture());
        assertEquals(f.order().getTotalAmount(),request.getValue().amount());assertEquals("PHP",request.getValue().currency());
        assertEquals(1,payments.countByOrder_Id(f.order().getId()));
        assertEquals(404,assertThrows(ResponseStatusException.class,()->service.checkout(f.other().getId(),f.order().getId())).getStatusCode().value());
    }
    @Test void rejectsUnverifiedEventsBeforeAnyPaymentMutation() {
        var response=service.checkout(f.owner().getId(),f.order().getId());
        when(gateway.verify(eq(raw),any())).thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST,"Bad signature"));
        assertThrows(ResponseStatusException.class,()->service.webhook(raw,new HttpHeaders()));
        assertEquals("PENDING",payments.findById(response.paymentId()).orElseThrow().getStatus());assertEquals("PENDING",f.order().getStatus());
    }
    @Test void verifiesAmountAndMakesSuccessfulDeliveryIdempotent() {
        var response=service.checkout(f.owner().getId(),f.order().getId());
        when(gateway.verify(eq(raw),any())).thenReturn(event("mismatch",EventType.SUCCEEDED,"1.00"));
        assertThrows(ResponseStatusException.class,()->service.webhook(raw,new HttpHeaders()));
        assertEquals("PENDING",f.order().getStatus());
        when(gateway.verify(eq(raw),any())).thenReturn(event("paid-event",EventType.SUCCEEDED,"10.50"));
        service.webhook(raw,new HttpHeaders());service.webhook(raw,new HttpHeaders());
        assertEquals("PAID",f.order().getStatus());assertEquals("PAID",payments.findById(response.paymentId()).orElseThrow().getStatus());
        assertEquals(1,jdbc.queryForObject("select count(*) from payment_transactions where payment_id=?",Integer.class,response.paymentId()));
        when(gateway.verify(eq(raw),any())).thenReturn(event("late-failure",EventType.FAILED,"10.50"));service.webhook(raw,new HttpHeaders());
        assertEquals("PAID",payments.findById(response.paymentId()).orElseThrow().getStatus());assertEquals("PAID",f.order().getStatus());
    }
    @Test void latePaymentForCancelledOrderRequiresReviewWithoutRestoringOrder() {
        var response=service.checkout(f.owner().getId(),f.order().getId());f.order().setStatus("CANCELLED");em.flush();
        when(gateway.verify(eq(raw),any())).thenReturn(event("late-success",EventType.SUCCEEDED,"10.50"));service.webhook(raw,new HttpHeaders());
        assertEquals("CANCELLED",f.order().getStatus());assertEquals("PAID_REVIEW_REQUIRED",payments.findById(response.paymentId()).orElseThrow().getStatus());
    }
    @Test void cancellationExpiresProviderSessionBeforeReleasingStock() {
        var response=service.checkout(f.owner().getId(),f.order().getId());
        when(gateway.expire(reference)).thenReturn(new Session(reference,"",SessionStatus.EXPIRED));
        orderService.requestCancellation(f.owner().getId(),f.order().getId(),"Please cancel");
        orderService.changeStatus(f.other().getId(),f.order().getId(),new com.example.ecommerce.order.dto.OrderDtos.StatusRequest(
                com.example.ecommerce.order.dto.OrderDtos.Status.CANCELLED,"Approved"));
        verify(gateway).expire(reference);assertEquals(0,f.inventory().getQuantityReserved());
        assertEquals("EXPIRED",payments.findById(response.paymentId()).orElseThrow().getStatus());
    }
}
