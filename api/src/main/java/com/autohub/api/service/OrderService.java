package com.autohub.api.service;

import com.autohub.api.model.*;
import com.autohub.api.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                    return objectMapper.readValue(
                            cached.get().getResponseBody(),
                            Order.class
                    );
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

        PricingService.PricingResult pricing =
                pricingService.calculatePricing(cart);

        Order order = new Order();
        order.setUser(user);
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

        inventoryService.reserveInventory(savedOrder);

        cart.getItems().clear();
        cart.setAppliedCoupon(null);
        cartRepository.save(cart);

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

    public BigDecimal calculateTotalRevenue() {
        return orderRepository.findAll().stream()
                .filter(o -> o.getStatus() == OrderStatus.PAID || o.getStatus() == OrderStatus.COMPLETED)
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
        manifest.put("courier", order.getCourierName());
        manifest.put("trackingNumber", order.getTrackingNumber());

        return manifest;
    }
}
