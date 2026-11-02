package com.example.ecommerce.product;

import com.example.ecommerce.product.service.ProductService;
import com.example.ecommerce.product.model.Products;
import com.example.ecommerce.product.model.ProductVariants;
import com.example.ecommerce.category.model.Categories;
import com.example.ecommerce.product.repository.ProductsRepository;
import com.example.ecommerce.product.repository.ProductVariantsRepository;
import com.example.ecommerce.category.repository.CategoriesRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;
import static com.example.ecommerce.product.dto.ProductDtos.*;
@SpringBootTest
@TestPropertySource(properties={"app.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=", "logging.level.org.springframework.security=WARN"})
@Transactional
class CatalogIntegrationTests {
    @Autowired ProductService catalog;
    @Autowired com.example.ecommerce.category.service.CategoryService categoryService;
    @Autowired ProductsRepository products;
    @Autowired CategoriesRepository categories;
    @Autowired ProductVariantsRepository variants;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    String marker;
    Categories category;
    @BeforeEach void setup() {
        marker="catalog-test-"+UUID.randomUUID();
        category=new Categories(); category.setName(marker); category.setSlug(marker); categories.saveAndFlush(category);
    }
    Products product(String suffix, String price) {
        var response=catalog.create(new ProductRequest(marker+suffix,marker+suffix,null,category.getId(),null,Status.ACTIVE));
        var p=products.findById(response.id()).orElseThrow();
        // The local legacy variants table also has required size/stock columns not mapped by the current entity.
        if (jdbc.queryForObject("select count(*) from information_schema.columns where table_schema=database() and table_name='product_variants' and column_name='size'",Integer.class)>0) {
        jdbc.update("insert into product_variants (product_id,sku,price,size,stock_quantity,is_active,created_at,updated_at) values (?,?,?,'',0,1,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",p.getId(),marker+suffix,new BigDecimal(price));
        var id=jdbc.queryForObject("select id from product_variants where sku = ?",Long.class,marker+suffix);
        p.getVariants().add(variants.findById(id).orElseThrow());
        } else {
            var v=new ProductVariants(); v.setProduct(p); v.setSku(marker+suffix); v.setPrice(new BigDecimal(price));
            variants.saveAndFlush(v); p.getVariants().add(v);
        }
        return p;
    }
    @Test void searchesFiltersPaginatesAndHidesArchivedProducts() {
        var first=product("-a","10.00"); var second=product("-b","30.00");
        var page=catalog.list(marker,category.getId(),null,null,null,0,1,"name,asc");
        assertEquals(2,page.totalElements()); assertEquals(2,page.totalPages()); assertEquals(first.getId(),page.content().getFirst().id());
        var filtered=catalog.list(marker,category.getId(),null,new BigDecimal("20"),new BigDecimal("40"),0,20,"id,asc");
        assertEquals(1,filtered.totalElements()); assertEquals(second.getId(),filtered.content().getFirst().id());
        catalog.status(second.getId(),Status.ARCHIVED);
        assertEquals(1,catalog.list(marker,null,null,null,null,0,20,"id,asc").totalElements());
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->catalog.detail(second.getId()));
        category.setActive(false); categories.saveAndFlush(category);
        assertEquals(0,catalog.list(marker,null,null,null,null,0,20,"id,asc").totalElements());
        assertTrue(categoryService.list().stream().noneMatch(c->c.id().equals(category.getId())));
    }
    @Test void treatsSearchWildcardsAsLiteralCharacters() {
        product("-a","10.00");
        assertEquals(0,catalog.list(marker+"%",null,null,null,null,0,20,"id,asc").totalElements());
        assertEquals(0,catalog.list(marker+"_",null,null,null,null,0,20,"id,asc").totalElements());
    }
    @Test void updatePreservesVariantsAndAllowsSameSlug() {
        var p=product("-a","10.00");
        var result=catalog.update(p.getId(),new ProductRequest("Renamed",p.getSlug(),"Updated",category.getId(),null,Status.ACTIVE));
        assertEquals("Renamed",result.name()); assertEquals(1,result.variants().size());
        assertEquals(new BigDecimal("10.00"),result.variants().getFirst().price());
    }
}
