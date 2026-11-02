package com.example.ecommerce.order;

import com.example.ecommerce.order.service.OrderService;
import com.example.ecommerce.order.dto.OrderDtos.*;
import com.example.ecommerce.payment.model.Payments;
import jakarta.persistence.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties={"app.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=","logging.level.org.springframework.security=WARN"})
@Transactional
class OrderLifecycleIntegrationTests {
    @Autowired OrderService service;
    @Autowired com.example.ecommerce.payment.service.PaymentService paymentService;
    @Autowired JdbcTemplate jdbc;
    @PersistenceContext EntityManager em;
    OrderFixtures.Fixture f;
    @BeforeEach void setup(){f=OrderFixtures.create(em,jdbc);}
    @Test void historyAndDetailsAreScopedToTheCustomer() {
        assertEquals(1,service.customerOrders(f.owner().getId(),0,20).totalElements());
        assertEquals(0,service.customerOrders(f.other().getId(),0,20).totalElements());
        assertEquals(f.order().getId(),service.customerDetail(f.owner().getId(),f.order().getId()).id());
        assertEquals(404,assertThrows(ResponseStatusException.class,()->service.customerDetail(f.other().getId(),f.order().getId())).getStatusCode().value());
        assertEquals(404,assertThrows(ResponseStatusException.class,()->service.requestCancellation(f.other().getId(),f.order().getId(),"Test")).getStatusCode().value());
    }
    @Test void cancellationIsRequestedThenApprovedAndReleasesStockOnlyOnce() {
        var details=service.requestCancellation(f.owner().getId(),f.order().getId(),"Changed my mind");
        assertEquals("CANCELLATION_REQUESTED",details.status());assertEquals(1,f.inventory().getQuantityReserved());
        details=service.changeStatus(f.other().getId(),f.order().getId(),new StatusRequest(Status.CANCELLED,"Approved"));
        assertEquals("CANCELLED",details.status());assertEquals(0,f.inventory().getQuantityReserved());assertEquals(5,f.inventory().getQuantityOnHand());
        assertEquals(2,details.statusHistory().size());
        service.changeStatus(f.other().getId(),f.order().getId(),new StatusRequest(Status.CANCELLED,null));
        assertEquals(0,f.inventory().getQuantityReserved());
        assertEquals(1,jdbc.queryForObject("select count(*) from inventory_movements where order_id=?",Integer.class,f.order().getId()));
    }
    @Test void shipmentConsumesTheReservationOnlyOnce() {
        f.order().setStatus("PAID");em.flush();
        service.changeStatus(f.other().getId(),f.order().getId(),new StatusRequest(Status.PROCESSING,null));
        service.changeStatus(f.other().getId(),f.order().getId(),new StatusRequest(Status.SHIPPED,null));
        assertEquals(0,f.inventory().getQuantityReserved());assertEquals(4,f.inventory().getQuantityOnHand());
        service.changeStatus(f.other().getId(),f.order().getId(),new StatusRequest(Status.SHIPPED,null));
        service.changeStatus(f.other().getId(),f.order().getId(),new StatusRequest(Status.DELIVERED,null));
        assertEquals(4,f.inventory().getQuantityOnHand());
        assertThrows(ResponseStatusException.class,()->service.requestCancellation(f.owner().getId(),f.order().getId(),"Too late"));
    }
    @Test void rejectsSkippedTransitionsAndForgedPaidStatus() {
        assertEquals(409,assertThrows(ResponseStatusException.class,()->service.changeStatus(f.other().getId(),f.order().getId(),new StatusRequest(Status.PAID,null))).getStatusCode().value());
        assertEquals(409,assertThrows(ResponseStatusException.class,()->service.changeStatus(f.other().getId(),f.order().getId(),new StatusRequest(Status.SHIPPED,null))).getStatusCode().value());
        assertEquals(1,f.inventory().getQuantityReserved());
    }
    @Test void activePaymentsPreventReleasingStockAndRequestsCanBeDeclined() {
        var p=new Payments();p.setOrder(f.order());p.setMethod("ONLINE");p.setStatus("PENDING");p.setAmount(f.order().getTotalAmount());em.persist(p);em.flush();
        service.requestCancellation(f.owner().getId(),f.order().getId(),"Please cancel");
        assertThrows(ResponseStatusException.class,()->service.changeStatus(f.other().getId(),f.order().getId(),new StatusRequest(Status.CANCELLED,null)));
        assertEquals(1,f.inventory().getQuantityReserved());
        assertEquals("PENDING",service.changeStatus(f.other().getId(),f.order().getId(),new StatusRequest(Status.PENDING,"Declined")).status());
    }
    @Test void paymentRoutesFailClosedWhenProviderIsNotConfigured() {
        assertEquals(503,assertThrows(ResponseStatusException.class,()->paymentService.checkout(f.owner().getId(),f.order().getId())).getStatusCode().value());
        assertEquals(503,assertThrows(ResponseStatusException.class,()->paymentService.webhook(new byte[]{1},new org.springframework.http.HttpHeaders())).getStatusCode().value());
        assertEquals("PENDING",f.order().getStatus());
    }
}
