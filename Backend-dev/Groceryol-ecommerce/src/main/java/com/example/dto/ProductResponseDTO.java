package com.example.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class ProductResponseDTO {
    private String id;
    private String name;
    private String description;
    private BigDecimal price;
    private BigDecimal discountedPrice; // New field
    private Double discountPercentage; // New field
    private Integer categoryId;
    private String categoryName;
    private String subCategory;
    private Long quantity;
    private String quantityType;
    private String status;
    private LocalDate expiryDate;
    private List<String> imageUrls;
    private Boolean inWishlist;
}