package com.autohub.api.repository;

import com.autohub.api.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByPartId(Long partId);

    List<Review> findByUserId(Long userId);

    boolean existsByUserIdAndPartId(Long userId, Long partId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.part.id = :partId")
    Optional<Double> getAverageRatingForPart(@Param("partId") Long partId);
}
