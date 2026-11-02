package com.example.ecommerce.order;

import com.example.ecommerce.user.model.Users;
import com.example.ecommerce.category.model.Categories;
import com.example.ecommerce.product.model.*;
import com.example.ecommerce.inventory.model.Inventories;
import com.example.ecommerce.order.model.*;
import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import java.math.BigDecimal;
import java.util.UUID;

public final class OrderFixtures {
    public record Fixture(Users owner,Users other,Orders order,Inventories inventory) {}
    public static Fixture create(EntityManager em,JdbcTemplate jdbc) {
        String marker="order-test-"+UUID.randomUUID();
        var owner=user(em,marker);var other=user(em,marker+"-other");
        var category=new Categories();category.setName(marker);category.setSlug(marker);em.persist(category);
        var product=new Products();product.setName(marker);product.setSlug(marker);product.setCategory(category);em.persist(product);em.flush();
        ProductVariants variant;
        if(jdbc.queryForObject("select count(*) from information_schema.columns where table_schema=database() and table_name='product_variants' and column_name='size'",Integer.class)>0) {
            jdbc.update("insert into product_variants (product_id,sku,price,size,stock_quantity,is_active,created_at,updated_at) values (?,?,10.50,'',0,1,NOW(),NOW())",product.getId(),marker);
            variant=em.find(ProductVariants.class,jdbc.queryForObject("select id from product_variants where sku=?",Long.class,marker));
        } else {variant=new ProductVariants();variant.setProduct(product);variant.setSku(marker);variant.setPrice(new BigDecimal("10.50"));em.persist(variant);}
        var stock=new Inventories();stock.setVariant(variant);stock.setQuantityOnHand(5);stock.setQuantityReserved(1);em.persist(stock);
        var order=new Orders();order.setUser(owner);order.setOrderNumber(marker);order.setRecipientName("Test");order.setShippingPhone("09123456789");order.setShippingAddress("Test address");
        order.setSubtotal(new BigDecimal("10.50"));order.setTotalAmount(new BigDecimal("10.50"));
        var line=new OrderItems();line.setVariant(variant);line.setProductName(product.getName());line.setSku(marker);line.setQuantity(1);line.setUnitPrice(new BigDecimal("10.50"));order.addItem(line);
        em.persist(order);em.flush();return new Fixture(owner,other,order,stock);
    }
    static Users user(EntityManager em,String marker) {
        var u=new Users();u.setFirstName("Order");u.setLastName("Test");u.setEmail(marker+"@example.invalid");u.setStatus("ACTIVE");u.setPasswordHash("test-hash");em.persist(u);return u;
    }
}
