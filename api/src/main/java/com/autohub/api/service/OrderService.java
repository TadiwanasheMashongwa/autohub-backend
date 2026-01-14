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
import java.util.*;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final PartRepository partRepository;
    private final VehicleRepository vehicleRepository;
    private final EmailService emailService;
    private final IdempotencyRepository idempotencyRepository;
    private final AuditLogRepository auditLogRepository;
    private final ShippingService shippingService;
    private final ObjectMapper objectMapper;

    public OrderService(
            OrderRepository orderRepository,
            PartRepository partRepository,
            VehicleRepository vehicleRepository,
            EmailService emailService,
            IdempotencyRepository idempotencyRepository,
            AuditLogRepository auditLogRepository,
            ShippingService shippingService,
            ObjectMapper objectMapper
    ) {
        this.orderRepository = orderRepository;
        this.partRepository = partRepository;
        this.vehicleRepository = vehicleRepository;
        this.emailService = emailService;
        this.idempotencyRepository = idempotencyRepository;
        this.auditLogRepository = auditLogRepository;
        this.shippingService = shippingService;
        this.objectMapper = objectMapper;
    }

    /**
     * CHECKOUT — SINGLE SOURCE OF TRUTH
     * STEP 1 CHANGE: vehicleId is mandatory and compatibility is enforced here only.
     */
    @Transactional
    public Order checkoutCart(User user, Long vehicleId, String idempotencyKey) {

        if (vehicleId == null) {
            throw new RuntimeException("Vehicle selection is required at checkout.");
        }

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found with ID: " + vehicleId));

        // --- Idempotency Guard ---
        if (idempotencyKey != null) {
            Optional<IdempotencyRecord> record = idempotencyRepository.findById(idempotencyKey);
            if (record.isPresent()) {
                try {
                    return objectMapper.readValue(record.get().getResponseBody(), Order.class);
                } catch (Exception e) {
                    throw new RuntimeException("Idempotency recovery failed.");
                }
            }
        }

        Cart cart = user.getCart();
        if (cart == null || cart.getItems().isEmpty()) {
            throw new RuntimeException("Cart is empty.");
        }

        // --- STEP 1: VEHICLE COMPATIBILITY ENFORCEMENT ---
        for (CartItem cartItem : cart.getItems()) {
            Part part = cartItem.getPart();
            boolean compatible = part.getCompatibleVehicles()
                    .stream()
                    .anyMatch(v -> v.getId().equals(vehicle.getId()));

            if (!compatible) {
                throw new RuntimeException(
                        "Part '" + part.getName() +
                                "' is not compatible with vehicle " +
                                vehicle.getMake() + " " + vehicle.getModel()
                );
            }
        }

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (CartItem ci : cart.getItems()) {
            Part part = partRepository.findById(ci.getPart().getId())
                    .orElseThrow(() -> new RuntimeException("Part not found during checkout."));

            OrderItem oi = new OrderItem();
            oi.setPart(part);
            oi.setQuantity(ci.getQuantity());
            oi.setPickedQuantity(0);
            oi.setPriceAtPurchase(part.getPrice());

            subtotal = subtotal.add(
                    part.getPrice().multiply(BigDecimal.valueOf(ci.getQuantity()))
            );
            orderItems.add(oi);
        }

        Order order = new Order();
        order.setUser(user);
        order.setItems(orderItems);
        order.setStatus(OrderStatus.PENDING);
        order.setRefundedAmount(BigDecimal.ZERO);

        BigDecimal discount = BigDecimal.ZERO;
        if (cart.getAppliedCoupon() != null &&
                subtotal.compareTo(cart.getAppliedCoupon().getMinSpend()) >= 0) {

            Coupon coupon = cart.getAppliedCoupon();
            discount = "PERCENTAGE".equals(coupon.getDiscountType())
                    ? subtotal.multiply(
                    coupon.getDiscountValue()
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP))
                    : coupon.getDiscountValue();

            order.setCouponCode(coupon.getCode());
        }

        order.setDiscountAmount(discount);
        order.setTotalAmount(subtotal.subtract(discount));

        Order savedOrder = orderRepository.save(order);

        cart.getItems().clear();
        cart.setAppliedCoupon(null);

        emailService.sendOrderReceivedEmail(savedOrder);

        if (idempotencyKey != null) {
            try {
                idempotencyRepository.save(
                        new IdempotencyRecord(
                                idempotencyKey,
                                objectMapper.writeValueAsString(savedOrder),
                                200
                        )
                );
            } catch (Exception ignored) {}
        }

        return savedOrder;
    }

    // ---------------- EXISTING METHODS (UNMODIFIED BEHAVIOR) ----------------

    public List<Order> getOrdersByUser(User u) {
        return orderRepository.findByUser(u);
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public long getTotalOrderCount() {
        return orderRepository.count();
    }

    public Order getOrderByIdSecurely(Long id, String email) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + id));

        boolean isAdmin = SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities()
                .stream()
                .anyMatch(a ->
                        a.getAuthority().equals("ROLE_ADMIN") ||
                                a.getAuthority().equals("ROLE_CLERK")
                );

        if (!isAdmin && !order.getUser().getEmail().equals(email)) {
            throw new RuntimeException("Access Denied: You do not own this order.");
        }
        return order;
    }

    @Transactional
    public Order confirmPayment(Long orderId, String paymentId) {
        Order order = orderRepository.findById(orderId).orElseThrow();

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Cannot confirm payment for non-pending order.");
        }

        for (OrderItem item : order.getItems()) {
            Part part = partRepository.findById(item.getPart().getId()).orElseThrow();
            if (part.getStockQuantity() < item.getQuantity()) {
                throw new RuntimeException("Stock sold out for SKU: " + part.getSku());
            }
            part.setStockQuantity(part.getStockQuantity() - item.getQuantity());
            partRepository.save(part);
        }

        order.setPaymentId(paymentId);
        order.setPaymentStatus("SUCCEEDED");
        order.setStatus(OrderStatus.COMPLETED);

        Order savedOrder = orderRepository.save(order);
        emailService.sendOrderConfirmation(savedOrder);
        return savedOrder;
    }

    @Transactional
    public Order verifyAndPickItem(Long orderId, String barcode) {
        Order order = orderRepository.findById(orderId).orElseThrow();

        OrderItem item = order.getItems().stream()
                .filter(i -> i.getPart().getBarcode().equals(barcode))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Barcode mismatch."));

        if (item.getPickedQuantity() >= item.getQuantity()) {
            throw new RuntimeException("Quantity already picked.");
        }

        item.setPickedQuantity(item.getPickedQuantity() + 1);

        String clerk = SecurityContextHolder.getContext().getAuthentication().getName();
        auditLogRepository.save(new AuditLog(
                "WAREHOUSE_PICK",
                clerk,
                "Picked unit for Order #" + orderId
        ));

        return orderRepository.save(order);
    }

    @Transactional
    public Order shipOrder(Long id, String courier, String tracking) {
        Order order = orderRepository.findById(id).orElseThrow();

        boolean allPicked = order.getItems().stream()
                .allMatch(i -> i.getPickedQuantity().equals(i.getQuantity()));

        if (!allPicked) {
            throw new RuntimeException("Picking incomplete.");
        }

        order.setStatus(OrderStatus.SHIPPED);
        order.setCourierName(courier);
        order.setTrackingNumber(tracking);
        order.setShippedDate(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);
        emailService.sendShippingNotification(savedOrder);
        return savedOrder;
    }

    @Transactional
    public Order transitOrder(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setStatus(OrderStatus.IN_TRANSIT);
        return orderRepository.save(order);
    }

    @Transactional
    public Order processRefund(Long id, BigDecimal amount, boolean restock) {
        Order order = orderRepository.findById(id).orElseThrow();
        order.setRefundedAmount(amount);
        order.setStatus(OrderStatus.REFUNDED);

        if (restock) {
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
                .filter(o -> List.of(
                        OrderStatus.DELIVERED,
                        OrderStatus.COMPLETED,
                        OrderStatus.SHIPPED
                ).contains(o.getStatus()))
                .map(o -> o.getTotalAmount().subtract(o.getRefundedAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Map<String, Object> getOrderManifest(Long id) {
        return shippingService.generateManifest(
                orderRepository.findById(id).orElseThrow()
        );
    }

    @Transactional
    public Order updateStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setStatus(status);

        if (status == OrderStatus.DELIVERED || status == OrderStatus.COMPLETED) {
            order.setDeliveryDate(LocalDateTime.now());
            emailService.sendDeliveryConfirmation(order);
        }
        return orderRepository.save(order);
    }

    @Transactional
    public Order cancelOrder(Long id) {
        Order order = orderRepository.findById(id).orElseThrow();
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Only pending orders can be cancelled.");
        }
        order.setStatus(OrderStatus.CANCELLED);
        auditLogRepository.save(new AuditLog(
                "ORDER_CANCELLED",
                "USER",
                "Order #" + id
        ));
        return orderRepository.save(order);
    }
}
