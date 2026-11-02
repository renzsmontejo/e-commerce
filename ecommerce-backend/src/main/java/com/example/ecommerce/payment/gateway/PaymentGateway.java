package com.example.ecommerce.payment.gateway;

import java.math.BigDecimal;
import org.springframework.http.HttpHeaders;

/** Provider adapter: authenticate callbacks before returning any event to the application. */
public interface PaymentGateway {
    enum SessionStatus { OPEN, PAID, EXPIRED }
    enum EventType { SUCCEEDED, FAILED, EXPIRED }
    record Checkout(Long orderId, String orderNumber, BigDecimal amount, String currency, String idempotencyKey) {}
    record Session(String reference, String checkoutUrl, SessionStatus status) {}
    record VerifiedEvent(String eventId, String sessionReference, EventType type, BigDecimal amount, String currency) {}
    String name();
    /** Must use the provider's idempotency mechanism to avoid duplicate remote sessions on retries. */
    Session create(Checkout checkout);
    Session retrieve(String reference);
    Session expire(String reference);
    /** Verify the signature and freshness using the exact raw bytes before parsing. Null means a verified but unsupported event. */
    VerifiedEvent verify(byte[] rawBody, HttpHeaders headers);
}
