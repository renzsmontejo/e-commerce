package com.example.ecommerce.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.*;
import io.swagger.v3.oas.models.PathItem;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.*;
import java.util.*;

@Configuration
public class OpenApiConfig {
    @Bean public OpenAPI ecommerceApi() {
        return new OpenAPI().servers(List.of(new io.swagger.v3.oas.models.servers.Server().url("/"))).info(new Info().title("E-commerce API").version("1.0")
                .description("Catalog, authentication, cart, order management, and PayMongo payments."))
            .components(new Components().addSecuritySchemes("bearerAuth",new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
    @Bean public OpenApiCustomizer publicOperations() {
        return api -> api.getPaths().forEach((path,item)->item.readOperationsMap().forEach((method,operation)->{
            boolean publicAuth=Set.of("/api/auth/login","/api/auth/register","/api/auth/forgot-password","/api/auth/reset-password").contains(path);
            boolean catalog=method==PathItem.HttpMethod.GET && (path.startsWith("/api/products") || path.equals("/api/categories"));
            if(publicAuth || catalog || path.equals("/api/webhooks/payments")) operation.setSecurity(List.of());
        }));
    }
}
