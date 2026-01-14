package com.autohub.api.service;

import com.autohub.api.model.Part;
import com.autohub.api.model.Review;
import com.autohub.api.model.User;
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
    private final OrderLifecycleService lifecycleService;

    public ReviewService(ReviewRepository reviewRepository,
                         PartRepository partRepository,
                         OrderLifecycleService lifecycleService) {
        this.reviewRepository = reviewRepository;
        this.partRepository = partRepository;
        this.lifecycleService = lifecycleService;
    }

    @Transactional
    public Review addReview(User user, Long partId, Double rating, String comment) {

        lifecycleService.assertCanReview(user, partId);

        if (reviewRepository.existsByUserIdAndPartId(user.getId(), partId)) {
            throw new RuntimeException("You have already reviewed this part");
        }

        Part part = partRepository.findById(partId)
                .orElseThrow(() -> new RuntimeException("Part not found"));

        Review review = new Review();
        review.setUser(user);
        review.setPart(part);
        review.setRating(rating);
        review.setComment(comment);

        Review saved = reviewRepository.save(review);
        updatePartRating(partId);
        return saved;
    }

    private void updatePartRating(Long partId) {
        Double avg = reviewRepository.getAverageRatingForPart(partId)
                .orElse(0.0);

        BigDecimal rounded = BigDecimal.valueOf(avg)
                .setScale(1, RoundingMode.HALF_UP);

        partRepository.updateAverageRatingNative(
                partId, rounded.doubleValue()
        );
    }
}
