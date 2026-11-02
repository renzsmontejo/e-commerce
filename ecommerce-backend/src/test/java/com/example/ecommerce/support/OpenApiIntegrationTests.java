package com.example.ecommerce.support;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiIntegrationTests extends MySqlIntegrationTest {
    @Autowired MockMvc mvc;
    @Test void documentsBearerSecurityPublicRoutesAndPayMongoSignature() throws Exception {
        mvc.perform(get("/api/docs")).andExpect(status().isOk())
            .andExpect(jsonPath("$.info.title").value("E-commerce API"))
            .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
            .andExpect(jsonPath("$.paths['/api/auth/login'].post.security").isEmpty())
            .andExpect(jsonPath("$.paths['/api/products'].get.security").isEmpty())
            .andExpect(jsonPath("$.paths['/api/orders'].get").exists())
            .andExpect(jsonPath("$.paths['/api/admin/orders/{id}/status'].patch").exists())
            .andExpect(jsonPath("$.paths['/api/webhooks/payments'].post.parameters[0].name").value("Paymongo-Signature"))
            .andExpect(jsonPath("$.servers[0].url").value("/"));
    }
    @Test void servesSwaggerUiWithoutWeakeningProtectedApiRoutes() throws Exception {
        String ui=mvc.perform(get("/api/swagger-ui")).andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
        mvc.perform(get(ui)).andExpect(status().isOk());
        mvc.perform(get("/api/orders")).andExpect(status().isUnauthorized());
    }
}
