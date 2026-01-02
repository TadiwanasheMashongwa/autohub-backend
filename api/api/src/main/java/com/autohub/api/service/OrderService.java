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

        Order order = createOrder(user, orderItems);
        cart.getItems().clear();
        return order;
    }

    public List<Order> getOrdersByUser(User user) {
        return orderRepository.findByUser(user);
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
    }

    public BigDecimal calculateTotalRevenue() {
        return orderRepository.findAll().stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public long getTotalOrderCount() {
        return orderRepository.count();
    }

    @Transactional
    public Order updateStatus(Long id, OrderStatus status) {
        Order order = getOrderById(id);
        order.setStatus(status);
        Order updatedOrder = orderRepository.save(order);

        // Notify user of status change
        try {
            emailService.sendOrderConfirmation(updatedOrder);
        } catch (Exception e) {
            System.err.println("Email notification failed: " + e.getMessage());
        }

        return updatedOrder;
    }

    @Transactional
    public Order createOrder(User user, List<OrderItem> items) {
        Order order = new Order();
        order.setUser(user);
        BigDecimal total = BigDecimal.ZERO;

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
            total = total.add(itemTotal);
        }

        order.setItems(items);
        order.setTotalAmount(total);
        order.setStatus(OrderStatus.PENDING);

        Order savedOrder = orderRepository.save(order);

        // Send confirmation email
        try {
            emailService.sendOrderConfirmation(savedOrder);
        } catch (Exception e) {
            System.err.println("Email notification failed: " + e.getMessage());
        }

        return savedOrder;
    }
}