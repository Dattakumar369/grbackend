package com.example.dao;

import com.example.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    // Find all wishlist items for a specific user
    List<Wishlist> findByUserId(Long userId);

    // Check if a product exists in a user's wishlist
    boolean existsByUserIdAndProductId(Long userId, String productId);

    // Find a specific wishlist item by user and product
    Optional<Wishlist> findByUserIdAndProductId(Long userId, String productId);

    // Delete a specific wishlist item by user and product
    void deleteByUserIdAndProductId(Long userId, String productId);

    // Count how many items are in a user's wishlist
    long countByUserId(Long userId);

    // Find wishlist items with product details (eager loading)
    @Query("SELECT w FROM Wishlist w JOIN FETCH w.product WHERE w.user.id = :userId")
    List<Wishlist> findByUserIdWithProducts(@Param("userId") Long userId);

    // Check if multiple products are in a user's wishlist (for batch operations)
    @Query("SELECT w.product.id FROM Wishlist w WHERE w.user.id = :userId AND w.product.id IN :productIds")
    List<Long> findWishlistedProductIds(@Param("userId") Long userId, @Param("productIds") List<String> productIds);

    // Delete all wishlist items for a user (clear wishlist)
    void deleteAllByUserId(Long userId);

    void deleteByProductId(String productId);
}