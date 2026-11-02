package com.example.ecommerce.config;

import com.example.ecommerce.product.storage.ProductImageStorage;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;
@Configuration
public class ProductImageWebConfig implements WebMvcConfigurer {
    private final ProductImageStorage storage;
    public ProductImageWebConfig(ProductImageStorage storage) { this.storage=storage; }
    @Override public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/api/product-images/**").addResourceLocations(storage.resourceLocation());
    }
}
