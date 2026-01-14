package com.autohub.api.service;

import com.autohub.api.model.*;
import com.autohub.api.repository.InventoryReservationRepository;
import com.autohub.api.repository.PartRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InventoryService {

    private final PartRepository partRepository;
    private final InventoryReservationRepository reservationRepository;

    public InventoryService(
            PartRepository partRepository,
            InventoryReservationRepository reservationRepository
    ) {
        this.partRepository = partRepository;
        this.reservationRepository = reservationRepository;
    }

    /**
     * STEP 5.1 — Reserve stock at checkout (NO deduction)
     */
    @Transactional
    public void reserveInventory(Order order) {

        for (OrderItem item : order.getItems()) {
            Part part = item.getPart();

            if (part.getStockQuantity() < item.getQuantity()) {
                throw new RuntimeException(
                        "Insufficient stock for SKU: " + part.getSku()
                );
            }

            InventoryReservation reservation =
                    new InventoryReservation(part, order, item.getQuantity());

            reservationRepository.save(reservation);
        }
    }

    /**
     * STEP 5.2 — Convert reservation → deduction (EXACTLY ONCE)
     */
    @Transactional
    public void deductReservedInventory(Order order) {

        List<InventoryReservation> reservations =
                reservationRepository.findByOrderIdAndActiveTrue(order.getId());

        for (InventoryReservation reservation : reservations) {
            Part part = reservation.getPart();

            part.setStockQuantity(
                    part.getStockQuantity() - reservation.getQuantity()
            );

            reservation.setActive(false);
            partRepository.save(part);
        }
    }

    /**
     * STEP 5.3 — Release inventory on failure/cancel
     */
    @Transactional
    public void releaseReservations(Order order) {

        List<InventoryReservation> reservations =
                reservationRepository.findByOrderIdAndActiveTrue(order.getId());

        for (InventoryReservation reservation : reservations) {
            reservation.setActive(false);
        }
    }
}
