package com.autohub.api.service;

import com.autohub.api.model.*;
import com.autohub.api.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class OrderLifecycleService {

    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;
    private final EmailService emailService;

    public OrderLifecycleService(OrderRepository orderRepository,
                                 InventoryService inventoryService,
                                 EmailService emailService) {
        this.orderRepository = orderRepository;
        this.inventoryService = inventoryService;
        this.emailService = emailService;
    }

    /**
     * PHASE 5.5: Strict Barcode Verification
     * @param verifyMap Key = OrderItem ID (String), Value = Scanned Barcode
     */
    @Transactional
    public Order verifyAndPick(Long orderId, Map<String, String> verifyMap) {
        Order order = get(orderId);

        if (order.getStatus() != OrderStatus.PAID) {
            throw new RuntimeException("Picking blocked: Order must be PAID before selection.");
        }

        // --- STRICT BARCODE AUDIT ---
        for (OrderItem item : order.getItems()) {
            String scanned = verifyMap.get(item.getId().toString());
            String actual = item.getPart().getBarcode();

            if (scanned == null || !scanned.equals(actual)) {
                throw new RuntimeException("Barcode Mismatch: Item '" + item.getPart().getName() + "' verification failed.");
            }

            // Mark item as physically picked
            item.setPickedQuantity(item.getQuantity());
        }

        order.setStatus(OrderStatus.PICKED);
        order.setPickedDate(LocalDateTime.now());
        return orderRepository.save(order);
    }

    @Transactional
    public Order markPaid(Long orderId, String paymentId) {
        Order order = get(orderId);
        if ("PAID".equals(order.getPaymentStatus())) return order;

        order.setPaymentId(paymentId);
        order.setPaymentStatus("PAID");
        order.setStatus(OrderStatus.PAID);
        inventoryService.deductReservedInventory(order);

        Order saved = orderRepository.save(order);
        try { emailService.sendOrderConfirmation(saved); } catch (Exception ignored) {}
        return saved;
    }

    @Transactional
    public Order markShipped(Long orderId, String courier, String tracking) {
        Order order = get(orderId);

        // --- GUARDRAIL: Must be verified by Clerk first ---
        if (order.getStatus() != OrderStatus.PICKED) {
            throw new RuntimeException("Shipping blocked: Order must be physically PICKED and verified first.");
        }

        order.setCourierName(courier);
        order.setTrackingNumber(tracking);
        order.setShippedDate(LocalDateTime.now());
        order.setStatus(OrderStatus.SHIPPED);
        return orderRepository.save(order);
    }

    @Transactional
    public Order executeRefund(Long orderId) {
        Order order = get(orderId);
        order.setRefundedAmount(order.getTotalAmount());
        order.setStatus(OrderStatus.REFUNDED);
        order.setPaymentStatus("REFUNDED");
        inventoryService.releaseReservations(order);
        return orderRepository.save(order);
    }

    @Transactional
    public Order overrideLogistics(Long orderId, String courier, String tracking) {
        Order order = get(orderId);
        order.setCourierName(courier);
        order.setTrackingNumber(tracking);
        return orderRepository.save(order);
    }

    private Order get(Long id) {
        return orderRepository.findById(id).orElseThrow(() -> new RuntimeException("Order ID " + id + " not found."));
    }

    public void assertCanReview(User user, Long partId) {
        boolean eligible = orderRepository.existsEligibleDeliveredOrder(user.getId(), partId, OrderStatus.DELIVERED, OrderStatus.COMPLETED);
        if (!eligible) throw new RuntimeException("Review blocked: Item not yet delivered.");
    }
}