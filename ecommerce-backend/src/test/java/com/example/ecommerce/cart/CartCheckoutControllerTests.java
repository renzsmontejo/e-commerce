package com.example.ecommerce.cart;

import com.example.ecommerce.cart.controller.CartController;
import com.example.ecommerce.cart.service.CartService;
import com.example.ecommerce.cart.dto.CartDtos.*;
import com.example.ecommerce.order.controller.CheckoutController;
import com.example.ecommerce.order.service.CheckoutService;
import com.example.ecommerce.config.SecurityConfig;
import com.example.ecommerce.security.*;
import com.example.ecommerce.auth.service.TokenService;
import com.example.ecommerce.user.model.*;
import com.example.ecommerce.user.repository.UsersRepository;
import java.util.*;
import java.math.BigDecimal;
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

@WebMvcTest(controllers={CartController.class,CheckoutController.class})
@Import({SecurityConfig.class,JwtAuthenticationFilter.class,JwtService.class,com.example.ecommerce.product.storage.ProductImageStorage.class})
@TestPropertySource(properties="app.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
class CartCheckoutControllerTests {
    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;
    @MockitoBean UsersRepository users;
    @MockitoBean TokenService tokens;
    @MockitoBean CartService carts;
    @MockitoBean CheckoutService checkout;
    String bearer;
    @BeforeEach void setup() {
        var user=new Users(); user.setId(7L); user.setStatus("ACTIVE");
        var role=new Roles(); role.setName("CUSTOMER"); user.setRoles(Set.of(role));
        when(users.findWithRolesById(7L)).thenReturn(Optional.of(user));
        when(tokens.isActive(anyString())).thenReturn(true); bearer="Bearer "+jwt.generateToken(user);
        when(carts.get(7L)).thenReturn(new CartResponse(null,List.of(),0,new BigDecimal("0.00"),"PHP"));
    }
    @Test void requiresAuthenticationForAllSixRoutes() throws Exception {
        mvc.perform(get("/api/cart")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/cart/items").contentType("application/json").content("{}")).andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/cart/items/1").contentType("application/json").content("{}")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/cart/items/1")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/checkout/preview")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/orders").contentType("application/json").content("{}")).andExpect(status().isUnauthorized());
        verifyNoInteractions(carts,checkout);
    }
    @Test void customerCanReadAndMutateOnlyTheirOwnCart() throws Exception {
        mvc.perform(get("/api/cart").header("Authorization",bearer)).andExpect(status().isOk()).andExpect(jsonPath("$.items").isArray());
        mvc.perform(post("/api/cart/items").header("Authorization",bearer).contentType("application/json").content("{\"variantId\":2,\"quantity\":3,\"userId\":99}"))
            .andExpect(status().isCreated());
        verify(carts).add(eq(7L),argThat(r->r.variantId()==2L && r.quantity()==3));
        mvc.perform(patch("/api/cart/items/1").header("Authorization",bearer).contentType("application/json").content("{\"quantity\":2}"))
            .andExpect(status().isOk()); verify(carts).change(7L,1L,2);
        mvc.perform(delete("/api/cart/items/1").header("Authorization",bearer)).andExpect(status().isNoContent()); verify(carts).remove(7L,1L);
    }
    @Test void validatesQuantityAndCheckoutShippingAndExpectedTotal() throws Exception {
        for(String quantity:List.of("0","-1","1000","null")) {
            mvc.perform(post("/api/cart/items").header("Authorization",bearer).contentType("application/json").content("{\"variantId\":2,\"quantity\":"+quantity+"}"))
                .andExpect(status().isBadRequest());
        }
        mvc.perform(patch("/api/cart/items/-1").header("Authorization",bearer).contentType("application/json").content("{\"quantity\":1}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/orders").header("Authorization",bearer).contentType("application/json").content("{}"))
            .andExpect(status().isBadRequest()); verifyNoInteractions(checkout);
    }
    @Test void previewAndPlaceUseAuthenticatedIdentity() throws Exception {
        mvc.perform(post("/api/checkout/preview").header("Authorization",bearer)).andExpect(status().isOk()); verify(checkout).preview(7L);
        mvc.perform(post("/api/orders").header("Authorization",bearer).contentType("application/json")
            .content("{\"recipientName\":\"Test Customer\",\"shippingPhone\":\"09123456789\",\"shippingAddress\":\"Test address\",\"expectedTotal\":21.00}"))
            .andExpect(status().isCreated());
        verify(checkout).place(eq(7L),argThat(r->r.expectedTotal().compareTo(new BigDecimal("21.00"))==0));
    }
}
