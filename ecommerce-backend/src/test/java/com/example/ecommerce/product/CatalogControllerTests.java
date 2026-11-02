package com.example.ecommerce.product;

import com.example.ecommerce.product.service.ProductService;
import com.example.ecommerce.product.controller.ProductController;
import com.example.ecommerce.product.service.ProductImageService;
import com.example.ecommerce.product.storage.ProductImageStorage;

import com.example.ecommerce.config.SecurityConfig;
import com.example.ecommerce.security.*;
import com.example.ecommerce.auth.service.TokenService;
import com.example.ecommerce.user.repository.UsersRepository;
import com.example.ecommerce.user.model.Users;
import com.example.ecommerce.user.model.Roles;
import static com.example.ecommerce.product.dto.ProductDtos.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {ProductController.class, com.example.ecommerce.category.controller.CategoryController.class})
@Import({SecurityConfig.class,JwtAuthenticationFilter.class,JwtService.class,ProductImageStorage.class})
@TestPropertySource(properties="app.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
class CatalogControllerTests {
    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;
    @MockitoBean ProductService catalog;
    @MockitoBean com.example.ecommerce.category.service.CategoryService categories;
    @MockitoBean ProductImageService images;
    @MockitoBean UsersRepository users;
    @MockitoBean TokenService tokens;
    Users user;
    final String body="{\"name\":\"Test Product\",\"slug\":\"test-product\",\"categoryId\":1,\"status\":\"ACTIVE\"}";
    @BeforeEach void setup() {
        user=new Users(); user.setId(1L); user.setStatus("ACTIVE");
        var role=new Roles(); role.setName("ADMIN"); user.setRoles(Set.of(role));
        when(users.findWithRolesById(1L)).thenReturn(Optional.of(user));
        when(tokens.isActive(anyString())).thenReturn(true);
    }
    String bearer() { return "Bearer "+jwt.generateToken(user); }
    ProductResponse response() { return new ProductResponse(7L,"Test Product","test-product",null,"ACTIVE",1L,"Test",null,null,List.of(),List.of()); }
    @Test void catalogIsPublicAndPaginationIsBounded() throws Exception {
        when(catalog.list(null,null,null,null,null,0,20,"createdAt,desc"))
            .thenReturn(new ProductPage(List.of(),0,20,0,0));
        mvc.perform(get("/api/products")).andExpect(status().isOk()).andExpect(jsonPath("$.content").isArray());
        mvc.perform(get("/api/categories")).andExpect(status().isOk());
        mvc.perform(get("/api/products").param("size","101")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/products").param("page","-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/products").param("minPrice","-1")).andExpect(status().isBadRequest());
    }
    @Test void allAdminWritesRequireAdmin() throws Exception {
        mvc.perform(post("/api/admin/products").contentType("application/json").content(body)).andExpect(status().isUnauthorized());
        var role=new Roles(); role.setName("CUSTOMER"); user.setRoles(Set.of(role));
        mvc.perform(post("/api/admin/products").header("Authorization",bearer()).contentType("application/json").content(body)).andExpect(status().isForbidden());
        mvc.perform(put("/api/admin/products/7").header("Authorization",bearer()).contentType("application/json").content(body)).andExpect(status().isForbidden());
        mvc.perform(patch("/api/admin/products/7/status").header("Authorization",bearer()).contentType("application/json").content("{\"status\":\"ARCHIVED\"}")).andExpect(status().isForbidden());
        mvc.perform(multipart("/api/admin/products/7/images").file(new MockMultipartFile("files","x.png","image/png",new byte[]{1})).header("Authorization",bearer())).andExpect(status().isForbidden());
        verifyNoInteractions(catalog,images);
    }
    @Test void adminCanCreateUpdateArchiveAndUpload() throws Exception {
        when(catalog.create(any())).thenReturn(response()); when(catalog.update(eq(7L),any())).thenReturn(response());
        when(catalog.status(7L,Status.ARCHIVED)).thenReturn(response());
        mvc.perform(post("/api/admin/products").header("Authorization",bearer()).contentType("application/json").content(body))
            .andExpect(status().isCreated()).andExpect(header().string("Location","/api/products/7"));
        mvc.perform(put("/api/admin/products/7").header("Authorization",bearer()).contentType("application/json").content(body)).andExpect(status().isOk());
        mvc.perform(patch("/api/admin/products/7/status").header("Authorization",bearer()).contentType("application/json").content("{\"status\":\"ARCHIVED\"}")).andExpect(status().isOk());
        mvc.perform(multipart("/api/admin/products/7/images").file(new MockMultipartFile("files","x.png","image/png",new byte[]{1})).header("Authorization",bearer())).andExpect(status().isCreated());
        verify(images).upload(eq(7L),anyList());
    }
    @Test void rejectsInvalidBodyAndStatus() throws Exception {
        mvc.perform(post("/api/admin/products").header("Authorization",bearer()).contentType("application/json").content("{}" )).andExpect(status().isBadRequest());
        mvc.perform(patch("/api/admin/products/7/status").header("Authorization",bearer()).contentType("application/json").content("{\"status\":\"DELETED\"}")).andExpect(status().isBadRequest());
        verifyNoInteractions(catalog);
    }
}
