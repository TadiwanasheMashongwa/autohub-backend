package com.autohub.api.repository;

import com.autohub.api.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    /**
     * AUDIT #9.2: Browse reviews for a specific part.
     */
    List<Review> findByPartId(Long partId);

    /**
     * Retrieve all reviews written by a specific user.
     */
    List<Review> findByUserId(Long userId);

    /**
     * PHASE 4: Integrity Guard.
     * NEW: Prevents a user from adding multiple reviews for the same part.
     * Ensures Mike's ratings are fair and not manipulated.
     */
    boolean existsByUserIdAndPartId(Long userId, Long partId);

    /**
     * PHASE 4: Rating Aggregation.
     * Calculates the mean rating for a specific part across all reviews.
     */
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.part.id = :partId")
    Optional<Double> getAverageRatingForPart(@Param("partId") Long partId);
}