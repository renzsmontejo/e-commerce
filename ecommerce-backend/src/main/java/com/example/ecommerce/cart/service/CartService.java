package com.example.ecommerce.cart.service;

import com.example.ecommerce.cart.dto.CartDtos.*;
import com.example.ecommerce.cart.model.*;
import com.example.ecommerce.cart.repository.CartsRepository;
import com.example.ecommerce.product.model.ProductVariants;
import com.example.ecommerce.product.repository.ProductVariantsRepository;
import com.example.ecommerce.inventory.repository.InventoriesRepository;
import com.example.ecommerce.user.model.Users;
import com.example.ecommerce.user.repository.UsersRepository;
import com.example.ecommerce.common.Money;
import static com.example.ecommerce.common.ApiErrors.*;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class CartService {
    private final CartsRepository carts;
    private final UsersRepository users;
    private final ProductVariantsRepository variants;
    private final InventoriesRepository inventories;
    public CartService(CartsRepository carts, UsersRepository users, ProductVariantsRepository variants, InventoriesRepository inventories) {
        this.carts=carts; this.users=users; this.variants=variants; this.inventories=inventories;
    }
    public Users lockOwner(Long userId) {
        var owner=users.findForCartUpdate(userId).filter(u -> "ACTIVE".equals(u.getStatus()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return owner;
    }
    // The caller must hold the owner's lock for the entire transaction.
    public Carts findOwnedCart(Long userId) { return carts.findByUser_Id(userId).orElse(null); }
    public CartResponse get(Long userId) {
        lockOwner(userId);
        return response(findOwnedCart(userId));
    }
    public CartResponse add(Long userId, AddItemRequest request) {
        var owner=lockOwner(userId);
        var cart=findOwnedCart(userId);
        if(cart==null) { cart=new Carts(); cart.setUser(owner); }
        var variant=variants.findById(request.variantId()).orElseThrow(() -> missing("Variant"));
        var existing=cart.getItems().stream().filter(i -> i.getVariant().getId().equals(variant.getId())).findFirst();
        int quantity=existing.map(CartItems::getQuantity).orElse(0)+request.quantity();
        if(quantity>999) throw bad("Quantity cannot exceed 999 per variant");
        validate(variant,quantity);
        if(existing.isPresent()) existing.get().setQuantity(quantity);
        else {
            if(cart.getItems().size()>=100) throw bad("A cart can contain at most 100 variants");
            var item=new CartItems(); item.setVariant(variant); item.setQuantity(quantity); cart.addItem(item);
        }
        touch(cart);
        if(cart.getId()==null) carts.saveAndFlush(cart); else carts.flush();
        return response(cart);
    }
    public CartResponse change(Long userId, Long itemId, int quantity) {
        lockOwner(userId); var cart=findOwnedCart(userId); var item=ownedItem(cart,itemId);
        validate(item.getVariant(),quantity); item.setQuantity(quantity); touch(cart);
        carts.flush(); return response(cart);
    }
    public void remove(Long userId, Long itemId) {
        lockOwner(userId); var cart=findOwnedCart(userId); var item=ownedItem(cart,itemId);
        cart.removeItem(item); touch(cart); carts.flush();
    }
    private CartItems ownedItem(Carts cart, Long itemId) {
        if(cart==null) throw missing("Cart item");
        return cart.getItems().stream().filter(i->i.getId().equals(itemId)).findFirst().orElseThrow(()->missing("Cart item"));
    }
    public static boolean active(ProductVariants v) {
        return v.isActive() && "ACTIVE".equals(v.getProduct().getStatus()) && v.getProduct().getCategory().isActive();
    }
    private void validate(ProductVariants v, int quantity) {
        if(quantity<1 || quantity>999) throw bad("Quantity must be between 1 and 999");
        if(!active(v)) throw conflict("Variant is unavailable");
        var inventory=inventories.findByVariant_Id(v.getId()).orElseThrow(()->conflict("Variant is out of stock"));
        if(!inventory.hasAvailableStock(quantity)) throw conflict("Insufficient stock for variant " + v.getId());
        Money.line(v.getPrice(),quantity);
    }
    public CartResponse response(Carts cart) {
        if(cart==null) return new CartResponse(null,List.of(),0,Money.ZERO,"PHP");
        var items=cart.getItems().stream().sorted(Comparator.comparing(CartItems::getId)).map(i -> {
            var v=i.getVariant(); var p=v.getProduct();
            int available=inventories.findByVariant_Id(v.getId()).map(inv->Math.max(0,inv.getAvailableQuantity())).orElse(0);
            return new ItemResponse(i.getId(),p.getId(),v.getId(),p.getName(),v.getSku(),i.getQuantity(),
                Money.checked(v.getPrice()),Money.line(v.getPrice(),i.getQuantity()),available,active(v) && available>=i.getQuantity());
        }).toList();
        var subtotal=Money.checked(items.stream().map(ItemResponse::lineTotal).reduce(Money.ZERO,java.math.BigDecimal::add));
        return new CartResponse(cart.getId(),items,items.stream().mapToInt(ItemResponse::quantity).sum(),subtotal,"PHP");
    }
    public void clear(Carts cart) {
        for(var item:new ArrayList<>(cart.getItems())) cart.removeItem(item);
        touch(cart); carts.flush();
    }
    private void touch(Carts cart) { cart.setUpdatedAt(LocalDateTime.now()); }
    public static ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT,message); }
}
