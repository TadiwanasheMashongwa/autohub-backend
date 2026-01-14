package com.autohub.api.repository;

import com.autohub.api.model.InventoryReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, Long> {

    List<InventoryReservation> findByOrderIdAndActiveTrue(Long orderId);
}
