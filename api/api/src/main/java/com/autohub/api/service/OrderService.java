package com.autohub.api.service;

import com.autohub.api.model.*;
import com.autohub.api.repository.OrderRepository;
import com.autohub.api.repository.PartRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final PartRepository partRepository;

    public OrderService(OrderRepository orderRepository, PartRepository partRepository) {
        this.orderRepository = orderRepository;
        this.partRepository = partRepository;
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

    // NEW: Centralized method to update status
    @Transactional
    public Order updateStatus(Long id, OrderStatus status) {
        Order order = getOrderById(id);
        order.setStatus(status);
        return orderRepository.save(order);
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

        return orderRepository.save(order);
    }
}