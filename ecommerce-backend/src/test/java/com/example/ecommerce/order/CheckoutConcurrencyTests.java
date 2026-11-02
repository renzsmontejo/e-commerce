package com.example.ecommerce.order;

import com.example.ecommerce.cart.service.CartService;
import com.example.ecommerce.cart.dto.CartDtos.AddItemRequest;
import com.example.ecommerce.order.service.CheckoutService;
import com.example.ecommerce.order.dto.CheckoutDtos.PlaceOrderRequest;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties={"app.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=", "logging.level.org.springframework.security=WARN"})
class CheckoutConcurrencyTests extends com.example.ecommerce.support.MySqlIntegrationTest {
    @Autowired CheckoutService checkout;
    @Autowired CartService carts;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactions;
    Long first,second,variant,product,category;
    TransactionTemplate tx;
    @BeforeEach void setup() {
        tx=new TransactionTemplate(transactions);
        tx.executeWithoutResult(status->{
            String marker="checkout-race-"+UUID.randomUUID();
            first=user(marker+"-1"); second=user(marker+"-2");
            jdbc.update("insert into categories (name,slug,is_active,created_at,updated_at) values (?,?,1,NOW(),NOW())",marker,marker);
            category=jdbc.queryForObject("select id from categories where slug=?",Long.class,marker);
            jdbc.update("insert into products (name,slug,description,status,category_id,created_at,updated_at) values (?,?,'','ACTIVE',?,NOW(),NOW())",marker,marker,category);
            product=jdbc.queryForObject("select id from products where slug=?",Long.class,marker);
            if(jdbc.queryForObject("select count(*) from information_schema.columns where table_schema=database() and table_name='product_variants' and column_name='size'",Integer.class)>0)
                jdbc.update("insert into product_variants (product_id,sku,price,size,stock_quantity,is_active,created_at,updated_at) values (?,?,10.50,'',0,1,NOW(),NOW())",product,marker);
            else jdbc.update("insert into product_variants (product_id,sku,price,is_active,created_at,updated_at) values (?,?,10.50,1,NOW(),NOW())",product,marker);
            variant=jdbc.queryForObject("select id from product_variants where sku=?",Long.class,marker);
            jdbc.update("insert into inventories (variant_id,quantity_on_hand,quantity_reserved,reorder_level,version,updated_at) values (?,5,0,1,0,NOW())",variant);
            carts.add(first,new AddItemRequest(variant,4)); carts.add(second,new AddItemRequest(variant,4));
        });
    }
    Long user(String marker) {
        jdbc.update("insert into users (first_name,last_name,email,password_hash,status,created_at,updated_at) values ('Checkout','Race',?,'test-hash','ACTIVE',NOW(),NOW())",marker+"@example.invalid");
        return jdbc.queryForObject("select id from users where email=?",Long.class,marker+"@example.invalid");
    }
    PlaceOrderRequest request() { return new PlaceOrderRequest("Test","09123456789","Test address",new BigDecimal("42.00")); }
    @AfterEach void cleanup() {
        if(first==null) return;
        tx.executeWithoutResult(status->{
            jdbc.update("delete from inventory_movements where order_id in (select id from orders where user_id in (?,?))",first,second);
            jdbc.update("delete from order_status_history where order_id in (select id from orders where user_id in (?,?))",first,second);
            jdbc.update("delete from order_items where order_id in (select id from orders where user_id in (?,?))",first,second);
            jdbc.update("delete from orders where user_id in (?,?)",first,second);
            jdbc.update("delete from cart_items where cart_id in (select id from carts where user_id in (?,?))",first,second);
            jdbc.update("delete from carts where user_id in (?,?)",first,second);
            jdbc.update("delete from users where id in (?,?)",first,second);
            jdbc.update("delete from inventories where variant_id=?",variant);
            jdbc.update("delete from product_variants where id=?",variant);
            jdbc.update("delete from products where id=?",product);
            jdbc.update("delete from categories where id=?",category);
        });
    }
    @Test void twoConcurrentOrdersCannotOversell() throws Exception {
        var start=new CountDownLatch(1); var ready=new CountDownLatch(2);
        try(var executor=Executors.newFixedThreadPool(2)) {
            var futures=new ArrayList<Future<Integer>>();
            for(Long user:List.of(first,second)) futures.add(executor.submit(()->{
                ready.countDown(); if(!start.await(5,TimeUnit.SECONDS)) throw new IllegalStateException("Start timed out");
                try { checkout.place(user,request()); return 201; }
                catch(ResponseStatusException e) { return e.getStatusCode().value(); }
            }));
            assertTrue(ready.await(5,TimeUnit.SECONDS)); start.countDown();
            var results=new ArrayList<Integer>(); for(var future:futures) results.add(future.get(20,TimeUnit.SECONDS));
            Collections.sort(results); assertEquals(List.of(201,409),results);
        }
        assertEquals(4,jdbc.queryForObject("select quantity_reserved from inventories where variant_id=?",Integer.class,variant));
        assertEquals(5,jdbc.queryForObject("select quantity_on_hand from inventories where variant_id=?",Integer.class,variant));
        assertEquals(1,jdbc.queryForObject("select count(*) from orders where user_id in (?,?)",Integer.class,first,second));
    }
    @Test void rollbackRestoresCartAndInventoryAndRemovesOrderRecords() {
        tx.executeWithoutResult(status->{ checkout.place(first,request()); status.setRollbackOnly(); });
        assertEquals(0,jdbc.queryForObject("select quantity_reserved from inventories where variant_id=?",Integer.class,variant));
        assertEquals(0,jdbc.queryForObject("select count(*) from orders where user_id=?",Integer.class,first));
        assertEquals(4,carts.get(first).totalQuantity());
    }
}
