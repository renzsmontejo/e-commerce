package com.example.ecommerce.order;

import com.example.ecommerce.order.controller.OrderController;
import com.example.ecommerce.order.service.OrderService;
import com.example.ecommerce.payment.controller.PaymentController;
import com.example.ecommerce.payment.service.PaymentService;
import com.example.ecommerce.config.SecurityConfig;
import com.example.ecommerce.security.*;
import com.example.ecommerce.auth.service.TokenService;
import com.example.ecommerce.user.model.*;
import com.example.ecommerce.user.repository.UsersRepository;
import com.example.ecommerce.payment.dto.PaymentDtos.WebhookResponse;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers={OrderController.class,PaymentController.class})
@Import({SecurityConfig.class,JwtAuthenticationFilter.class,JwtService.class,com.example.ecommerce.product.storage.ProductImageStorage.class})
@TestPropertySource(properties="app.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
class OrderPaymentControllerTests {
    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;
    @MockitoBean UsersRepository users;
    @MockitoBean TokenService tokens;
    @MockitoBean OrderService orders;
    @MockitoBean PaymentService payments;
    Users user;
    @BeforeEach void setup() {
        user=new Users();user.setId(7L);user.setStatus("ACTIVE");role("CUSTOMER");
        when(users.findWithRolesById(7L)).thenReturn(Optional.of(user));when(tokens.isActive(anyString())).thenReturn(true);
    }
    void role(String name){var role=new Roles();role.setName(name);user.setRoles(Set.of(role));}
    String bearer(){return "Bearer "+jwt.generateToken(user);}
    @Test void customerRoutesRequireAuthenticationAndAdminRoutesEnforceRole() throws Exception {
        mvc.perform(get("/api/orders")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/orders/1")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/orders/1/cancel").contentType("application/json").content("{\"reason\":\"Test\"}")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/payments/checkout-session").contentType("application/json").content("{\"orderId\":1}")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/orders").header("Authorization",bearer())).andExpect(status().isForbidden());
        mvc.perform(patch("/api/admin/orders/1/status").header("Authorization",bearer()).contentType("application/json").content("{\"status\":\"CANCELLED\"}")).andExpect(status().isForbidden());
        role("ADMIN");mvc.perform(get("/api/admin/orders").header("Authorization",bearer())).andExpect(status().isOk());
    }
    @Test void bindsOwnerIdentityAndValidatesPageStatusAndReason() throws Exception {
        mvc.perform(get("/api/orders").header("Authorization",bearer())).andExpect(status().isOk());verify(orders).customerOrders(7L,0,20);
        mvc.perform(get("/api/orders/1").header("Authorization",bearer())).andExpect(status().isOk());verify(orders).customerDetail(7L,1L);
        mvc.perform(post("/api/orders/1/cancel").header("Authorization",bearer()).contentType("application/json").content("{\"reason\":\"Please cancel\"}")).andExpect(status().isOk());verify(orders).requestCancellation(7L,1L,"Please cancel");
        mvc.perform(get("/api/orders").param("size","101").header("Authorization",bearer())).andExpect(status().isBadRequest());
        mvc.perform(post("/api/orders/1/cancel").header("Authorization",bearer()).contentType("application/json").content("{}" )).andExpect(status().isBadRequest());
        role("ADMIN");mvc.perform(patch("/api/admin/orders/1/status").header("Authorization",bearer()).contentType("application/json").content("{\"status\":\"INVALID\"}" )).andExpect(status().isBadRequest());
    }
    @Test void providerWebhookIsPublicAndReceivesExactRawBody() throws Exception {
        when(payments.webhook(any(),any())).thenReturn(new WebhookResponse(true));
        String body="{ \"signature-sensitive\": true }";
        mvc.perform(post("/api/webhooks/payments").contentType("application/json").content(body)).andExpect(status().isOk());
        verify(payments).webhook(org.mockito.AdditionalMatchers.aryEq(body.getBytes(java.nio.charset.StandardCharsets.UTF_8)),any());
    }
}
