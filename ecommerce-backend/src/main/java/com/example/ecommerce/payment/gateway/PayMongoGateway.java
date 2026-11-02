package com.example.ecommerce.payment.gateway;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class PayMongoGateway implements PaymentGateway {
    private final RestClient client;
    private final ObjectMapper json;
    private final String secretKey,webhookSecret,successUrl,cancelUrl;
    private final boolean liveMode;
    private final List<String> methods;
    private final Clock clock;
    private final long tolerance;
    @org.springframework.beans.factory.annotation.Autowired
    public PayMongoGateway(ObjectMapper json,
            @Value("${app.paymongo.secret-key:}") String secretKey,
            @Value("${app.paymongo.webhook-secret:}") String webhookSecret,
            @Value("${app.paymongo.live-mode:false}") boolean liveMode,
            @Value("${app.paymongo.success-url:http://localhost:4200/account?payment=success}") String successUrl,
            @Value("${app.paymongo.cancel-url:http://localhost:4200/account?payment=cancelled}") String cancelUrl,
            @Value("${app.paymongo.payment-methods:gcash,card}") String methods,
            @Value("${app.paymongo.signature-tolerance-seconds:300}") long tolerance) {
        this(defaultClient(secretKey),json,secretKey,webhookSecret,liveMode,successUrl,cancelUrl,methods,Clock.systemUTC(),tolerance);
    }
    // Package-private transport/clock injection keeps tests offline and deterministic.
    PayMongoGateway(RestClient client,ObjectMapper json,String secretKey,String webhookSecret,boolean liveMode,
            String successUrl,String cancelUrl,String methods,Clock clock,long tolerance) {
        this.client=client;this.json=json;this.secretKey=secretKey;this.webhookSecret=webhookSecret;this.liveMode=liveMode;
        this.successUrl=successUrl;this.cancelUrl=cancelUrl;this.methods=Arrays.stream(methods.split(",")).map(String::trim).filter(s->!s.isEmpty()).toList();
        this.clock=clock;this.tolerance=tolerance;
        if(tolerance<30 || tolerance>3600) throw new IllegalArgumentException("PayMongo signature tolerance must be 30-3600 seconds");
    }
    private static RestClient defaultClient(String key) {
        var factory=new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
        factory.setReadTimeout(Duration.ofSeconds(20));
        return RestClient.builder().baseUrl("https://api.paymongo.com").requestFactory(factory)
            .defaultHeaders(h->h.setBasicAuth(key,"",StandardCharsets.UTF_8)).build();
    }
    @Override public String name(){return "PAYMONGO";}
    private void configured() {
        if(secretKey.isBlank() || webhookSecret.isBlank()) throw unavailable();
        if(!secretKey.startsWith(liveMode?"sk_live_":"sk_test_")) throw unavailable();
        if(methods.isEmpty()) throw unavailable();
    }
    private ResponseStatusException unavailable(){return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"PayMongo credentials or configuration are missing");}
    private ResponseStatusException invalid(){return new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid PayMongo webhook");}
    private ResponseStatusException upstream(){return new ResponseStatusException(HttpStatus.BAD_GATEWAY,"PayMongo could not complete the request");}
    @Override public Session create(Checkout checkout) {
        configured();
        if(!"PHP".equals(checkout.currency())) throw new ResponseStatusException(HttpStatus.CONFLICT,"PayMongo checkout requires PHP");
        long centavos;
        try {centavos=checkout.amount().movePointRight(2).longValueExact();}catch(ArithmeticException e){throw upstream();}
        var attributes=new LinkedHashMap<String,Object>();
        attributes.put("line_items",List.of(Map.of("name","Order "+checkout.orderNumber(),"amount",centavos,"currency","PHP","quantity",1)));
        attributes.put("payment_method_types",methods);attributes.put("reference_number",checkout.orderNumber());
        attributes.put("success_url",returnUrl(successUrl,checkout.orderId()));attributes.put("cancel_url",returnUrl(cancelUrl,checkout.orderId()));
        attributes.put("metadata",Map.of("order_id",checkout.orderId().toString()));
        attributes.put("send_email_receipt",false);attributes.put("pass_on_fees",false);
        try {
            return session(client.post().uri("/v2/checkout_sessions").contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key",checkout.idempotencyKey()).body(Map.of("data",Map.of("attributes",attributes))).retrieve().body(JsonNode.class),null);
        } catch(RestClientException e){throw upstream();}
    }
    private String returnUrl(String base,Long orderId) {
        URI uri;
        try {uri=URI.create(base);}catch(IllegalArgumentException e){throw unavailable();}
        if(uri.getHost()==null || (!"https".equals(uri.getScheme()) && !("http".equals(uri.getScheme()) && Set.of("localhost","127.0.0.1").contains(uri.getHost())))) throw unavailable();
        return UriComponentsBuilder.fromUriString(base).queryParam("orderId",orderId).build().encode().toUriString();
    }
    private void reference(String ref){if(ref==null || !ref.matches("cs_[A-Za-z0-9]+")) throw upstream();}
    @Override public Session retrieve(String ref) {
        configured();reference(ref);
        try{return session(client.get().uri("/v1/checkout_sessions/{id}",ref).retrieve().body(JsonNode.class),ref);}
        catch(RestClientException e){throw upstream();}
    }
    @Override public Session expire(String ref) {
        configured();reference(ref);
        try{return session(client.post().uri("/v1/checkout_sessions/{id}/expire",ref).retrieve().body(JsonNode.class),ref);}
        catch(RestClientResponseException e){if(e.getStatusCode().value()==400)return retrieve(ref);throw upstream();}
        catch(RestClientException e){throw upstream();}
    }
    private Session session(JsonNode root,String expected) {
        if(root==null) throw upstream();var data=root.path("data");var attributes=data.path("attributes");String id=data.path("id").asString();reference(id);
        if(expected!=null && !expected.equals(id))throw upstream();
        if(attributes.has("livemode") && attributes.path("livemode").asBoolean()!=liveMode)throw upstream();
        SessionStatus status;
        if(paidPayment(attributes)!=null)status=SessionStatus.PAID;
        else if("expired".equals(attributes.path("status").asString()))status=SessionStatus.EXPIRED;
        else if("active".equals(attributes.path("status").asString()))status=SessionStatus.OPEN;
        else throw upstream();
        String url=attributes.path("checkout_url").asString("");
        if(status==SessionStatus.OPEN) {
            try {var uri=URI.create(url);if(!"https".equals(uri.getScheme()) || !"checkout.paymongo.com".equals(uri.getHost()) || uri.getUserInfo()!=null)throw upstream();}
            catch(IllegalArgumentException e){throw upstream();}
        }
        return new Session(id,url,status);
    }
    private JsonNode paidPayment(JsonNode attributes) {
        JsonNode paid=null;
        for(var payment:attributes.path("payments"))if("paid".equals(payment.path("attributes").path("status").asString())) {
            if(paid!=null)throw invalid();paid=payment.path("attributes");
        }
        return paid;
    }
    @Override public VerifiedEvent verify(byte[] raw,HttpHeaders headers) {
        if(webhookSecret.isBlank())throw unavailable();
        String header=headers.getFirst("Paymongo-Signature");if(header==null || header.length()>2048)throw invalid();
        var parts=new HashMap<String,String>();
        for(String part:header.split(",")) {String[] pair=part.trim().split("=",2);if(pair.length!=2 || parts.putIfAbsent(pair[0],pair[1])!=null)throw invalid();}
        String timestamp=parts.get("t"),signature=parts.get(liveMode?"li":"te");
        if(timestamp==null || !timestamp.matches("[0-9]{1,12}") || signature==null || !signature.matches("[0-9a-fA-F]{64}"))throw invalid();
        long seconds=Long.parseLong(timestamp),now=clock.instant().getEpochSecond();if(seconds<now-tolerance || seconds>now+tolerance)throw invalid();
        try {
            var mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));
            mac.update((timestamp+".").getBytes(StandardCharsets.UTF_8));byte[] expected=mac.doFinal(raw);
            if(!MessageDigest.isEqual(expected,HexFormat.of().parseHex(signature)))throw invalid();
        }catch(GeneralSecurityException e){throw new IllegalStateException(e);}
        JsonNode root;
        try{root=json.readTree(raw);}catch(RuntimeException e){throw invalid();}
        if(root==null || !root.isObject())throw invalid();
        var envelope=root.path("data");var event=envelope.has("attributes")?envelope.path("attributes"):envelope;
        if(!event.path("livemode").isBoolean() || event.path("livemode").asBoolean()!=liveMode)throw invalid();
        if(!"checkout_session.payment.paid".equals(event.path("type").asString()))return null;
        var resource=event.path("data");String ref=resource.path("id").asString("");
        if(!ref.matches("cs_[A-Za-z0-9]+"))throw invalid();
        var paid=paidPayment(resource.path("attributes"));
        if(paid==null || !paid.path("amount").isIntegralNumber() || !paid.path("amount").canConvertToLong() || paid.path("amount").asLong()<=0)throw invalid();
        String currency=paid.path("currency").asString("");if(!"PHP".equals(currency))throw invalid();
        String eventId=envelope.path("id").asString("");
        if(eventId.isBlank()) {try{eventId="body_"+HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
        return new VerifiedEvent(eventId,ref,EventType.SUCCEEDED,BigDecimal.valueOf(paid.path("amount").asLong(),2),currency);
    }
}
