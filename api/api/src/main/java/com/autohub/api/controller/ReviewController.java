package com.autohub.api.controller;

import com.autohub.api.model.Review;
import com.autohub.api.model.User;
import com.autohub.api.repository.ReviewRepository;
import com.autohub.api.repository.UserRepository;
import com.autohub.api.service.ReviewService;
import org.springframework.http.ResponseEntity;
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

    @PostMapping("/add")
    public ResponseEntity<Review> addReview(@RequestBody Map<String, Object> payload, Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Long partId = Long.valueOf(payload.get("partId").toString());
        Integer rating = (Integer) payload.get("rating");
        String comment = (String) payload.get("comment");

        return ResponseEntity.ok(reviewService.addReview(user, partId, rating, comment));
    }

    // NEW: Needed for the Full Use Case (Customer browsing metrics)
    @GetMapping("/part/{partId}")
    public ResponseEntity<List<Review>> getReviewsByPart(@PathVariable Long partId) {
        return ResponseEntity.ok(reviewRepository.findByPartId(partId));
    }
}