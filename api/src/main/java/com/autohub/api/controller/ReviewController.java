package com.autohub.api.controller;

import com.autohub.api.model.Review;
import com.autohub.api.model.User;
import com.autohub.api.repository.ReviewRepository;
import com.autohub.api.repository.UserRepository;
import com.autohub.api.service.ReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    public ReviewController(ReviewService reviewService, ReviewRepository reviewRepository, UserRepository userRepository) {
        this.reviewService = reviewService;
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
    }

    /**
     * AUDIT #9.1: Add a product review.
     * Delegates to ReviewService to enforce the "Verified Purchase" rule
     * and automatically update the Part's average rating.
     */
    @PostMapping("/add")
    @PreAuthorize("hasRole('CUSTOMER')") // SECURED: Admins should not review products
    public ResponseEntity<Review> addReview(@RequestBody Map<String, Object> payload, Authentication authentication) {
        User user = getUserFromAuth(authentication);

        Long partId = Long.valueOf(payload.get("partId").toString());
        Double rating = Double.valueOf(payload.get("rating").toString());
        String comment = (String) payload.get("comment");

        return ResponseEntity.ok(reviewService.addReview(user, partId, rating, comment));
    }

    /**
     * AUDIT #9.2: Browse reviews for a specific part.
     */
    @GetMapping("/part/{partId}")
    public ResponseEntity<List<Review>> getReviewsByPart(@PathVariable Long partId) {
        return ResponseEntity.ok(reviewRepository.findByPartId(partId));
    }

    private User getUserFromAuth(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found with email: " + authentication.getName()));
    }
}