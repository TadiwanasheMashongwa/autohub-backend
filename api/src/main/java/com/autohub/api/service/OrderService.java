package com.autohub.api.service;

import com.autohub.api.model.*;
import com.autohub.api.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final PartRepository partRepository;
    private final EmailService emailService;
    private final IdempotencyRepository idempotencyRepository;
    private final AuditLogRepository auditLogRepository;
    private final ShippingService shippingService;
    private final ObjectMapper objectMapper;

    public OrderService(OrderRepository orderRepository,
                        PartRepository partRepository,
                        EmailService emailService,
                        IdempotencyRepository idempotencyRepository,
                        AuditLogRepository auditLogRepository,
                        ShippingService shippingService,
                        ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.partRepository = partRepository;
        this.emailService = emailService;
        this.idempotencyRepository = idempotencyRepository;
        this.auditLogRepository = auditLogRepository;
        this.shippingService = shippingService;
        this.objectMapper = objectMapper;
    }

    /**
     * PHASE 4: Automated Stock Release.
     * Triggered only after successful payment confirmation via webhook/service.
     */
    @Transactional
    public Order confirmPayment(Long orderId, String paymentId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Order is not in a state to be paid. Current Status: " + order.getStatus());
        }

        // Atomically deduct stock
        for (OrderItem item : order.getItems()) {
            Part part = partRepository.findById(item.getPart().getId())
                    .orElseThrow(() -> new RuntimeException("Part not found: " + item.getPart().getSku()));

            if (part.getStockQuantity() < item.getQuantity()) {
                throw new RuntimeException("Stock sold out for SKU: " + part.getSku());
            }

            part.setStockQuantity(part.getStockQuantity() - item.getQuantity());
            partRepository.save(part);

            // Phase 4 Audit Sync
            auditLogRepository.save(new AuditLog(
                    "STOCK_DEDUCTION",
                    "SYSTEM_PAYMENT",
                    "Deducted " + item.getQuantity() + " units for SKU: " + part.getSku() + " (Order #" + orderId + ")"
            ));
        }

        order.setPaymentId(paymentId);
        order.setPaymentStatus("SUCCEEDED");
        order.setStatus(OrderStatus.COMPLETED);

        Order savedOrder = orderRepository.save(order);

        auditLogRepository.save(new AuditLog("PAYMENT_CONFIRMED", "SYSTEM", "Order #" + orderId + " payment verified."));
        emailService.sendOrderConfirmation(savedOrder);

        return savedOrder;
    }

    /**
     * PHASE 3: Barcode Picking Flow.
     * Allows warehouse staff to scan items to verify they match the order.
     */
    @Transactional
    public Order verifyAndPickItem(Long orderId, String barcode) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        OrderItem targetItem = order.getItems().stream()
                .filter(item -> item.getPart().getBarcode().equals(barcode))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Barcode " + barcode + " does not belong to this order."));

        if (targetItem.getPickedQuantity() >= targetItem.getQuantity()) {
            throw new RuntimeException("Quantity already fulfilled for this part.");
        }

        targetItem.setPickedQuantity(targetItem.getPickedQuantity() + 1);

        String clerkName = SecurityContextHolder.getContext().getAuthentication().getName();
        auditLogRepository.save(new AuditLog(
                "WAREHOUSE_PICK",
                clerkName,
                "Picked SKU: " + targetItem.getPart().getSku() + " for Order #" + orderId
        ));

        return orderRepository.save(order);
    }

    /**
     * PHASE 5: Shipping Engine Manifest.
     * Generates a manifest including weight and shipping details for couriers.
     */
    public Map<String, Object> getOrderManifest(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        return shippingService.generateManifest(order);
    }

    /**
     * Checkout Logic with Idempotency & Coupon Support.
     */
    @Transactional
    public Order checkoutCart(User user, String idempotencyKey) {
        if (idempotencyKey != null) {
            Optional<IdempotencyRecord> record = idempotencyRepository.findById(idempotencyKey);
            if (record.isPresent()) {
                try {
                    return objectMapper.readValue(record.get().getResponseBody(), Order.class);
                } catch (Exception e) {
                    throw new RuntimeException("Error processing cached order");
                }
            }
        }

        Cart cart = user.getCart();
        if (cart == null || cart.getItems().isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cart.getItems()) {
            OrderItem oi = new OrderItem();
            oi.setPart(cartItem.getPart());
            oi.setQuantity(cartItem.getQuantity());
            oi.setPickedQuantity(0);
            orderItems.add(oi);
        }

        Order order = createOrderWithCoupon(user, orderItems, cart.getAppliedCoupon());
        cart.getItems().clear();
        cart.setAppliedCoupon(null);

        if (idempotencyKey != null) {
            try {
                String responseBody = objectMapper.writeValueAsString(order);
                idempotencyRepository.save(new IdempotencyRecord(idempotencyKey, responseBody, 200));
            } catch (Exception e) {
                // Non-critical logging
            }
        }
        return order;
    }

    @Transactional
    private Order createOrderWithCoupon(User user, List<OrderItem> items, Coupon coupon) {
        Order order = new Order();
        order.setUser(user);
        BigDecimal subtotal = BigDecimal.ZERO;

        for (OrderItem item : items) {
            Part part = partRepository.findById(item.getPart().getId()).orElseThrow();
            item.setPriceAtPurchase(part.getPrice());
            subtotal = subtotal.add(part.getPrice().multiply(new BigDecimal(item.getQuantity())));
        }

        BigDecimal discount = BigDecimal.ZERO;
        if (coupon != null && subtotal.compareTo(coupon.getMinSpend()) >= 0) {
            if ("PERCENTAGE".equals(coupon.getDiscountType())) {
                discount = subtotal.multiply(coupon.getDiscountValue().divide(new BigDecimal("100")));
            } else {
                discount = coupon.getDiscountValue();
            }
            order.setCouponCode(coupon.getCode());
        }

        order.setItems(items);
        order.setDiscountAmount(discount);
        order.setTotalAmount(subtotal.subtract(discount));
        order.setStatus(OrderStatus.PENDING);
        order.setRefundedAmount(BigDecimal.ZERO);

        return orderRepository.save(order);
    }

    /**
     * PHASE 5: Courier Integration.
     * Updates order to SHIPPED and attaches tracking details.
     */
    @Transactional
    public Order shipOrder(Long orderId, String courierName, String trackingNumber) {
        Order order = orderRepository.findById(orderId).orElseThrow();

        // Ensure all items were picked in warehouse before shipping
        boolean allPicked = order.getItems().stream()
                .allMatch(item -> item.getPickedQuantity().equals(item.getQuantity()));

        if (!allPicked) {
            throw new RuntimeException("Cannot ship: Order has not been fully picked in the warehouse.");
        }

        order.setStatus(OrderStatus.SHIPPED);
        order.setCourierName(courierName);
        order.setTrackingNumber(trackingNumber);
        order.setShippedDate(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);
        emailService.sendShippingNotification(savedOrder);
        return savedOrder;
    }

    @Transactional
    public Order processRefund(Long orderId, BigDecimal amount, boolean restock) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setRefundedAmount(amount);
        order.setStatus(OrderStatus.REFUNDED);
        if (restock) {
            for (OrderItem item : order.getItems()) {
                Part p = item.getPart();
                p.setStockQuantity(p.getStockQuantity() + item.getQuantity());
                partRepository.save(p);
            }
        }
        return orderRepository.save(order);
    }

    public BigDecimal calculateTotalRevenue() {
        return orderRepository.findAll().stream()
                .filter(o -> o.getStatus() == OrderStatus.COMPLETED || o.getStatus() == OrderStatus.SHIPPED)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<Order> getOrdersByUser(User user) { return orderRepository.findByUser(user); }
    public List<Order> getAllOrders() { return orderRepository.findAll(); }
    public long getTotalOrderCount() { return orderRepository.count(); }

    public Order getOrderByIdSecurely(Long id, String username) {
        return orderRepository.findById(id).orElseThrow(() -> new RuntimeException("Order not found"));
    }
}