package com.autohub.api.service;

import com.autohub.api.model.*;
import com.autohub.api.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * CORE ORCHESTRATOR: Manages Order Lifecycle, Warehouse Picking, and Financials.
 * Synchronized with Master 53-endpoint list (v10.4.6).
 */
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
     * PHASE 6: Checkout Logic with Idempotency Support.
     * ENDPOINT #39: Prevents duplicate orders during network flickers.
     */
    @Transactional
    public Order checkoutCart(User user, String idempotencyKey) {
        if (idempotencyKey != null) {
            Optional<IdempotencyRecord> record = idempotencyRepository.findById(idempotencyKey);
            if (record.isPresent()) {
                try {
                    return objectMapper.readValue(record.get().getResponseBody(), Order.class);
                } catch (Exception e) {
                    throw new RuntimeException("Idempotency recovery failed");
                }
            }
        }

        Cart cart = user.getCart();
        if (cart == null || cart.getItems().isEmpty()) throw new RuntimeException("Cart is empty");

        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem ci : cart.getItems()) {
            OrderItem oi = new OrderItem();
            oi.setPart(ci.getPart());
            oi.setQuantity(ci.getQuantity());
            oi.setPickedQuantity(0); // Initialize for warehouse flow
            orderItems.add(oi);
        }

        Order order = createOrderWithCoupon(user, orderItems, cart.getAppliedCoupon());
        cart.getItems().clear();
        cart.setAppliedCoupon(null);

        // TRIGGER 1: Order Received Email (#1.6)
        emailService.sendOrderReceivedEmail(order);

        if (idempotencyKey != null) {
            try {
                String responseBody = objectMapper.writeValueAsString(order);
                idempotencyRepository.save(new IdempotencyRecord(idempotencyKey, responseBody, 200));
            } catch (Exception e) { /* Log locally */ }
        }
        return order;
    }

    /**
     * PHASE 4: Payment Confirmation & Stock Release.
     * Triggered after successful payment gateway callback.
     */
    @Transactional
    public Order confirmPayment(Long orderId, String paymentId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Cannot confirm payment. Status is: " + order.getStatus());
        }

        // Atomically deduct stock
        for (OrderItem item : order.getItems()) {
            Part part = partRepository.findById(item.getPart().getId()).orElseThrow();

            if (part.getStockQuantity() < item.getQuantity()) {
                throw new RuntimeException("Inventory exhausted for SKU: " + part.getSku());
            }

            part.setStockQuantity(part.getStockQuantity() - item.getQuantity());
            partRepository.save(part);
            auditLogRepository.save(new AuditLog("STOCK_DEDUCTION", "SYSTEM", "Order #" + orderId));
        }

        order.setPaymentId(paymentId);
        order.setPaymentStatus("SUCCEEDED");
        order.setStatus(OrderStatus.COMPLETED);

        Order savedOrder = orderRepository.save(order);
        emailService.sendOrderConfirmation(savedOrder); // TRIGGER 2
        return savedOrder;
    }

    /**
     * ENDPOINT #44: Warehouse Picking Verification.
     * Ensures Clerks pull the correct barcode before order can ship.
     */
    @Transactional
    public Order verifyAndPickItem(Long orderId, String barcode) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        OrderItem item = order.getItems().stream()
                .filter(i -> i.getPart().getBarcode().equals(barcode))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Barcode does not match any item in this order."));

        if (item.getPickedQuantity() >= item.getQuantity()) {
            throw new RuntimeException("Item already fully picked.");
        }

        item.setPickedQuantity(item.getPickedQuantity() + 1);
        String clerk = SecurityContextHolder.getContext().getAuthentication().getName();
        auditLogRepository.save(new AuditLog("WAREHOUSE_PICK", clerk, "Order #" + orderId + " - Item: " + barcode));
        return orderRepository.save(order);
    }

    /**
     * ENDPOINT #45: Ship Order logic.
     */
    @Transactional
    public Order shipOrder(Long id, String courier, String tracking) {
        Order order = orderRepository.findById(id).orElseThrow();

        // Safety Guard: Require picking to be 100% complete
        boolean allPicked = order.getItems().stream()
                .allMatch(i -> i.getPickedQuantity() != null && i.getPickedQuantity().equals(i.getQuantity()));
        if (!allPicked) throw new RuntimeException("Picking incomplete. Cannot ship.");

        order.setStatus(OrderStatus.SHIPPED);
        order.setCourierName(courier);
        order.setTrackingNumber(tracking);
        order.setShippedDate(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);
        emailService.sendShippingNotification(savedOrder); // TRIGGER 3
        return savedOrder;
    }

    /**
     * ENDPOINT #49: Process Refund with Optional Restocking.
     */
    @Transactional
    public Order processRefund(Long id, BigDecimal amount, boolean restock) {
        Order o = orderRepository.findById(id).orElseThrow();
        o.setRefundedAmount(amount);
        o.setStatus(OrderStatus.REFUNDED);

        if (restock) {
            for (OrderItem i : o.getItems()) {
                Part p = i.getPart();
                p.setStockQuantity(p.getStockQuantity() + i.getQuantity());
                partRepository.save(p);
            }
        }
        auditLogRepository.save(new AuditLog("REFUND_ISSUED", "ADMIN", "Order #" + id + ": $" + amount));
        return orderRepository.save(o);
    }

    /**
     * ENDPOINT #51: Dashboard Financial Aggregation.
     */
    public BigDecimal calculateTotalRevenue() {
        return orderRepository.findAll().stream()
                .filter(o -> List.of(OrderStatus.DELIVERED, OrderStatus.COMPLETED, OrderStatus.SHIPPED).contains(o.getStatus()))
                .map(o -> o.getTotalAmount().subtract(o.getRefundedAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // --- SUPPORTING LOGIC ---

    @Transactional
    public Order transitOrder(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setStatus(OrderStatus.IN_TRANSIT);
        return orderRepository.save(order);
    }

    public long getTotalOrderCount() { return orderRepository.count(); }
    public List<Order> getAllOrders() { return orderRepository.findAll(); }
    public List<Order> getOrdersByUser(User u) { return orderRepository.findByUser(u); }
    public Map<String, Object> getOrderManifest(Long id) {
        return shippingService.generateManifest(orderRepository.findById(id).orElseThrow());
    }

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
            discount = "PERCENTAGE".equals(coupon.getDiscountType())
                    ? subtotal.multiply(coupon.getDiscountValue().divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP))
                    : coupon.getDiscountValue();
            order.setCouponCode(coupon.getCode());
        }

        order.setItems(items);
        order.setDiscountAmount(discount);
        order.setTotalAmount(subtotal.subtract(discount));
        order.setStatus(OrderStatus.PENDING);
        order.setRefundedAmount(BigDecimal.ZERO);
        return orderRepository.save(order);
    }
}