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

    List<Review> findByPartId(Long partId);

    List<Review> findByUserId(Long userId);

    boolean existsByUserIdAndPartId(Long userId, Long partId);

    /**
     * PHASE 4.1 & 4.2: Moderation & Sentiment Analysis.
     */
    List<Review> findAllByOrderByCreatedAtDesc();

    @Query("SELECT r FROM Review r WHERE r.rating <= 2.0 ORDER BY r.createdAt DESC")
    List<Review> findNegativeSentimentReviews();

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.part.id = :partId")
    Optional<Double> getAverageRatingForPart(@Param("partId") Long partId);
}