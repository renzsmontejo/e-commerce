package com.example.ecommerce.product.controller;

import com.example.ecommerce.product.service.ProductService;
import com.example.ecommerce.product.service.ProductImageService;

import static com.example.ecommerce.product.dto.ProductDtos.*;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Validated
public class ProductController {
    private final ProductService catalog;
    private final ProductImageService images;
    public ProductController(ProductService catalog, ProductImageService images) { this.catalog=catalog; this.images=images; }
    @GetMapping("/api/products")
    public ProductPage products(@RequestParam(required=false) @Size(max=200) String q,
        @RequestParam(required=false) @Positive Long categoryId, @RequestParam(required=false) @Positive Long brandId,
        @RequestParam(required=false) @DecimalMin("0") BigDecimal minPrice,
        @RequestParam(required=false) @DecimalMin("0") BigDecimal maxPrice,
        @RequestParam(defaultValue="0") @Min(0) int page,
        @RequestParam(defaultValue="20") @Min(1) @Max(100) int size,
        @RequestParam(defaultValue="createdAt,desc") String sort) {
        return catalog.list(q,categoryId,brandId,minPrice,maxPrice,page,size,sort);
    }
    @GetMapping("/api/products/{id}")
    public ProductResponse detail(@PathVariable @Positive Long id) { return catalog.detail(id); }
    @PostMapping("/api/admin/products")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        var result=catalog.create(request);
        return ResponseEntity.created(URI.create("/api/products/"+result.id())).body(result);
    }
    @PutMapping("/api/admin/products/{id}")
    public ProductResponse update(@PathVariable @Positive Long id, @Valid @RequestBody ProductRequest request) { return catalog.update(id,request); }
    @PatchMapping("/api/admin/products/{id}/status")
    public ProductResponse status(@PathVariable @Positive Long id, @Valid @RequestBody StatusRequest request) { return catalog.status(id,request.status()); }
    @PostMapping(value="/api/admin/products/{id}/images",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<ImageResponse>> upload(@PathVariable @Positive Long id, @RequestParam("files") List<MultipartFile> files) {
        return ResponseEntity.status(HttpStatus.CREATED).body(images.upload(id,files));
    }
}
