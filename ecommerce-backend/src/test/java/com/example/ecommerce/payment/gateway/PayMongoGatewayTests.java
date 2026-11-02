package com.example.ecommerce.payment.gateway;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import com.example.ecommerce.payment.gateway.PaymentGateway.*;

class PayMongoGatewayTests {
    static final long NOW=1793606400L;
    static final String SECRET="test-endpoint-signing-secret";
    MockRestServiceServer server;
    PayMongoGateway gateway;
    @BeforeEach void setup() {
        var builder=RestClient.builder().baseUrl("https://api.paymongo.com").defaultHeaders(h->h.setBasicAuth("sk_test_mock",""));
        server=MockRestServiceServer.bindTo(builder).build();
        gateway=new PayMongoGateway(builder.build(),JsonMapper.builder().build(),"sk_test_mock",SECRET,false,
            "http://localhost:4200/account?payment=success","http://localhost:4200/account?payment=cancelled","gcash,card",Clock.fixed(Instant.ofEpochSecond(NOW),ZoneOffset.UTC),300);
    }
    String response(String status,String payments) {
        return "{\"data\":{\"id\":\"cs_test123\",\"attributes\":{\"livemode\":false,\"status\":\""+status+"\",\"checkout_url\":\"https://checkout.paymongo.com/cs_test123\",\"payments\":"+payments+"}}}";
    }
    String paid() {return "[{\"id\":\"pay_123\",\"attributes\":{\"status\":\"paid\",\"amount\":1050,\"currency\":\"PHP\"}}]";}
    String event() {return "{\"data\":{\"id\":\"evt_123\",\"type\":\"event\",\"attributes\":{\"type\":\"checkout_session.payment.paid\",\"livemode\":false,\"data\":{"+
        "\"id\":\"cs_test123\",\"attributes\":{\"payments\":"+paid()+"}}}}}";}
    HttpHeaders headers(byte[] raw,long timestamp,String slot) throws Exception {
        var mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));
        mac.update((timestamp+".").getBytes(StandardCharsets.UTF_8));String signature=HexFormat.of().formatHex(mac.doFinal(raw));
        var h=new HttpHeaders();h.set("Paymongo-Signature","t="+timestamp+","+slot+"="+signature);return h;
    }
    @Test void createsV2CheckoutUsingCentavosAndIdempotencyHeader() {
        server.expect(requestTo("https://api.paymongo.com/v2/checkout_sessions")).andExpect(method(HttpMethod.POST))
            .andExpect(header("Idempotency-Key","checkout-7-1"))
            .andExpect(header("Authorization","Basic "+Base64.getEncoder().encodeToString("sk_test_mock:".getBytes(StandardCharsets.UTF_8))))
            .andExpect(jsonPath("$.data.attributes.line_items[0].amount").value(1050))
            .andExpect(jsonPath("$.data.attributes.pass_on_fees").value(false))
            .andExpect(jsonPath("$.data.attributes.reference_number").value("ORD-7"))
            .andExpect(jsonPath("$.data.attributes.success_url").value("http://localhost:4200/account?payment=success&orderId=7"))
            .andRespond(withSuccess(response("active","[]"),MediaType.APPLICATION_JSON));
        var result=gateway.create(new Checkout(7L,"ORD-7",new BigDecimal("10.50"),"PHP","checkout-7-1"));
        assertEquals("cs_test123",result.reference());assertEquals(SessionStatus.OPEN,result.status());server.verify();
    }
    @Test void retrievesPaidSessionWithoutMistakingActiveForUnpaid() {
        server.expect(requestTo("https://api.paymongo.com/v1/checkout_sessions/cs_test123"))
            .andRespond(withSuccess(response("active",paid()),MediaType.APPLICATION_JSON));
        assertEquals(SessionStatus.PAID,gateway.retrieve("cs_test123").status());server.verify();
    }
    @Test void expiresSessionAndChecksCurrentStateWhenProviderRefuses() {
        server.expect(requestTo("https://api.paymongo.com/v1/checkout_sessions/cs_test123/expire")).andExpect(method(HttpMethod.POST)).andRespond(withBadRequest());
        server.expect(requestTo("https://api.paymongo.com/v1/checkout_sessions/cs_test123"))
            .andRespond(withSuccess(response("active",paid()),MediaType.APPLICATION_JSON));
        assertEquals(SessionStatus.PAID,gateway.expire("cs_test123").status());server.verify();
    }
    @Test void authenticatesExactRawBodyAndExtractsPaidPayment() throws Exception {
        byte[] raw=event().getBytes(StandardCharsets.UTF_8);var result=gateway.verify(raw,headers(raw,NOW,"te"));
        assertEquals("evt_123",result.eventId());assertEquals("cs_test123",result.sessionReference());
        assertEquals(new BigDecimal("10.50"),result.amount());assertEquals(EventType.SUCCEEDED,result.type());
        assertThrows(ResponseStatusException.class,()->gateway.verify((event()+" ").getBytes(StandardCharsets.UTF_8),headers(raw,NOW,"te")));
    }
    @Test void rejectsMissingInvalidOldFutureAndWrongModeSignatures() throws Exception {
        byte[] raw=event().getBytes(StandardCharsets.UTF_8);
        assertThrows(ResponseStatusException.class,()->gateway.verify(raw,new HttpHeaders()));
        assertThrows(ResponseStatusException.class,()->gateway.verify(raw,headers(raw,NOW-301,"te")));
        assertThrows(ResponseStatusException.class,()->gateway.verify(raw,headers(raw,NOW+301,"te")));
        assertThrows(ResponseStatusException.class,()->gateway.verify(raw,headers(raw,NOW,"li")));
        byte[] live=event().replace("\"livemode\":false","\"livemode\":true").getBytes(StandardCharsets.UTF_8);
        assertThrows(ResponseStatusException.class,()->gateway.verify(live,headers(live,NOW,"te")));
    }
    @Test void acceptsCurrentHostedCheckoutEnvelopeAndIgnoresVerifiedUnsupportedEvents() throws Exception {
        String modern="{\"event_type\":\"send.webhook\",\"data\":{\"type\":\"checkout_session.payment.paid\",\"livemode\":false,\"data\":{\"id\":\"cs_test123\",\"attributes\":{\"payments\":"+paid()+"}}}}";
        byte[] raw=modern.getBytes(StandardCharsets.UTF_8);assertTrue(gateway.verify(raw,headers(raw,NOW,"te")).eventId().startsWith("body_"));
        byte[] unknown=event().replace("checkout_session.payment.paid","payment.failed").getBytes(StandardCharsets.UTF_8);
        assertNull(gateway.verify(unknown,headers(unknown,NOW,"te")));
    }
}
