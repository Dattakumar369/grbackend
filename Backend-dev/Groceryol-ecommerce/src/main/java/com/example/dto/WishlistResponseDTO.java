package com.example.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
 
public class WishlistResponseDTO {
    private Long id;
    private Long userId;
    private String productId;
    private String productName;
    private String productDescription;
    private BigDecimal productPrice;
    private String categoryName;
    private String productImage;
    private boolean inStock; // Derived from product quantity
}