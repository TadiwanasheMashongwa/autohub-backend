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
 * Workflow v2.4: Optimized transaction commit via DB aggregation.
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
        // 1. Verify Purchaser (Confirmed working for User 21 / Part 2)
        if (!orderRepository.hasUserPurchasedPart(user.getId(), partId)) {
            throw new RuntimeException("Only verified purchasers can review this part.");
        }

        Part part = partRepository.findById(partId)
                .orElseThrow(() -> new RuntimeException("Part not found"));

        // 2. Create and Save the Review independently to lock the record
        Review review = new Review();
        review.setUser(user);
        review.setPart(part);
        review.setRating(rating);
        review.setComment(comment);

        Review savedReview = reviewRepository.save(review);

        // 3. Update Part Average Rating using fresh DB calculation
        updatePartRating(partId);

        return savedReview;
    }

    private void updatePartRating(Long partId) {
        Part part = partRepository.findById(partId).orElseThrow();

        // Get average from DB instead of streaming the collection in-memory
        Double average = reviewRepository.getAverageRatingForPart(partId).orElse(0.0);

        // Precision rounding to satisfy PostgreSQL numeric constraints
        BigDecimal bd = BigDecimal.valueOf(average).setScale(1, RoundingMode.HALF_UP);
        part.setAverageRating(bd.doubleValue());

        partRepository.save(part);
    }
}