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

    @Transactional
    public Order createOrder(User user, List<OrderItem> items) {
        Order order = new Order();
        order.setUser(user);

        BigDecimal total = BigDecimal.ZERO;

        for (OrderItem item : items) {
            // 1. Verify Part exists and check stock
            Part part = partRepository.findById(item.getPart().getId())
                    .orElseThrow(() -> new RuntimeException("Part not found"));

            if (part.getStockQuantity() < item.getQuantity()) {
                throw new RuntimeException("Insufficient stock for: " + part.getName());
            }

            // 2. Deduct Stock
            part.setStockQuantity(part.getStockQuantity() - item.getQuantity());
            partRepository.save(part);

            // 3. Set Price at time of purchase
            item.setPriceAtPurchase(part.getPrice());

            // 4. Update Totals
            BigDecimal itemTotal = part.getPrice().multiply(new BigDecimal(item.getQuantity()));
            total = total.add(itemTotal);
        }

        order.setItems(items);
        order.setTotalAmount(total);
        order.setStatus(OrderStatus.COMPLETED);

        return orderRepository.save(order);
    }
}