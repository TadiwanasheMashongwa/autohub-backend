package com.autohub.api.service;

import com.autohub.api.model.Order;
import com.autohub.api.model.OrderItem;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
public class ShippingService {

    /**
     * Generates a manifest detail map for a given order.
     * Calculates total weight to help the warehouse select the right packaging.
     */
    public Map<String, Object> generateManifest(Order order) {
        double totalWeight = 0.0;
        int totalItems = 0;

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
        manifest.put("totalWeightKg", totalWeight);
        manifest.put("itemCount", totalItems);
        manifest.put("status", order.getStatus());

        return manifest;
    }
}