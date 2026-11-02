package com.example.ecommerce.category.service;

import com.example.ecommerce.category.dto.CategoryResponse;
import com.example.ecommerce.category.repository.CategoriesRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CategoryService {
    private final CategoriesRepository categories;

    public CategoryService(CategoriesRepository categories) {
        this.categories = categories;
    }

    public List<CategoryResponse> list() {
        return categories.findByIsActiveTrueOrderByNameAscIdAsc().stream()
                .map(category -> new CategoryResponse(category.getId(), category.getName(),
                        category.getSlug(), category.getDescription(),
                        category.getParent() == null ? null : category.getParent().getId()))
                .toList();
    }
}
