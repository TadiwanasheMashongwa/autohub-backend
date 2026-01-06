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
        // Debug Log: Check IDs in IntelliJ Console
        System.out.println("DEBUG: Verifying User ID: " + user.getId() + " for Part ID: " + partId);

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

        Review savedReview = reviewRepository.save(review);
        updatePartRating(part);

        return savedReview;
    }

    private void updatePartRating(Part part) {
        double average = part.getReviews().stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);

        BigDecimal bd = BigDecimal.valueOf(average).setScale(1, RoundingMode.HALF_UP);
        part.setAverageRating(bd.doubleValue());
        partRepository.save(part);
    }
}