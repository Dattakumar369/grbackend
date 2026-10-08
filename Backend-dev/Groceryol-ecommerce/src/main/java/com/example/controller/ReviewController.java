package com.example.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.dto.ReviewDTO;
import com.example.dto.ReviewResponseDTO;
import com.example.service.ReviewService;

@RestController
@RequestMapping("/reviews")
public class ReviewController {

    private static final Logger logger = LoggerFactory.getLogger(ReviewController.class);
    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/add")
    public ResponseEntity<ReviewResponseDTO> createReview(@RequestBody ReviewDTO reviewDTO) {
        logger.info("POST /reviews/add - Creating new review for product: {}, user: {}", 
                  reviewDTO.getProductId(), reviewDTO.getUserId());
        
        ReviewResponseDTO response = reviewService.createReview(reviewDTO);
        
        logger.debug("Review created successfully with ID: {}", response.getId());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReviewResponseDTO> getReviewById(@PathVariable Integer id) {
        logger.debug("GET /reviews/{} - Fetching review by ID", id);
        
        ReviewResponseDTO response = reviewService.getReviewResponseById(id);
        
        logger.info("Retrieved review with ID: {}", id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ReviewResponseDTO>> getReviewsByProduct(@PathVariable String productId) {
        logger.info("GET /reviews/product/{} - Fetching reviews for product", productId);
        
        List<ReviewResponseDTO> responses = reviewService.getReviewResponsesByProduct(productId);
        
        logger.debug("Found {} reviews for product ID: {}", responses.size(), productId);
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReviewResponseDTO> updateReview(
            @PathVariable Integer id,
            @RequestBody ReviewDTO reviewDTO) {
        logger.info("PUT /reviews/{} - Updating review", id);
        
        ReviewResponseDTO response = reviewService.updateReview(id, reviewDTO);
        
        logger.debug("Review with ID: {} updated successfully", id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable Integer id) {
        logger.info("DELETE /reviews/{} - Deleting review", id);
        
        reviewService.deleteReview(id);
        
        logger.debug("Review with ID: {} deleted successfully", id);
        return ResponseEntity.noContent().build();
    }
}