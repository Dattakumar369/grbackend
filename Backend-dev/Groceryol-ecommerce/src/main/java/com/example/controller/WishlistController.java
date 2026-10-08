package com.example.controller;

import com.example.dto.WishlistRequestDTO;
import com.example.dto.WishlistResponseDTO;
import com.example.service.WishlistService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/wishlist")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @PostMapping
    public ResponseEntity<WishlistResponseDTO> addToWishlist(@RequestBody WishlistRequestDTO requestDTO) {
        return ResponseEntity.ok(wishlistService.addToWishlist(requestDTO));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<WishlistResponseDTO>> getWishlistByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(wishlistService.getWishlistByUserId(userId));
    }

    @DeleteMapping("/user/{userId}/product/{productId}")
    public ResponseEntity<Void> removeFromWishlist(
            @PathVariable Long userId,
            @PathVariable String productId) {
        wishlistService.removeFromWishlist(userId, productId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/move-to-cart/user/{userId}/product/{productId}")
    public ResponseEntity<Void> moveToCart(
            @PathVariable Long userId,
            @PathVariable String productId,
            @RequestParam(defaultValue = "1") Long quantity) {
        wishlistService.moveToCart(userId, productId, quantity);
        return ResponseEntity.ok().build();
    }
}