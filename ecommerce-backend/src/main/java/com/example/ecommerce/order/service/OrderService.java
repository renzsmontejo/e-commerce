package com.example.ecommerce.order.service;

import com.example.ecommerce.order.model.*;
import com.example.ecommerce.order.repository.*;
import com.example.ecommerce.order.dto.OrderDtos.*;
import com.example.ecommerce.order.dto.CheckoutDtos.*;
import com.example.ecommerce.payment.repository.PaymentsRepository;
import com.example.ecommerce.inventory.model.*;
import com.example.ecommerce.inventory.repository.*;
import com.example.ecommerce.user.repository.UsersRepository;
import static com.example.ecommerce.common.ApiErrors.*;
import static com.example.ecommerce.cart.service.CartService.conflict;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.*;

@Service
@Transactional(readOnly=true)
public class OrderService {
    @PersistenceContext private EntityManager em;
    private final OrdersRepository orders;
    private final PaymentsRepository payments;
    private final OrderStatusHistoryRepository history;
    private final InventoriesRepository inventories;
    private final InventoryMovementsRepository movements;
    private final UsersRepository users;
    private final com.example.ecommerce.payment.service.PaymentCancellationService cancellation;
    public OrderService(OrdersRepository orders, PaymentsRepository payments, OrderStatusHistoryRepository history,
            InventoriesRepository inventories, InventoryMovementsRepository movements, UsersRepository users, com.example.ecommerce.payment.service.PaymentCancellationService cancellation) {
        this.orders=orders; this.payments=payments; this.history=history; this.inventories=inventories; this.movements=movements; this.users=users; this.cancellation=cancellation;
    }
    public OrderPage customerOrders(Long userId,int page,int size) {
        return page(orders.findByUser_Id(userId,pageable(page,size)));
    }
    public OrderPage adminOrders(Status status,int page,int size) {
        return page(status==null?orders.findAll(pageable(page,size)):orders.findByStatus(status.name(),pageable(page,size)));
    }
    public Details customerDetail(Long userId,Long id) {
        return details(orders.findByIdAndUser_Id(id,userId).orElseThrow(()->missing("Order")));
    }
    // Serialize lifecycle/payment actions on the order before acquiring payment or stock locks.
    @Transactional
    public Orders lock(Long id) {
        orders.lockId(id).orElseThrow(()->missing("Order"));
        var order=orders.findById(id).orElseThrow(()->missing("Order"));
        em.refresh(order,LockModeType.PESSIMISTIC_WRITE);
        return order;
    }
    @Transactional
    public Details requestCancellation(Long userId,Long id,String reason) {
        var order=lock(id); own(order,userId);
        if("CANCELLATION_REQUESTED".equals(order.getStatus()) || "CANCELLED".equals(order.getStatus())) return details(order);
        if(!Set.of("PENDING","PAID","PROCESSING").contains(order.getStatus())) throw conflict("This order can no longer be cancelled");
        change(order,"CANCELLATION_REQUESTED",userId,reason.trim()); orders.flush(); return details(order);
    }
    @Transactional
    public Details changeStatus(Long adminId,Long id,StatusRequest request) {
        var order=lock(id); String current=order.getStatus(), next=request.status().name();
        if(current.equals(next)) return details(order);
        boolean allowed=switch(current) {
            case "PENDING" -> next.equals("CANCELLED");
            case "PAID" -> next.equals("PROCESSING");
            case "PROCESSING" -> next.equals("SHIPPED");
            case "SHIPPED" -> next.equals("DELIVERED");
            case "CANCELLATION_REQUESTED" -> next.equals("CANCELLED") || next.equals(resumeStatus(order));
            default -> false;
        };
        if(!allowed) throw conflict("Invalid order status transition");
        if(next.equals("CANCELLED")) {
            cancellation.prepare(id);
            adjustStock(order,adminId,false);
        }
        if(next.equals("SHIPPED")) adjustStock(order,adminId,true);
        change(order,next,adminId,request.notes()); orders.flush(); return details(order);
    }
    public void own(Orders order,Long userId) {
        if(!order.getUser().getId().equals(userId)) throw missing("Order");
    }
    public void change(Orders order,String next,Long actorId,String notes) {
        order.getStatusHistory().size();
        var event=new OrderStatusHistory(); event.setOrder(order); event.setPreviousStatus(order.getStatus()); event.setNewStatus(next);
        event.setNotes(notes); if(actorId!=null) event.setChangedByUser(users.getReferenceById(actorId)); history.save(event);
        order.setStatus(next); order.getStatusHistory().add(event);
    }
    private String resumeStatus(Orders order) {
        String previous=order.getStatusHistory().stream().filter(h->"CANCELLATION_REQUESTED".equals(h.getNewStatus()))
            .max(Comparator.comparing(OrderStatusHistory::getId)).map(OrderStatusHistory::getPreviousStatus).orElse("PENDING");
        if("PENDING".equals(previous) && payments.existsByOrder_IdAndStatusIn(order.getId(),List.of("PAID","SUCCEEDED"))) return "PAID";
        return previous;
    }
    private void adjustStock(Orders order,Long actorId,boolean shipped) {
        var quantities=new TreeMap<Long,Integer>();
        for(var item:order.getItems()) {
            if(item.getVariant()==null) throw conflict("Order inventory reference is missing");
            quantities.merge(item.getVariant().getId(),item.getQuantity(),Math::addExact);
        }
        for(var entry:quantities.entrySet()) {
            Long inventoryId=inventories.lockIdByVariantId(entry.getKey()).orElseThrow(()->conflict("Order inventory is missing"));
            var stock=inventories.findById(inventoryId).orElseThrow(); em.refresh(stock,LockModeType.PESSIMISTIC_WRITE);
            int quantity=entry.getValue();
            if(stock.getQuantityReserved()<quantity || stock.getQuantityOnHand()<stock.getQuantityReserved()) throw conflict("Order stock reservation is inconsistent");
            stock.setQuantityReserved(stock.getQuantityReserved()-quantity);
            if(shipped) stock.setQuantityOnHand(stock.getQuantityOnHand()-quantity);
            var movement=new InventoryMovements(); movement.setInventory(stock); movement.setOrder(order);
            movement.setUser(users.getReferenceById(actorId)); movement.setMovementType(shipped?"SHIPMENT":"RELEASE");
            movement.setQuantityChange(shipped?-quantity:quantity); movement.setReason(shipped?"Order shipped":"Order cancelled"); movements.save(movement);
        }
    }
    public Details details(Orders o) {
        var totals=new Totals(o.getSubtotal(),o.getDiscountAmount(),o.getShippingFee(),o.getTaxAmount(),o.getTotalAmount(),o.getCurrency());
        var lines=o.getItems().stream().sorted(Comparator.comparing(OrderItems::getId)).map(i->new OrderLine(i.getId(),i.getVariant()==null?null:i.getVariant().getId(),
            i.getProductName(),i.getSku(),i.getQuantity(),i.getUnitPrice(),i.getLineTotal())).toList();
        var paymentList=payments.findByOrder_IdOrderByIdAsc(o.getId()).stream().map(p->new PaymentSummary(p.getId(),p.getMethod(),p.getProvider(),p.getStatus(),p.getAmount(),p.getCurrency(),p.getPaidAt())).toList();
        var events=o.getStatusHistory().stream().sorted(Comparator.comparing(OrderStatusHistory::getChangedAt).thenComparing(OrderStatusHistory::getId))
            .map(h->new StatusEvent(h.getPreviousStatus(),h.getNewStatus(),h.getNotes(),h.getChangedAt())).toList();
        return new Details(o.getId(),o.getOrderNumber(),o.getStatus(),o.getRecipientName(),o.getShippingPhone(),o.getShippingAddress(),o.getPlacedAt(),totals,lines,paymentList,events);
    }
    private Pageable pageable(int page,int size) { return PageRequest.of(page,size,Sort.by(Sort.Direction.DESC,"placedAt","id")); }
    private OrderPage page(Page<Orders> page) {
        return new OrderPage(page.getContent().stream().map(o->new Summary(o.getId(),o.getOrderNumber(),o.getStatus(),o.getTotalAmount(),o.getCurrency(),o.getPlacedAt())).toList(),
            page.getNumber(),page.getSize(),page.getTotalElements(),page.getTotalPages());
    }
}
