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
     * PHASE 4: Payment Confirmation & Stock Release.
     */
    @Transactional
    public Order confirmPayment(Long orderId, String paymentId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Payment cannot be confirmed. Current status: " + order.getStatus());
        }

        for (OrderItem item : order.getItems()) {
            Part part = partRepository.findById(item.getPart().getId())
                    .orElseThrow(() -> new RuntimeException("Part not found: " + item.getPart().getSku()));

            if (part.getStockQuantity() < item.getQuantity()) {
                throw new RuntimeException("Insufficient stock for SKU: " + part.getSku());
            }

            part.setStockQuantity(part.getStockQuantity() - item.getQuantity());
            partRepository.save(part);

            auditLogRepository.save(new AuditLog("STOCK_DEDUCTION", "SYSTEM_PAYMENT",
                    "Deducted " + item.getQuantity() + " for SKU: " + part.getSku()));
        }

        order.setPaymentId(paymentId);
        order.setPaymentStatus("SUCCEEDED");
        order.setStatus(OrderStatus.COMPLETED);

        Order savedOrder = orderRepository.save(order);
        emailService.sendOrderConfirmation(savedOrder);

        return savedOrder;
    }

    /**
     * PHASE 6: Checkout Logic & Email Trigger 1.
     */
    @Transactional
    public Order checkoutCart(User user, String idempotencyKey) {
        if (idempotencyKey != null) {
            Optional<IdempotencyRecord> record = idempotencyRepository.findById(idempotencyKey);
            if (record.isPresent()) {
                try {
                    return objectMapper.readValue(record.get().getResponseBody(), Order.class);
                } catch (Exception e) { throw new RuntimeException("Checkout recovery failed"); }
            }
        }

        Cart cart = user.getCart();
        if (cart == null || cart.getItems().isEmpty()) throw new RuntimeException("Cart is empty");

        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem ci : cart.getItems()) {
            OrderItem oi = new OrderItem();
            oi.setPart(ci.getPart());
            oi.setQuantity(ci.getQuantity());
            oi.setPickedQuantity(0);
            orderItems.add(oi);
        }

        Order order = createOrderWithCoupon(user, orderItems, cart.getAppliedCoupon());
        cart.getItems().clear();
        cart.setAppliedCoupon(null);

        emailService.sendOrderReceivedEmail(order);

        if (idempotencyKey != null) {
            try {
                idempotencyRepository.save(new IdempotencyRecord(idempotencyKey, objectMapper.writeValueAsString(order), 200));
            } catch (Exception e) { /* ignored */ }
        }
        return order;
    }

    /**
     * PHASE 3: Barcode Picking.
     */
    @Transactional
    public Order verifyAndPickItem(Long orderId, String barcode) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        OrderItem item = order.getItems().stream()
                .filter(i -> i.getPart().getBarcode().equals(barcode))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Barcode mismatch."));

        if (item.getPickedQuantity() >= item.getQuantity()) {
            throw new RuntimeException("Already picked.");
        }

        item.setPickedQuantity(item.getPickedQuantity() + 1);
        String clerk = SecurityContextHolder.getContext().getAuthentication().getName();
        auditLogRepository.save(new AuditLog("WAREHOUSE_PICK", clerk, "Order #" + orderId));
        return orderRepository.save(order);
    }

    /**
     * PHASE 5: Courier Integration.
     */
    @Transactional
    public Order shipOrder(Long orderId, String courierName, String trackingNumber) {
        Order order = orderRepository.findById(orderId).orElseThrow();

        boolean allPicked = order.getItems().stream()
                .allMatch(i -> i.getPickedQuantity().equals(i.getQuantity()));

        if (!allPicked) throw new RuntimeException("Cannot ship: Not all items picked.");

        order.setStatus(OrderStatus.SHIPPED);
        order.setCourierName(courierName);
        order.setTrackingNumber(trackingNumber);
        order.setShippedDate(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);
        emailService.sendShippingNotification(savedOrder);
        return savedOrder;
    }

    /**
     * PHASE 5/6: Final Delivery.
     */
    @Transactional
    public Order markAsDelivered(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveryDate(LocalDateTime.now()); // Now correctly mapped to Updated Order.java

        Order savedOrder = orderRepository.save(order);
        emailService.sendDeliveryConfirmation(savedOrder);
        return savedOrder;
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

    @Transactional
    public Order transitOrder(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setStatus(OrderStatus.IN_TRANSIT);
        return orderRepository.save(order);
    }

    public BigDecimal calculateTotalRevenue() {
        return orderRepository.findAll().stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED || o.getStatus() == OrderStatus.COMPLETED)
                .map(o -> o.getTotalAmount().subtract(o.getRefundedAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
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

    public Map<String, Object> getOrderManifest(Long orderId) {
        return shippingService.generateManifest(orderRepository.findById(orderId).orElseThrow());
    }

    public List<Order> getOrdersByUser(User user) { return orderRepository.findByUser(user); }
    public List<Order> getAllOrders() { return orderRepository.findAll(); }
    public long getTotalOrderCount() { return orderRepository.count(); }
    public Order getOrderByIdSecurely(Long id, String u) { return orderRepository.findById(id).orElseThrow(); }
}