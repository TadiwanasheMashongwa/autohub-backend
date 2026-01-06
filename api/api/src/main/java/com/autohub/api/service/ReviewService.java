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
 * Workflow v2.7: Duplicate Prevention & Decimal Precision.
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
    public Review addReview(User user, Long partId, Double rating, String comment) {
        // 1. Verify Purchaser
        if (!orderRepository.hasUserPurchasedPart(user.getId(), partId)) {
            throw new RuntimeException("Only verified purchasers can review this part.");
        }

        // 2. Prevent Duplicate Reviews (FIX: stops double posting)
        if (reviewRepository.existsByUserIdAndPartId(user.getId(), partId)) {
            throw new RuntimeException("You have already reviewed this part.");
        }

        Part part = partRepository.findById(partId)
                .orElseThrow(() -> new RuntimeException("Part not found"));

        // 3. Save the Review
        Review review = new Review();
        review.setUser(user);
        review.setPart(part);
        review.setRating(rating); // Now supports decimals like 4.5
        review.setComment(comment);

        Review savedReview = reviewRepository.save(review);

        // 4. Update Part Rating using Native SQL
        updatePartRatingNative(partId);

        return savedReview;
    }

    private void updatePartRatingNative(Long partId) {
        Double average = reviewRepository.getAverageRatingForPart(partId).orElse(0.0);
        BigDecimal bd = BigDecimal.valueOf(average).setScale(1, RoundingMode.HALF_UP);
        partRepository.updateAverageRatingNative(partId, bd.doubleValue());
    }
}