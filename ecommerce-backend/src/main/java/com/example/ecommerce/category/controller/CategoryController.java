package com.example.ecommerce.category.controller;

import com.example.ecommerce.category.dto.CategoryResponse;
import com.example.ecommerce.category.service.CategoryService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CategoryController {
    private final CategoryService categories;

    public CategoryController(CategoryService categories) {
        this.categories = categories;
    }

    @GetMapping("/api/categories")
    public List<CategoryResponse> list() {
        return categories.list();
    }
}
