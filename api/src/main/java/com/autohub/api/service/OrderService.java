package com.autohub.api.service;

import com.autohub.api.model.*;
import com.autohub.api.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.math.BigDecimal;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final EmailService emailService;
    private final InventoryService inventoryService;
    private final PricingService pricingService;
    private final ObjectMapper objectMapper;

    public OrderService(
            OrderRepository orderRepository,
            CartRepository cartRepository,
            IdempotencyRepository idempotencyRepository,
            EmailService emailService,
            InventoryService inventoryService,
            PricingService pricingService,
            ObjectMapper objectMapper
    ) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.emailService = emailService;
        this.inventoryService = inventoryService;
        this.pricingService = pricingService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Order checkoutCart(User user, String idempotencyKey) {
        if (idempotencyKey != null) {
            Optional<IdempotencyRecord> cached = idempotencyRepository.findById(idempotencyKey);
            if (cached.isPresent()) {
                try {
                    return objectMapper.readValue(cached.get().getResponseBody(), Order.class);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to restore idempotent order");
                }
            }
        }

        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Cannot checkout empty cart");
        }

        PricingService.PricingResult pricing = pricingService.calculatePricing(cart);

        Order order = new Order();
        order.setUser(user);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(OrderStatus.PENDING);
        order.setDiscountAmount(pricing.getDiscount());
        order.setCouponCode(pricing.getCouponCode());
        order.setTotalAmount(pricing.getTotal());

        List<OrderItem> items = new ArrayList<>();
        for (CartItem ci : cart.getItems()) {
            OrderItem oi = new OrderItem();
            oi.setPart(ci.getPart());
            oi.setQuantity(ci.getQuantity());
            oi.setPriceAtPurchase(ci.getPart().getPrice());
            items.add(oi);
        }

        order.setItems(items);
        Order savedOrder = orderRepository.save(order);

        // Initial reservation (Ghost Inventory Prevention - Checklist #4)
        inventoryService.reserveInventory(savedOrder);

        cart.getItems().clear();
        cart.setAppliedCoupon(null);
        cartRepository.save(cart);

        emailService.sendOrderReceivedEmail(savedOrder);

        if (idempotencyKey != null) {
            try {
                idempotencyRepository.save(new IdempotencyRecord(
                        idempotencyKey,
                        objectMapper.writeValueAsString(savedOrder),
                        200
                ));
            } catch (Exception ignored) {}
        }

        return savedOrder;
    }

    @Transactional
    public Order updateLogistics(Long orderId, String courier, String tracking) {
        Order order = getOrderById(orderId);
        order.setCourierName(courier);
        order.setTrackingNumber(tracking);
        order.setStatus(OrderStatus.SHIPPED);
        return orderRepository.save(order);
    }

    @Transactional
    public Order updateStatus(Long orderId, OrderStatus status) {
        Order order = getOrderById(orderId);
        order.setStatus(status);
        return orderRepository.save(order);
    }

    public List<Order> getAllActiveOrders() {
        return orderRepository.findAllActiveOrders();
    }

    public BigDecimal calculateTotalRevenue() {
        // Updated to include orders currently in the Warehouse workflow
        List<OrderStatus> revenueStatuses = Arrays.asList(
                OrderStatus.PAID,
                OrderStatus.PICKED,
                OrderStatus.SHIPPED,
                OrderStatus.IN_TRANSIT,
                OrderStatus.COMPLETED
        );

        return orderRepository.findAll().stream()
                .filter(o -> revenueStatuses.contains(o.getStatus()))
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public long getTotalOrderCount() {
        return orderRepository.count();
    }

    public Map<String, Object> getOrderManifest(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        Map<String, Object> manifest = new HashMap<>();
        manifest.put("orderId", order.getId());
        manifest.put("customer", order.getUser().getEmail());
        manifest.put("items", order.getItems());
        manifest.put("status", order.getStatus());
        manifest.put("courier", order.getCourierName());
        manifest.put("trackingNumber", order.getTrackingNumber());

        return manifest;
    }

    public List<Order> findOrdersByUser(User user) {
        return orderRepository.findByUserOrderByOrderDateDesc(user);
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));
    }
}