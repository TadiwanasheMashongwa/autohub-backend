package com.autohub.api.service;

import com.autohub.api.model.Part;
import com.autohub.api.model.Review;
import com.autohub.api.model.User;
import com.autohub.api.repository.OrderRepository;
import com.autohub.api.repository.PartRepository;
import com.autohub.api.repository.ReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Service managing product reviews.
 * Version 2026.01.05 - Fixed Transaction Commit Error with Rounding.
 */
@Service
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final PartRepository partRepository;
    private final OrderRepository orderRepository;

    public ReviewService(ReviewRepository reviewRepository, PartRepository partRepository, OrderRepository orderRepository) {
        this.reviewRepository = reviewRepository;
        this.partRepository = partRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public Review addReview(User user, Long partId, Integer rating, String comment) {
        // Step 1: Verification (SUCCESSFUL IN LOGS)
        if (!orderRepository.hasUserPurchasedPart(user.getId(), partId)) {
            throw new RuntimeException("Only verified purchasers can review this part.");
        }

        Part part = partRepository.findById(partId)
                .orElseThrow(() -> new RuntimeException("Part not found"));

        Review review = new Review();
        review.setUser(user);
        review.setPart(part);
        review.setRating(rating);
        review.setComment(comment);

        // Step 2: Save Review (SUCCESSFUL IN LOGS)
        Review savedReview = reviewRepository.save(review);

        // Step 3: Update Average Rating (STABILIZED WITH ROUNDING)
        updatePartRating(part);

        return savedReview;
    }

    private void updatePartRating(Part part) {
        double average = part.getReviews().stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);

        // Fix: Rounding to 1 decimal place prevents PostgreSQL scale errors
        BigDecimal bd = new BigDecimal(Double.toString(average));
        bd = bd.setScale(1, RoundingMode.HALF_UP);

        part.setAverageRating(bd.doubleValue());
        partRepository.save(part);
    }
}