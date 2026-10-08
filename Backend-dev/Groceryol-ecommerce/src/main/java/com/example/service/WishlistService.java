package com.example.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.dao.ProductRepository;
import com.example.dao.UserRepository;
import com.example.dao.WishlistRepository;
import com.example.dto.CartDTO;
import com.example.dto.WishlistRequestDTO;
import com.example.dto.WishlistResponseDTO;
import com.example.entity.Product;
import com.example.entity.User;
import com.example.entity.Wishlist;
import com.example.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CartService cartService; // For moving items to cart

    @Transactional
    public WishlistResponseDTO addToWishlist(WishlistRequestDTO requestDTO) {
        // Check if already in wishlist
        if (wishlistRepository.existsByUserIdAndProductId(requestDTO.getUserId(), requestDTO.getProductId())) {
            throw new IllegalArgumentException("Product already exists in wishlist");
        }

        User user = userRepository.findById(requestDTO.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        Product product = productRepository.findById(requestDTO.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        Wishlist wishlist = new Wishlist();
        wishlist.setUser(user);
        wishlist.setProduct(product);
        
        Wishlist savedWishlist = wishlistRepository.save(wishlist);
        return mapToResponseDTO(savedWishlist);
    }

    @Transactional(readOnly = true)
    public List<WishlistResponseDTO> getWishlistByUserId(Long userId) {
        return wishlistRepository.findByUserId(userId).stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void removeFromWishlist(Long userId, String productId) {
        wishlistRepository.deleteByUserIdAndProductId(userId, productId);
    }

@Transactional
public void moveToCart(Long userId, String productId, Long quantity) {
    // First check if product is in wishlist
    if (!wishlistRepository.existsByUserIdAndProductId(userId, productId)) {
        throw new ResourceNotFoundException("Product not found in wishlist");
    }
    
    // Add to cart
    CartDTO cartDTO = new CartDTO();
    cartDTO.setUserId(userId);
    cartDTO.setProductId(productId);
    cartDTO.setProductQuantity(quantity);
    
    // Since this is a logged-in user action, we can pass null or empty string for sessionId
    cartService.addToCart(cartDTO, ""); // or generate a session ID if needed
    
    // Remove from wishlist
    removeFromWishlist(userId, productId);
}
    private WishlistResponseDTO mapToResponseDTO(Wishlist wishlist) {
        WishlistResponseDTO responseDTO = new WishlistResponseDTO();
        responseDTO.setId(wishlist.getId());
        responseDTO.setUserId(wishlist.getUser().getId());
        responseDTO.setProductId(wishlist.getProduct().getId());
        responseDTO.setProductName(wishlist.getProduct().getName());
        responseDTO.setProductPrice(wishlist.getProduct().getPrice());
        
        // Set first available image
        if (wishlist.getProduct().getImage1() != null) {
            responseDTO.setProductImage(wishlist.getProduct().getImage1());
        } else if (wishlist.getProduct().getImage2() != null) {
            responseDTO.setProductImage(wishlist.getProduct().getImage2());
        } else if (wishlist.getProduct().getImage3() != null) {
            responseDTO.setProductImage(wishlist.getProduct().getImage3());
        }
        
        return responseDTO;
    }
}