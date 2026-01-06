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
 * Workflow v2.6: Atomic Native Persistence.
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
        // 1. Verify Purchaser
        if (!orderRepository.hasUserPurchasedPart(user.getId(), partId)) {
            throw new RuntimeException("Only verified purchasers can review this part.");
        }

        Part part = partRepository.findById(partId)
                .orElseThrow(() -> new RuntimeException("Part not found"));

        // 2. Save the Review
        Review review = new Review();
        review.setUser(user);
        review.setPart(part);
        review.setRating(rating);
        review.setComment(comment);

        Review savedReview = reviewRepository.save(review);

        // 3. Update Part Rating using NATIVE SQL (Bypasses @Version conflict)
        updatePartRatingNative(partId);

        return savedReview;
    }

    private void updatePartRatingNative(Long partId) {
        Double average = reviewRepository.getAverageRatingForPart(partId).orElse(0.0);

        // Precision rounding
        BigDecimal bd = BigDecimal.valueOf(average).setScale(1, RoundingMode.HALF_UP);

        // Use the native method from PartRepository
        partRepository.updateAverageRatingNative(partId, bd.doubleValue());
    }
}