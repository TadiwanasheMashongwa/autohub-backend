package com.autohub.api.service;

import com.autohub.api.model.AuditLog;
import com.autohub.api.model.Order;
import com.autohub.api.model.OrderItem;
import com.autohub.api.repository.AuditLogRepository;
import com.autohub.api.repository.OrderRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WarehouseService {

    private final OrderRepository orderRepository;
    private final AuditLogRepository auditLogRepository;

    public WarehouseService(OrderRepository orderRepository,
                            AuditLogRepository auditLogRepository) {
        this.orderRepository = orderRepository;
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * STEP 5.5 — Warehouse Picking (Barcode Verification)
     * Invariant:
     * - Cannot pick more than ordered
     * - Must match barcode to OrderItem.part
     */
    @Transactional
    public Order verifyAndPickItem(Long orderId, String barcode) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        OrderItem matchedItem = order.getItems().stream()
                .filter(item -> item.getPart().getBarcode().equals(barcode))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                        "No matching item found for barcode: " + barcode
                ));

        if (matchedItem.getPickedQuantity() >= matchedItem.getQuantity()) {
            throw new RuntimeException(
                    "Item already fully picked for SKU: " + matchedItem.getPart().getSku()
            );
        }

        matchedItem.setPickedQuantity(matchedItem.getPickedQuantity() + 1);

        auditLogRepository.save(new AuditLog(
                "WAREHOUSE_PICK",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                "Picked SKU " + matchedItem.getPart().getSku() +
                        " for Order #" + orderId
        ));

        return orderRepository.save(order);
    }
}
