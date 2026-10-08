 package com.example.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.entity.Cart;
public interface CartRepository extends JpaRepository<Cart, Long> {
    List<Cart> findByUserId(Long userId);
    List<Cart> findBySessionId(String sessionId);
    void deleteByUserId(Long userId);
    void deleteBySessionId(String sessionId);
    Optional<Cart> findByUserIdAndProductId(Long userId, String productId);
    Optional<Cart> findBySessionIdAndProductId(String sessionId, String productId);
    void deleteByProductId(String productId);

}