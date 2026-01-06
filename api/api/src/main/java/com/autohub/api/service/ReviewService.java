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
        // 1. Verify Purchaser (Confirmed working via logs)
        if (!orderRepository.hasUserPurchasedPart(user.getId(), partId)) {
            throw new RuntimeException("Only verified purchasers can review this part.");
        }

        Part part = partRepository.findById(partId)
                .orElseThrow(() -> new RuntimeException("Part not found"));

        Review review = new Review();
        review.setUser(user);
        review.setRating(rating);
        review.setComment(comment);

        // 2. Link bidirectionally and calculate rating
        part.addReview(review);

        double average = part.getReviews().stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);

        // 3. Precision rounding to satisfy PostgreSQL numeric constraints
        BigDecimal bd = BigDecimal.valueOf(average).setScale(1, RoundingMode.HALF_UP);
        part.setAverageRating(bd.doubleValue());

        // 4. Atomic Save (Review is saved automatically via CascadeType.ALL on Part)
        partRepository.save(part);

        return review;
    }
}