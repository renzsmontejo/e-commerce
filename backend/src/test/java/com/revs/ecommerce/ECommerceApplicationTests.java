package com.revs.ecommerce;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.security.SecureRandom;
import java.util.Base64;

@SpringBootTest
class ECommerceApplicationTests {

    @DynamicPropertySource
    static void jwtProperties(DynamicPropertyRegistry registry) {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        String secret = Base64.getEncoder().encodeToString(key);
        registry.add("app.jwt.secret", () -> secret);
    }

    @Test
    void contextLoads() {
    }

}
