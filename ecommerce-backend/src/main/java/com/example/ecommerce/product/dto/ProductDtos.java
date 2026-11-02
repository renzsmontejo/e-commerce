package com.example.ecommerce.product.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public final class ProductDtos {
    private ProductDtos() {}
    public enum Status { ACTIVE, ARCHIVED }
    public record ProductRequest(
        @NotBlank @Size(max=200) String name,
        @NotBlank @Size(max=255) @Pattern(regexp="[a-z0-9]+(?:-[a-z0-9]+)*") String slug,
        @Size(max=65535) String description,
        @NotNull @Positive Long categoryId,
        @Positive Long brandId,
        @NotNull Status status) {}
    public record StatusRequest(@NotNull Status status) {}
    public record ImageResponse(Long id, String url, String altText, boolean primary, Integer sortOrder) {}
    public record VariantResponse(Long id, String sku, BigDecimal price, BigDecimal compareAtPrice, Integer weightGrams) {}
    public record ProductResponse(Long id, String name, String slug, String description, String status,
        Long categoryId, String categoryName, Long brandId, String brandName,
        List<ImageResponse> images, List<VariantResponse> variants) {}
    public record ProductPage(List<ProductResponse> content, int page, int size, long totalElements, int totalPages) {}
}
