package com.example.ecommerce.cart;

import com.example.ecommerce.cart.service.CartService;
import com.example.ecommerce.cart.dto.CartDtos.*;
import com.example.ecommerce.order.service.CheckoutService;
import com.example.ecommerce.order.dto.CheckoutDtos.*;
import com.example.ecommerce.order.repository.OrdersRepository;
import com.example.ecommerce.inventory.model.Inventories;
import com.example.ecommerce.inventory.repository.InventoriesRepository;
import com.example.ecommerce.product.model.*;
import com.example.ecommerce.product.repository.*;
import com.example.ecommerce.category.model.Categories;
import com.example.ecommerce.category.repository.CategoriesRepository;
import com.example.ecommerce.user.model.Users;
import com.example.ecommerce.user.repository.UsersRepository;
import com.example.ecommerce.cart.repository.CartsRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties={"app.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=", "logging.level.org.springframework.security=WARN"})
@Transactional
class CartCheckoutIntegrationTests extends com.example.ecommerce.support.MySqlIntegrationTest {
    @Autowired CartService carts;
    @Autowired CheckoutService checkout;
    @Autowired CartsRepository cartRepository;
    @Autowired UsersRepository users;
    @Autowired ProductsRepository products;
    @Autowired ProductVariantsRepository variants;
    @Autowired CategoriesRepository categories;
    @Autowired InventoriesRepository inventories;
    @Autowired OrdersRepository orders;
    @Autowired JdbcTemplate jdbc;
    Users owner;
    ProductVariants variant;
    Inventories inventory;
    @BeforeEach void setup() {
        String marker="checkout-test-"+UUID.randomUUID();
        owner=user(marker);
        var category=new Categories(); category.setName(marker); category.setSlug(marker); categories.saveAndFlush(category);
        var product=new Products(); product.setName(marker); product.setSlug(marker); product.setCategory(category); product.setStatus("ACTIVE"); products.saveAndFlush(product);
        if(jdbc.queryForObject("select count(*) from information_schema.columns where table_schema=database() and table_name='product_variants' and column_name='size'",Integer.class)>0) {
            jdbc.update("insert into product_variants (product_id,sku,price,size,stock_quantity,is_active,created_at,updated_at) values (?,?,10.50,'',0,1,NOW(),NOW())",product.getId(),marker);
            variant=variants.findById(jdbc.queryForObject("select id from product_variants where sku=?",Long.class,marker)).orElseThrow();
        } else {
            variant=new ProductVariants(); variant.setProduct(product); variant.setSku(marker); variant.setPrice(new BigDecimal("10.50")); variants.saveAndFlush(variant);
        }
        inventory=new Inventories(); inventory.setVariant(variant); inventory.setQuantityOnHand(10); inventory.setQuantityReserved(0); inventories.saveAndFlush(inventory);
    }
    Users user(String marker) {
        var u=new Users(); u.setFirstName("Checkout"); u.setLastName("Test"); u.setEmail(marker+"@example.invalid"); u.setPasswordHash("test-hash"); u.setStatus("ACTIVE"); return users.saveAndFlush(u);
    }
    PlaceOrderRequest request(String total) { return new PlaceOrderRequest("Test Recipient","09123456789","Test address",new BigDecimal(total)); }
    @Test void emptyReadDoesNotCreateACartAndEmptyCheckoutConflicts() {
        assertTrue(carts.get(owner.getId()).items().isEmpty()); assertTrue(cartRepository.findByUser_Id(owner.getId()).isEmpty());
        assertEquals(409,assertThrows(ResponseStatusException.class,()->checkout.preview(owner.getId())).getStatusCode().value());
    }
    @Test void addMergesVariantAndQuantityChangeAndDeleteWork() {
        var cart=carts.add(owner.getId(),new AddItemRequest(variant.getId(),2));
        cart=carts.add(owner.getId(),new AddItemRequest(variant.getId(),3));
        assertEquals(1,cart.items().size()); assertEquals(5,cart.totalQuantity()); assertEquals(new BigDecimal("52.50"),cart.subtotal());
        var id=cart.items().getFirst().id(); cart=carts.change(owner.getId(),id,1); assertEquals(new BigDecimal("10.50"),cart.subtotal());
        carts.remove(owner.getId(),id); assertTrue(carts.get(owner.getId()).items().isEmpty());
        assertEquals(0,inventory.getQuantityReserved());
    }
    @Test void anotherUserCannotChangeOrDeleteCartItems() {
        var id=carts.add(owner.getId(),new AddItemRequest(variant.getId(),2)).items().getFirst().id();
        var other=user("other-"+UUID.randomUUID());
        assertEquals(404,assertThrows(ResponseStatusException.class,()->carts.change(other.getId(),id,1)).getStatusCode().value());
        assertEquals(404,assertThrows(ResponseStatusException.class,()->carts.remove(other.getId(),id)).getStatusCode().value());
        assertEquals(2,carts.get(owner.getId()).totalQuantity());
    }
    @Test void previewDoesNotReserveAndPlaceSnapshotsReservesAndClearsCart() {
        carts.add(owner.getId(),new AddItemRequest(variant.getId(),2));
        var preview=checkout.preview(owner.getId()); assertEquals(new BigDecimal("21.00"),preview.totals().totalAmount());
        assertEquals(0,inventory.getQuantityReserved());
        var order=checkout.place(owner.getId(),request("21.00"));
        assertEquals("PENDING",order.status()); assertEquals(1,order.items().size());
        assertEquals(new BigDecimal("10.50"),order.items().getFirst().unitPrice()); assertEquals(new BigDecimal("21.00"),order.totals().totalAmount());
        assertEquals(2,inventory.getQuantityReserved()); assertEquals(10,inventory.getQuantityOnHand()); assertTrue(carts.get(owner.getId()).items().isEmpty());
        assertEquals(1,jdbc.queryForObject("select count(*) from order_status_history where order_id=?",Integer.class,order.id()));
        assertEquals(-2,jdbc.queryForObject("select quantity_change from inventory_movements where order_id=?",Integer.class,order.id()));
        assertEquals(409,assertThrows(ResponseStatusException.class,()->checkout.place(owner.getId(),request("21.00"))).getStatusCode().value());
    }
    @Test void staleTotalAndUnavailableItemsDoNotPlaceOrders() {
        carts.add(owner.getId(),new AddItemRequest(variant.getId(),2));
        assertEquals(409,assertThrows(ResponseStatusException.class,()->checkout.place(owner.getId(),request("1.00"))).getStatusCode().value());
        assertEquals(0,inventory.getQuantityReserved()); assertEquals(2,carts.get(owner.getId()).totalQuantity());
        variant.getProduct().setStatus("ARCHIVED"); products.saveAndFlush(variant.getProduct());
        assertFalse(carts.get(owner.getId()).items().getFirst().purchasable());
        assertEquals(409,assertThrows(ResponseStatusException.class,()->checkout.preview(owner.getId())).getStatusCode().value());
    }
    @Test void stockIsValidatedAtMutationAndRecheckedAtPlacement() {
        assertEquals(409,assertThrows(ResponseStatusException.class,()->carts.add(owner.getId(),new AddItemRequest(variant.getId(),11))).getStatusCode().value());
        carts.add(owner.getId(),new AddItemRequest(variant.getId(),2));
        inventory.setQuantityReserved(9); inventories.saveAndFlush(inventory);
        assertEquals(409,assertThrows(ResponseStatusException.class,()->checkout.place(owner.getId(),request("21.00"))).getStatusCode().value());
        assertEquals(9,inventory.getQuantityReserved()); assertEquals(2,carts.get(owner.getId()).totalQuantity());
    }
}
