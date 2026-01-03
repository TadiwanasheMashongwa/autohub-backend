package com.autohub.api.service;

import com.autohub.api.model.*;
import com.autohub.api.repository.AuditLogRepository;
import com.autohub.api.repository.IdempotencyRepository;
import com.autohub.api.repository.OrderRepository;
import com.autohub.api.repository.PartRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final PartRepository partRepository;
    private final EmailService emailService;
    private final IdempotencyRepository idempotencyRepository;
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public OrderService(OrderRepository orderRepository,
                        PartRepository partRepository,
                        EmailService emailService,
                        IdempotencyRepository idempotencyRepository,
                        AuditLogRepository auditLogRepository,
                        ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.partRepository = partRepository;
        this.emailService = emailService;
        this.idempotencyRepository = idempotencyRepository;
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Order checkoutCart(User user, String idempotencyKey) {
        if (idempotencyKey != null) {
            Optional<IdempotencyRecord> record = idempotencyRepository.findById(idempotencyKey);
            if (record.isPresent()) {
                try {
                    return objectMapper.readValue(record.get().getResponseBody(), Order.class);
                } catch (Exception e) {
                    throw new RuntimeException("Error retrieving cached order");
                }
            }
        }

        Cart cart = user.getCart();
        if (cart == null || cart.getItems().isEmpty()) {
            throw new RuntimeException("Cannot checkout an empty cart");
        }

        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cart.getItems()) {
            OrderItem oi = new OrderItem();
            oi.setPart(cartItem.getPart());
            oi.setQuantity(cartItem.getQuantity());
            orderItems.add(oi);
        }

        // Create Order as PENDING_PAYMENT
        Order order = createOrderWithCoupon(user, orderItems, cart.getAppliedCoupon());
        cart.getItems().clear();
        cart.setAppliedCoupon(null);

        if (idempotencyKey != null) {
            try {
                String responseBody = objectMapper.writeValueAsString(order);
                idempotencyRepository.save(new IdempotencyRecord(idempotencyKey, responseBody, 200));
            } catch (Exception e) {
                System.err.println("Idempotency save failed: " + e.getMessage());
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
            Part part = partRepository.findById(item.getPart().getId())
                    .orElseThrow(() -> new RuntimeException("Part not found"));

            item.setPriceAtPurchase(part.getPrice());
            BigDecimal itemTotal = part.getPrice().multiply(new BigDecimal(item.getQuantity()));
            subtotal = subtotal.add(itemTotal);
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
        order.setStatus(OrderStatus.PENDING); // Awaiting Payment confirmation

        return orderRepository.save(order);
    }

    // --- FINANCIAL INTEGRITY: CONFIRM PAYMENT ---
    @Transactional
    public Order confirmPayment(Long orderId, String paymentId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Order is not in a state to be paid.");
        }

        // Deduct Stock NOW that money is confirmed
        for (OrderItem item : order.getItems()) {
            Part part = item.getPart();
            if (part.getStockQuantity() < item.getQuantity()) {
                throw new RuntimeException("Stock sold out during payment process for: " + part.getName());
            }
            part.setStockQuantity(part.getStockQuantity() - item.getQuantity());
            partRepository.save(part);
        }

        order.setPaymentId(paymentId);
        order.setPaymentStatus("SUCCEEDED");
        order.setStatus(OrderStatus.COMPLETED);
        Order savedOrder = orderRepository.save(order);

        // Audit & Notify
        auditLogRepository.save(new AuditLog("PAYMENT_CONFIRMED", "SYSTEM", "Order #" + orderId + " paid via " + paymentId));
        emailService.sendOrderConfirmation(savedOrder);

        return savedOrder;
    }

    @Transactional
    public Order shipOrder(Long orderId, String courierName, String trackingNumber) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setStatus(OrderStatus.SHIPPED);
        order.setCourierName(courierName);
        order.setTrackingNumber(trackingNumber);
        order.setShippedDate(LocalDateTime.now());
        return orderRepository.save(order);
    }

    @Transactional
    public Order processRefund(Long orderId, BigDecimal amount, boolean restockItems) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setRefundedAmount(amount);
        order.setStatus(OrderStatus.REFUNDED);
        if (restockItems) {
            for (OrderItem item : order.getItems()) {
                Part part = item.getPart();
                part.setStockQuantity(part.getStockQuantity() + item.getQuantity());
                partRepository.save(part);
            }
        }
        return orderRepository.save(order);
    }

    public BigDecimal calculateTotalRevenue() {
        return orderRepository.findAll().stream()
                .filter(o -> o.getStatus() == OrderStatus.COMPLETED || o.getStatus() == OrderStatus.SHIPPED)
                .map(o -> o.getTotalAmount().subtract(o.getRefundedAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public long getTotalOrderCount() { return orderRepository.count(); }
}