package com.example.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.dao.CartRepository;
import com.example.dao.DiscountRepository;
import com.example.dao.ProductRepository;
import com.example.dao.UserRepository;
import com.example.dto.CartDTO;
import com.example.dto.CartResponseDTO;
import com.example.dto.CartSummaryDTO;
import com.example.entity.Cart;
import com.example.entity.Discount;
import com.example.entity.Product;
import com.example.entity.User;
import com.example.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CartService {

    private static final Logger logger = LoggerFactory.getLogger(CartService.class);
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final DiscountRepository discountRepository;

    @Transactional
    public CartResponseDTO addToCart(CartDTO cartDTO, String sessionId) {
        logger.debug("Starting addToCart for sessionId: {}, userId: {}, productId: {}", 
                   sessionId, cartDTO.getUserId(), cartDTO.getProductId());

        User user = cartDTO.getUserId() != null ? 
            userRepository.findById(cartDTO.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + cartDTO.getUserId())) : null;
        
        Product product = productRepository.findById(cartDTO.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + cartDTO.getProductId()));

        validateQuantity(cartDTO.getProductQuantity(), product.getQuantity());

        Optional<Cart> existingCartItem = findExistingCartItem(user, sessionId, product.getId());

        if (existingCartItem.isPresent()) {
            return updateExistingCartItem(existingCartItem.get(), cartDTO.getProductQuantity(), product);
        } else {
            return createNewCartItem(user, sessionId, product, cartDTO.getProductQuantity());
        }
    }

    private void validateQuantity(long requestedQuantity, long availableQuantity) {
        if (requestedQuantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
        if (requestedQuantity > availableQuantity) {
            throw new IllegalArgumentException(
                String.format("Requested quantity (%d) exceeds available stock (%d)", 
                    requestedQuantity, availableQuantity));
        }
    }

    private Optional<Cart> findExistingCartItem(User user, String sessionId, String productId) {
        return user != null ? 
            cartRepository.findByUserIdAndProductId(user.getId(), productId) :
            cartRepository.findBySessionIdAndProductId(sessionId, productId);
    }

    private CartResponseDTO updateExistingCartItem(Cart existingItem, long additionalQuantity, Product product) {
        long newQuantity = existingItem.getProductQuantity() + additionalQuantity;
        
        if (newQuantity > product.getQuantity()) {
            throw new IllegalArgumentException(
                String.format("Cannot add %d items. You already have %d in cart, but only %d available in stock", 
                    additionalQuantity, existingItem.getProductQuantity(), product.getQuantity()));
        }
        
        existingItem.setProductQuantity(newQuantity);
        existingItem.setTotalPrice(calculateTotalPrice(existingItem));
        cartRepository.save(existingItem);
        
        logger.info("Updated existing cart item. CartId: {}, New Quantity: {}", 
                   existingItem.getId(), existingItem.getProductQuantity());
        return mapToResponseDTO(existingItem);
    }

    private CartResponseDTO createNewCartItem(User user, String sessionId, Product product, long quantity) {
        Cart cart = new Cart();
        if (user != null) {
            cart.setUser(user);
            cart.setNameOfuser(user.getFirstName());
        } else {
            cart.setSessionId(sessionId);
            cart.setNameOfuser("Guest");
        }
        cart.setProduct(product);
        cart.setProductQuantity(quantity);
        cart.setProductPrice(product.getPrice());
        cart.setTotalPrice(calculateTotalPrice(cart));
        
        Cart savedCart = cartRepository.save(cart);
        logger.info("Created new cart item. CartId: {}, ProductId: {}", 
                   savedCart.getId(), product.getId());
        return mapToResponseDTO(savedCart);
    }

    @Transactional
    public CartResponseDTO updateCartItem(Long cartId, Long quantity, String sessionId) {
        logger.debug("Updating cart item. CartId: {}, New Quantity: {}", cartId, quantity);
        
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + cartId));
        
        validateCartOwnership(cart, sessionId);
        validateQuantity(quantity, cart.getProduct().getQuantity());
        
        cart.setProductQuantity(quantity);
        cart.setTotalPrice(calculateTotalPrice(cart));
        Cart updatedCart = cartRepository.save(cart);
        
        logger.info("Cart item updated successfully. CartId: {}", cartId);
        return mapToResponseDTO(updatedCart);
    }

    private void validateCartOwnership(Cart cart, String sessionId) {
        if (cart.getUser() != null && sessionId != null) {
            throw new SecurityException("Invalid cart access attempt");
        }
        if (cart.getUser() == null && !cart.getSessionId().equals(sessionId)) {
            throw new SecurityException("Invalid session for guest cart");
        }
    }

    public List<CartResponseDTO> getCartByUserId(Long userId) {
        logger.debug("Fetching cart items for userId: {}", userId);
        List<Cart> cartItems = cartRepository.findByUserId(userId);
        logger.info("Found {} cart items for userId: {}", cartItems.size(), userId);
        return cartItems.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public List<CartResponseDTO> getCartBySessionId(String sessionId) {
        logger.debug("Fetching cart items for sessionId: {}", sessionId);
        List<Cart> cartItems = cartRepository.findBySessionId(sessionId);
        logger.info("Found {} cart items for sessionId: {}", cartItems.size(), sessionId);
        return cartItems.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public CartSummaryDTO getCartSummaryByUserId(Long userId) {
        List<CartResponseDTO> cartItems = getCartByUserId(userId);
        return calculateCartSummary(cartItems);
    }

    public CartSummaryDTO getCartSummaryBySessionId(String sessionId) {
        List<CartResponseDTO> cartItems = getCartBySessionId(sessionId);
        return calculateCartSummary(cartItems);
    }

private CartSummaryDTO calculateCartSummary(List<CartResponseDTO> cartItems) {
    CartSummaryDTO summary = new CartSummaryDTO();

    // Calculate subtotal
    BigDecimal subtotal = cartItems.stream()
        .map(CartResponseDTO::getSubtotal)
        .reduce(BigDecimal.ZERO, BigDecimal::add)
        .setScale(2, RoundingMode.HALF_UP);
    summary.setSubtotal(subtotal);

    // Calculate item discounts
    BigDecimal totalItemDiscount = cartItems.stream()
        .map(CartResponseDTO::getItemDiscount)
        .reduce(BigDecimal.ZERO, BigDecimal::add)
        .setScale(2, RoundingMode.HALF_UP);
    summary.setTotalItemDiscount(totalItemDiscount);

    // Get the SINGLE active cart discount
    Optional<Discount> activeDiscount = discountRepository.findActiveCartDiscount();
    
    BigDecimal cartDiscount = BigDecimal.ZERO;
    String cartDiscountMessage = null;
    
    if (activeDiscount.isPresent()) {
        Discount discount = activeDiscount.get();
        if (subtotal.compareTo(discount.getMinimumAmount()) >= 0) {
            cartDiscount = subtotal.multiply(discount.getPercentage())
                             .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            cartDiscountMessage = discount.getDescription();
        }
    }
    
    summary.setCartDiscount(cartDiscount);
    summary.setCartDiscountMessage(cartDiscountMessage);
    
    // Calculate grand total
    BigDecimal grandTotal = subtotal.subtract(totalItemDiscount).subtract(cartDiscount);
    summary.setGrandTotal(grandTotal.max(BigDecimal.ZERO)); // Ensure not negative
    
    summary.setItems(cartItems);
    
    return summary;
}
public void removeFromCart(Long cartId, String sessionId) {
        logger.debug("Attempting to remove cart item. CartId: {}", cartId);
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + cartId));
        
        validateCartOwnership(cart, sessionId);
        cartRepository.delete(cart);
        logger.info("Cart item removed successfully. CartId: {}", cartId);
    }

    @Transactional
    public void clearCart(Long userId) {
        logger.debug("Clearing cart for userId: {}", userId);
        cartRepository.deleteByUserId(userId);
        logger.info("Cart cleared for userId: {}", userId);
    }

    @Transactional
    public void clearGuestCart(String sessionId) {
        logger.debug("Clearing guest cart for sessionId: {}", sessionId);
        cartRepository.deleteBySessionId(sessionId);
        logger.info("Guest cart cleared for sessionId: {}", sessionId);
    }

    @Transactional
    public void mergeGuestCartWithUserCart(String sessionId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        List<Cart> guestCartItems = cartRepository.findBySessionId(sessionId);
        if (guestCartItems.isEmpty()) return;
        
        for (Cart guestItem : guestCartItems) {
            Optional<Cart> existingUserItem = cartRepository.findByUserIdAndProductId(
                userId, guestItem.getProduct().getId());
            
            if (existingUserItem.isPresent()) {
                Cart userItem = existingUserItem.get();
                long newQuantity = userItem.getProductQuantity() + guestItem.getProductQuantity();
                userItem.setProductQuantity(newQuantity);
                userItem.setTotalPrice(calculateTotalPrice(userItem));
                cartRepository.save(userItem);
                cartRepository.delete(guestItem);
            } else {
                guestItem.setUser(user);
                guestItem.setNameOfuser(user.getFirstName());
                guestItem.setSessionId(null);
                cartRepository.save(guestItem);
            }
        }
        logger.info("Merged {} guest cart items to user {}", guestCartItems.size(), userId);
    }

    public String generateSessionId() {
        return UUID.randomUUID().toString();
    }

    private BigDecimal calculateTotalPrice(Cart cart) {
        BigDecimal basePrice = cart.getProductPrice().multiply(BigDecimal.valueOf(cart.getProductQuantity()));
        BigDecimal discountedPrice = applySubcategoryDiscount(cart, basePrice);
        return discountedPrice;
    }

    private BigDecimal applySubcategoryDiscount(Cart cart, BigDecimal basePrice) {
        if (cart.getProduct() != null && cart.getProduct().getCategory() != null) {
            Double discountPercentage = cart.getProduct().getCategory()
                .getSubcategoryDiscounts().get(cart.getProduct().getSubCategory());
            
            if (discountPercentage != null && discountPercentage > 0) {
                return basePrice.subtract(
                    basePrice.multiply(BigDecimal.valueOf(discountPercentage / 100)));
            }
        }
        return basePrice;
    }

    private CartResponseDTO mapToResponseDTO(Cart cart) {
        CartResponseDTO responseDTO = new CartResponseDTO();
        responseDTO.setId(cart.getId());
        responseDTO.setUserName(cart.getNameOfuser());
        responseDTO.setProductId(cart.getProduct().getId());
        responseDTO.setProductName(cart.getProduct().getName());
        responseDTO.setProductQuantity(cart.getProductQuantity());
        responseDTO.setProductPrice(cart.getProductPrice().setScale(2, RoundingMode.HALF_UP));
        
        BigDecimal subtotal = cart.getProductPrice()
                           .multiply(BigDecimal.valueOf(cart.getProductQuantity()))
                           .setScale(2, RoundingMode.HALF_UP);
        responseDTO.setSubtotal(subtotal);
        
        // Apply subcategory discount
        BigDecimal itemDiscount = BigDecimal.ZERO;
        String itemDiscountMessage = null;
        
        if (cart.getProduct() != null && cart.getProduct().getCategory() != null) {
            Double discountPercentage = cart.getProduct().getCategory()
                .getSubcategoryDiscounts().get(cart.getProduct().getSubCategory());
            
            if (discountPercentage != null && discountPercentage > 0) {
                itemDiscount = subtotal.multiply(BigDecimal.valueOf(discountPercentage / 100))
                                  .setScale(2, RoundingMode.HALF_UP);
                itemDiscountMessage = String.format("%.0f%% subcategory discount", discountPercentage);
            }
        }
        
        responseDTO.setItemDiscount(itemDiscount);
        responseDTO.setDiscountedPrice(subtotal.subtract(itemDiscount));
        responseDTO.setItemDiscountMessage(itemDiscountMessage);
        
        return responseDTO;
    }
}