package com.example.controller;

import com.example.dto.CartDTO;
import com.example.dto.CartResponseDTO;
import com.example.dto.CartSummaryDTO;
import com.example.service.CartService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private static final Logger logger = LoggerFactory.getLogger(CartController.class);
    private final CartService cartService;

    @PostMapping("/add")
    public ResponseEntity<CartResponseDTO> addToCart(
            @RequestBody CartDTO cartDTO,
            @RequestHeader(value = "X-Session-ID", required = false) String sessionId) {
        
        if (cartDTO.getUserId() == null && sessionId == null) {
            throw new IllegalArgumentException("Either userId or sessionId must be provided");
        }
        
        logger.info("Adding item to cart for user ID: {} or session ID: {}", 
                  cartDTO.getUserId(), sessionId);
        CartResponseDTO response = cartService.addToCart(cartDTO, sessionId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<CartResponseDTO>> getCartByUserId(@PathVariable Long userId) {
        logger.info("Fetching cart items for user ID: {}", userId);
        List<CartResponseDTO> cartItems = cartService.getCartByUserId(userId);
        return ResponseEntity.ok(cartItems);
    }

    @GetMapping("/user/{userId}/summary")
    public ResponseEntity<CartSummaryDTO> getCartSummaryByUserId(@PathVariable Long userId) {
        logger.info("Fetching cart summary for user ID: {}", userId);
        CartSummaryDTO summary = cartService.getCartSummaryByUserId(userId);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/guest")
    public ResponseEntity<List<CartResponseDTO>> getGuestCart(
            @RequestHeader("X-Session-ID") String sessionId) {
        logger.info("Fetching cart items for session ID: {}", sessionId);
        List<CartResponseDTO> cartItems = cartService.getCartBySessionId(sessionId);
        return ResponseEntity.ok(cartItems);
    }

    @GetMapping("/guest/summary")
    public ResponseEntity<CartSummaryDTO> getGuestCartSummary(
            @RequestHeader("X-Session-ID") String sessionId) {
        logger.info("Fetching cart summary for session ID: {}", sessionId);
        CartSummaryDTO summary = cartService.getCartSummaryBySessionId(sessionId);
        return ResponseEntity.ok(summary);
    }

    @PutMapping("/{cartId}")
    public ResponseEntity<CartResponseDTO> updateCartItem(
            @PathVariable Long cartId,
            @RequestParam Long quantity,
            @RequestHeader(value = "X-Session-ID", required = false) String sessionId) {
        logger.info("Updating cart item ID: {} with new quantity: {}", cartId, quantity);
        CartResponseDTO response = cartService.updateCartItem(cartId, quantity, sessionId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{cartId}")
    public ResponseEntity<Void> removeFromCart(
            @PathVariable Long cartId,
            @RequestHeader(value = "X-Session-ID", required = false) String sessionId) {
        logger.info("Removing cart item ID: {}", cartId);
        cartService.removeFromCart(cartId, sessionId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/clear/user/{userId}")
    public ResponseEntity<Void> clearUserCart(@PathVariable Long userId) {
        logger.info("Clearing cart for user ID: {}", userId);
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/clear/guest")
    public ResponseEntity<Void> clearGuestCart(
            @RequestHeader("X-Session-ID") String sessionId) {
        logger.info("Clearing guest cart for session ID: {}", sessionId);
        cartService.clearGuestCart(sessionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/merge/{sessionId}/{userId}")
    public ResponseEntity<Void> mergeGuestCart(
            @PathVariable String sessionId,
            @PathVariable Long userId) {
        logger.info("Merging guest cart {} with user {}", sessionId, userId);
        cartService.mergeGuestCartWithUserCart(sessionId, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/session/new")
    public ResponseEntity<String> createNewSession() {
        String sessionId = cartService.generateSessionId();
        logger.info("Generated new session ID: {}", sessionId);
        return ResponseEntity.ok(sessionId);
    }
}