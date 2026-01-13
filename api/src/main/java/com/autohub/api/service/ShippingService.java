package com.autohub.api.service;

import com.autohub.api.model.Order;
import com.autohub.api.model.OrderItem;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ShippingService {

    /**
     * AUDIT #3.4: Generate Shipping Manifest.
     * Provides the Warehouse Clerks with all necessary data to pack and label the order.
     */
    public Map<String, Object> generateManifest(Order order) {
        double totalWeight = 0.0;
        int totalItems = 0;

        // FIXED: Using .put() instead of .add() for the HashMap
        List<Map<String, Object>> itemDetails = order.getItems().stream().map(item -> {
            Map<String, Object> details = new HashMap<>();
            details.put("sku", item.getPart().getSku());
            details.put("name", item.getPart().getName());
            details.put("quantity", item.getQuantity());
            details.put("binLocation", item.getPart().getBinLocation());
            return details;
        }).collect(Collectors.toList());

        for (OrderItem item : order.getItems()) {
            Double partWeight = item.getPart().getWeight();
            if (partWeight != null) {
                totalWeight += (partWeight * item.getQuantity());
            }
            totalItems += item.getQuantity();
        }

        Map<String, Object> manifest = new HashMap<>();
        manifest.put("orderId", order.getId());
        manifest.put("customerName", order.getUser().getFirstName() + " " + order.getUser().getLastName());
        manifest.put("shippingAddress", order.getUser().getAddress());
        manifest.put("contactNumber", order.getUser().getPhoneNumber());
        manifest.put("items", itemDetails);
        manifest.put("totalWeightKg", totalWeight);
        manifest.put("itemCount", totalItems);
        manifest.put("status", order.getStatus());

        return manifest;
    }
}