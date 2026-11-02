package com.example.ecommerce.product;

import com.example.ecommerce.product.service.ProductService;
import static com.example.ecommerce.product.dto.ProductDtos.*;
import com.example.ecommerce.product.model.Products;
import com.example.ecommerce.category.model.Categories;
import com.example.ecommerce.product.repository.ProductsRepository;
import com.example.ecommerce.product.repository.BrandsRepository;
import com.example.ecommerce.category.repository.CategoriesRepository;
import java.util.Optional;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class CatalogServiceTests {
    final ProductsRepository products=mock(ProductsRepository.class);
    final CategoriesRepository categories=mock(CategoriesRepository.class);
    final BrandsRepository brands=mock(BrandsRepository.class);
    final ProductService service=new ProductService(products,categories,brands);
    Products product() { var c=new Categories(); c.setId(1L); c.setName("Category"); var p=new Products(); p.setId(7L); p.setCategory(c); return p; }
    @Test void hidesArchivedProductsAndInactiveCategories() {
        var p=product(); when(products.findById(7L)).thenReturn(Optional.of(p)); p.setStatus("ARCHIVED");
        assertEquals(404,assertThrows(ResponseStatusException.class,()->service.detail(7L)).getStatusCode().value());
        p.setStatus("ACTIVE"); p.getCategory().setActive(false);
        assertEquals(404,assertThrows(ResponseStatusException.class,()->service.detail(7L)).getStatusCode().value());
    }
    @Test void rejectsDuplicateSlugAndUnknownCategory() {
        when(products.findBySlug("test")).thenReturn(Optional.of(product()));
        var request=new ProductRequest("Test","test",null,1L,null,Status.ACTIVE);
        assertEquals(409,assertThrows(ResponseStatusException.class,()->service.create(request)).getStatusCode().value());
        when(products.findBySlug("test")).thenReturn(Optional.empty());
        assertEquals(400,assertThrows(ResponseStatusException.class,()->service.create(request)).getStatusCode().value());
        verify(products,never()).saveAndFlush(any());
    }
    @Test void archiveAndReactivateRetainsProduct() {
        var p=product(); when(products.findForUpdate(7L)).thenReturn(Optional.of(p));
        when(products.saveAndFlush(p)).thenReturn(p);
        assertEquals("ARCHIVED",service.status(7L,Status.ARCHIVED).status());
        assertEquals("ACTIVE",service.status(7L,Status.ACTIVE).status());
    }
    @Test void rejectsInvalidSortAndInvertedPriceRange() {
        assertThrows(ResponseStatusException.class,()->service.list(null,null,null,BigDecimal.TEN,BigDecimal.ONE,0,20,"id,asc"));
        assertThrows(ResponseStatusException.class,()->service.list(null,null,null,null,null,0,20,"passwordHash,asc"));
        verifyNoInteractions(products);
    }
}
