package com.example.service;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.dao.ProductRepository;
import com.example.dao.ReviewRepository;
import com.example.dao.UserRepository;
import com.example.dto.ReviewDTO;
import com.example.dto.ReviewResponseDTO;
import com.example.entity.Product;
import com.example.entity.Review;
import com.example.entity.User;
import com.example.exception.ResourceNotFoundException;

@Service
public class ReviewService {

    private static final Logger logger = LoggerFactory.getLogger(ReviewService.class);
    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public ReviewService(ReviewRepository reviewRepository,
                        ProductRepository productRepository,
                        UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    public ReviewResponseDTO createReview(ReviewDTO reviewDTO) {
        logger.info("Creating new review for product ID: {} by user ID: {}", 
                   reviewDTO.getProductId(), reviewDTO.getUserId());

        Product product = productRepository.findById(reviewDTO.getProductId())
                .orElseThrow(() -> {
                    logger.error("Product not found with ID: {}", reviewDTO.getProductId());
                    return new ResourceNotFoundException(
                            "Product not found with id: " + reviewDTO.getProductId());
                });
        
        User user = userRepository.findById(reviewDTO.getUserId())
                .orElseThrow(() -> {
                    logger.error("User not found with ID: {}", reviewDTO.getUserId());
                    return new ResourceNotFoundException(
                            "User not found with id: " + reviewDTO.getUserId());
                });

        Review review = new Review();
        review.setProduct(product);
        review.setUser(user);
        review.setStar(reviewDTO.getStar());
        review.setReview(reviewDTO.getReview());

        Review savedReview = reviewRepository.save(review);
        logger.info("Created review with ID: {} (Rating: {})", 
                   savedReview.getId(), savedReview.getStar());
        
        return convertToResponseDTO(savedReview);
    }

    public ReviewResponseDTO getReviewResponseById(Integer id) {
        logger.debug("Fetching review with ID: {}", id);
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Review not found with ID: {}", id);
                    return new ResourceNotFoundException("Review not found with id: " + id);
                });
        return convertToResponseDTO(review);
    }

    public List<ReviewResponseDTO> getReviewResponsesByProduct(String productId) {
        logger.debug("Fetching reviews for product ID: {}", productId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    logger.error("Product not found with ID: {}", productId);
                    return new ResourceNotFoundException(
                            "Product not found with id: " + productId);
                });
        
        List<Review> reviews = reviewRepository.findByProduct(product);
        logger.info("Found {} reviews for product ID: {}", reviews.size(), productId);
        return reviews.stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    public ReviewResponseDTO updateReview(Integer id, ReviewDTO reviewDTO) {
        logger.info("Updating review with ID: {}", id);
        Review review = getReviewById(id);
        
        if (reviewDTO.getStar() != null) {
            logger.debug("Updating rating from {} to {}", review.getStar(), reviewDTO.getStar());
            review.setStar(reviewDTO.getStar());
        }
        
        if (reviewDTO.getReview() != null) {
            logger.debug("Updating review text");
            review.setReview(reviewDTO.getReview());
        }
        
        Review updatedReview = reviewRepository.save(review);
        logger.info("Review with ID: {} updated successfully", id);
        return convertToResponseDTO(updatedReview);
    }

    public void deleteReview(Integer id) {
        logger.info("Deleting review with ID: {}", id);
        Review review = getReviewById(id);
        reviewRepository.delete(review);
        logger.info("Review with ID: {} deleted successfully", id);
    }

    // Helper method to convert entity to DTO
    private ReviewResponseDTO convertToResponseDTO(Review review) {
        ReviewResponseDTO dto = new ReviewResponseDTO();
        dto.setId(review.getId());
        dto.setProductId(review.getProduct().getId());
        dto.setUserId(review.getUser().getId());
        dto.setStar(review.getStar());
        dto.setReview(review.getReview());
        return dto;
    }

    // Internal method to get the entity (not exposed to controller)
    private Review getReviewById(Integer id) {
        return reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + id));
    }
}