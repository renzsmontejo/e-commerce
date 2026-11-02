package com.example.ecommerce.order.service;

import com.example.ecommerce.order.dto.CheckoutDtos.*;
import com.example.ecommerce.cart.service.CartService;
import com.example.ecommerce.cart.model.*;
import com.example.ecommerce.order.model.*;
import com.example.ecommerce.order.repository.*;
import com.example.ecommerce.inventory.model.*;
import com.example.ecommerce.inventory.repository.*;
import com.example.ecommerce.common.Money;
import static com.example.ecommerce.cart.service.CartService.conflict;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CheckoutService {
    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;
    private final CartService carts;
    private final CheckoutPricing pricing;
    private final InventoriesRepository inventories;
    private final OrdersRepository orders;
    private final OrderStatusHistoryRepository history;
    private final InventoryMovementsRepository movements;
    public CheckoutService(CartService carts, CheckoutPricing pricing, InventoriesRepository inventories,
            OrdersRepository orders, OrderStatusHistoryRepository history, InventoryMovementsRepository movements) {
        this.carts=carts; this.pricing=pricing; this.inventories=inventories;
        this.orders=orders; this.history=history; this.movements=movements;
    }
    public PreviewResponse preview(Long userId) {
        carts.lockOwner(userId); var cart=requireCart(userId);
        validate(cart,false);
        var response=carts.response(cart);
        return new PreviewResponse(response.items(),pricing.calculate(response.subtotal()));
    }
    public OrderResponse place(Long userId, PlaceOrderRequest request) {
        var owner=carts.lockOwner(userId); var cart=requireCart(userId);
        var locked=validate(cart,true);
        var response=carts.response(cart); var totals=pricing.calculate(response.subtotal());
        if(totals.totalAmount().compareTo(request.expectedTotal())!=0) throw conflict("Checkout total changed; preview your cart again");
        var order=new Orders(); order.setUser(owner); order.setOrderNumber("ORD-"+UUID.randomUUID());
        order.setStatus("PENDING"); order.setRecipientName(request.recipientName().trim());
        order.setShippingPhone(request.shippingPhone().trim()); order.setShippingAddress(request.shippingAddress().trim());
        order.setSubtotal(totals.subtotal()); order.setDiscountAmount(totals.discountAmount());
        order.setShippingFee(totals.shippingFee()); order.setTaxAmount(totals.taxAmount());
        order.setTotalAmount(totals.totalAmount()); order.setCurrency(totals.currency());
        for(var item:cart.getItems()) {
            var v=item.getVariant(); var line=new OrderItems(); line.setVariant(v); line.setProductName(v.getProduct().getName());
            line.setSku(v.getSku()); line.setQuantity(item.getQuantity()); line.setUnitPrice(Money.checked(v.getPrice()));
            line.setLineTotal(Money.line(v.getPrice(),item.getQuantity())); order.addItem(line);
        }
        orders.saveAndFlush(order);
        for(var item:cart.getItems()) {
            var inventory=locked.get(item.getVariant().getId());
            inventory.setQuantityReserved(inventory.getQuantityReserved()+item.getQuantity());
            inventories.save(inventory);
            var movement=new InventoryMovements(); movement.setInventory(inventory); movement.setUser(owner); movement.setOrder(order);
            movement.setMovementType("RESERVATION"); movement.setQuantityChange(-item.getQuantity());
            movement.setReason("Reserved for pending order"); movements.save(movement);
        }
        var event=new OrderStatusHistory(); event.setOrder(order); event.setChangedByUser(owner); event.setNewStatus("PENDING");
        event.setNotes("Order placed"); history.save(event);
        carts.clear(cart);
        return new OrderResponse(order.getId(),order.getOrderNumber(),order.getStatus(),order.getRecipientName(),order.getShippingPhone(),
                order.getShippingAddress(),order.getPlacedAt(),totals,order.getItems().stream().map(i -> new OrderLine(
                    i.getId(),i.getVariant().getId(),i.getProductName(),i.getSku(),i.getQuantity(),i.getUnitPrice(),i.getLineTotal())).toList());
    }
    private Carts requireCart(Long userId) {
        var cart=carts.findOwnedCart(userId);
        if(cart==null || cart.getItems().isEmpty()) throw conflict("Cart is empty");
        return cart;
    }
    private Map<Long,Inventories> validate(Carts cart, boolean lock) {
        var stock=new HashMap<Long,Inventories>();
        // Consistent lock order prevents two multi-product checkouts from deadlocking.
        for(var item:cart.getItems().stream().sorted(Comparator.comparing(i->i.getVariant().getId())).toList()) {
            var v=item.getVariant();
            if(lock) {
                // Shared locks keep the product, category and price valid until the order commits.
                entityManager.refresh(v,jakarta.persistence.LockModeType.PESSIMISTIC_READ);
                entityManager.refresh(v.getProduct(),jakarta.persistence.LockModeType.PESSIMISTIC_READ);
                entityManager.refresh(v.getProduct().getCategory(),jakarta.persistence.LockModeType.PESSIMISTIC_READ);
            }
            if(!CartService.active(v)) throw conflict("Variant is unavailable: "+v.getId());
            if(item.getQuantity()<1 || item.getQuantity()>999) throw conflict("Invalid cart quantity");
            Inventories inventory;
            if(lock) {
                var inventoryId=inventories.lockIdByVariantId(v.getId()).orElseThrow(()->conflict("Variant is out of stock: "+v.getId()));
                inventory=inventories.findById(inventoryId).orElseThrow(()->conflict("Variant is out of stock: "+v.getId()));
            } else inventory=inventories.findByVariant_Id(v.getId()).orElseThrow(()->conflict("Variant is out of stock: "+v.getId()));
            // A previously loaded inventory entity can be stale after waiting for another checkout.
            if(lock) entityManager.refresh(inventory,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
            if(inventory.getQuantityOnHand()<0 || inventory.getQuantityReserved()<0
                    || inventory.getQuantityReserved()>inventory.getQuantityOnHand()
                    || !inventory.hasAvailableStock(item.getQuantity())) throw conflict("Insufficient stock for variant "+v.getId());
            stock.put(v.getId(),inventory);
        }
        return stock;
    }
}
