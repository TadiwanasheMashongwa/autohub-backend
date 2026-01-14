package com.autohub.api.service;

import com.autohub.api.model.*;
import com.autohub.api.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final PartRepository partRepository;
    private final CartRepository cartRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final EmailService emailService;
    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;

    public OrderService(
            OrderRepository orderRepository,
            PartRepository partRepository,
            CartRepository cartRepository,
            IdempotencyRepository idempotencyRepository,
            EmailService emailService,
            InventoryService inventoryService,
            ObjectMapper objectMapper
    ) {
        this.orderRepository = orderRepository;
        this.partRepository = partRepository;
        this.cartRepository = cartRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.emailService = emailService;
        this.inventoryService = inventoryService;
        this.objectMapper = objectMapper;
    }

    /**
     * STEP 5 — Checkout creates order + reserves inventory
     */
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

        Order order = new Order();
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING);

        List<OrderItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (CartItem ci : cart.getItems()) {
            Part part = partRepository.findById(ci.getPart().getId())
                    .orElseThrow(() -> new RuntimeException("Part missing"));

            OrderItem oi = new OrderItem();
            oi.setPart(part);
            oi.setQuantity(ci.getQuantity());
            oi.setPriceAtPurchase(part.getPrice());

            items.add(oi);

            total = total.add(
                    part.getPrice().multiply(BigDecimal.valueOf(ci.getQuantity()))
            );
        }

        order.setItems(items);
        order.setTotalAmount(total);

        Order savedOrder = orderRepository.save(order);

        // 🔒 Inventory is RESERVED here (not deducted)
        inventoryService.reserveInventory(savedOrder);

        cart.getItems().clear();
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
}
