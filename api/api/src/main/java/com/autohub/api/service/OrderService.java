package com.autohub.api.service;

import com.autohub.api.model.*;
import com.autohub.api.repository.OrderRepository;
import com.autohub.api.repository.PartRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final PartRepository partRepository;
    private final EmailService emailService;

    public OrderService(OrderRepository orderRepository, PartRepository partRepository, EmailService emailService) {
        this.orderRepository = orderRepository;
        this.partRepository = partRepository;
        this.emailService = emailService;
    }

    @Transactional
    public Order checkoutCart(User user) {
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

        // Pass the coupon to the creation logic
        Order order = createOrderWithCoupon(user, orderItems, cart.getAppliedCoupon());

        cart.getItems().clear();
        cart.setAppliedCoupon(null); // Reset coupon after use
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

            if (part.getStockQuantity() < item.getQuantity()) {
                throw new RuntimeException("Insufficient stock for: " + part.getName());
            }

            part.setStockQuantity(part.getStockQuantity() - item.getQuantity());
            partRepository.save(part);

            item.setPriceAtPurchase(part.getPrice());
            BigDecimal itemTotal = part.getPrice().multiply(new BigDecimal(item.getQuantity()));
            subtotal = subtotal.add(itemTotal);
        }

        // Apply Coupon Logic
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

        Order savedOrder = orderRepository.save(order);

        try {
            emailService.sendOrderConfirmation(savedOrder);
        } catch (Exception e) {
            System.err.println("Email notification failed: " + e.getMessage());
        }

        return savedOrder;
    }

    // Existing methods (getOrdersByUser, updateStatus, etc.) remain here...
}