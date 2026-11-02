package com.example.ecommerce.product.service;

import static com.example.ecommerce.common.ApiErrors.*;

import com.example.ecommerce.product.model.Products;
import com.example.ecommerce.product.model.ProductVariants;
import com.example.ecommerce.product.model.ProductImages;
import com.example.ecommerce.product.repository.ProductsRepository;
import com.example.ecommerce.product.repository.BrandsRepository;
import com.example.ecommerce.category.repository.CategoriesRepository;
import static com.example.ecommerce.product.dto.ProductDtos.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly=true)
public class ProductService {
    private final ProductsRepository products;
    private final CategoriesRepository categories;
    private final BrandsRepository brands;
    public ProductService(ProductsRepository products, CategoriesRepository categories, BrandsRepository brands) {
        this.products=products; this.categories=categories; this.brands=brands;
    }
    public ProductPage list(String q, Long categoryId, Long brandId, BigDecimal minPrice, BigDecimal maxPrice,
                            int page, int size, String sort) {
        if (minPrice!=null && maxPrice!=null && minPrice.compareTo(maxPrice)>0) throw bad("minPrice must not exceed maxPrice");
        String[] ordering=sort.split(",",-1);
        if (ordering.length!=2 || !Set.of("name","createdAt","id").contains(ordering[0])
                || !Set.of("asc","desc").contains(ordering[1])) throw bad("sort must be name,asc; createdAt,desc; or id,asc (either direction)");
        var order=Sort.by(Sort.Direction.fromString(ordering[1]),ordering[0]);
        if (!ordering[0].equals("id")) order=order.and(Sort.by("id"));
        Specification<Products> filter=(root,query,cb) -> {
            var predicates=new ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.equal(root.get("status"),"ACTIVE"));
            predicates.add(cb.isTrue(root.get("category").get("isActive")));
            if(q!=null && !q.isBlank()) {
                String pattern="%"+q.trim().toLowerCase(Locale.ROOT).replace("\\","\\\\").replace("%","\\%").replace("_","\\_")+"%";
                predicates.add(cb.or(cb.like(cb.lower(root.get("name")),pattern,'\\'),cb.like(cb.lower(root.get("description")),pattern,'\\')));
            }
            if(categoryId!=null) predicates.add(cb.equal(root.get("category").get("id"),categoryId));
            if(brandId!=null) predicates.add(cb.equal(root.get("brand").get("id"),brandId));
            if(minPrice!=null || maxPrice!=null) {
                var sub=query.subquery(Long.class); var variant=sub.from(ProductVariants.class);
                var prices=new ArrayList<jakarta.persistence.criteria.Predicate>();
                prices.add(cb.equal(variant.get("product"),root)); prices.add(cb.isTrue(variant.get("isActive")));
                if(minPrice!=null) prices.add(cb.greaterThanOrEqualTo(variant.get("price"),minPrice));
                if(maxPrice!=null) prices.add(cb.lessThanOrEqualTo(variant.get("price"),maxPrice));
                sub.select(variant.get("id")).where(prices.toArray(jakarta.persistence.criteria.Predicate[]::new));
                predicates.add(cb.exists(sub));
            }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        var result=products.findAll(filter,PageRequest.of(page,size,order));
        return new ProductPage(result.getContent().stream().map(this::response).toList(),page,size,result.getTotalElements(),result.getTotalPages());
    }
    public ProductResponse detail(Long id) {
        var p=find(id);
        if(!"ACTIVE".equals(p.getStatus()) || !p.getCategory().isActive()) throw missing("Product");
        return response(p);
    }
    @Transactional
    public ProductResponse create(ProductRequest request) {
        var p=new Products(); apply(p,request); return response(products.saveAndFlush(p));
    }
    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        var p=products.findForUpdate(id).orElseThrow(()->missing("Product"));
        apply(p,request); return response(products.saveAndFlush(p));
    }
    @Transactional
    public ProductResponse status(Long id, Status status) {
        var p=products.findForUpdate(id).orElseThrow(()->missing("Product"));
        if(status==Status.ACTIVE && !p.getCategory().isActive()) throw bad("Choose an active category before activating this product");
        p.setStatus(status.name()); return response(products.saveAndFlush(p));
    }
    private void apply(Products p, ProductRequest r) {
        var duplicate=products.findBySlug(r.slug());
        if(duplicate.isPresent() && !Objects.equals(duplicate.get().getId(),p.getId()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Product slug already exists");
        var category=categories.findById(r.categoryId()).orElseThrow(()->bad("Category does not exist"));
        if(r.status()==Status.ACTIVE && !category.isActive()) throw bad("Active products require an active category");
        p.setCategory(category);
        p.setBrand(r.brandId()==null?null:brands.findById(r.brandId()).orElseThrow(()->bad("Brand does not exist")));
        p.setName(r.name().trim()); p.setSlug(r.slug()); p.setDescription(r.description()); p.setStatus(r.status().name());
    }
    Products find(Long id) { return products.findById(id).orElseThrow(()->missing("Product")); }
    ProductResponse response(Products p) {
        return new ProductResponse(p.getId(),p.getName(),p.getSlug(),p.getDescription(),p.getStatus(),
            p.getCategory().getId(),p.getCategory().getName(),p.getBrand()==null?null:p.getBrand().getId(),
            p.getBrand()==null?null:p.getBrand().getName(),
            p.getImages().stream().sorted(Comparator.comparing(ProductImages::getSortOrder,Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(ProductImages::getId))
                .map(ProductService::image).toList(),
            p.getVariants().stream().filter(ProductVariants::isActive).sorted(Comparator.comparing(ProductVariants::getId))
                .map(v->new VariantResponse(v.getId(),v.getSku(),v.getPrice(),v.getCompareAtPrice(),v.getWeightGrams())).toList());
    }
    public static ImageResponse image(ProductImages i) { return new ImageResponse(i.getId(),i.getImageUrl(),i.getAltText(),i.isPrimary(),i.getSortOrder()); }
}
