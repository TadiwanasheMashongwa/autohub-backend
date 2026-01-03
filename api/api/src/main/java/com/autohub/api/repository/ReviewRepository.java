package com.autohub.api.repository;

import com.autohub.api.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    // Allows us to fetch all reviews for a specific part (e.g., for the product page)
    List<Review> findByPartId(Long partId);

    // Allows us to see all reviews written by a specific user
    List<Review> findByUserId(Long userId);
}