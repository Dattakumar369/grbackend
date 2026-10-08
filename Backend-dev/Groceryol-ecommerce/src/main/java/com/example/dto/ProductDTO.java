package com.example.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class ProductDTO {
    private String name;
    private String description;
    private BigDecimal price;
    private Integer categoryId;
    private String subCategory;
    private Long quantity;
    private String quantityType;
    private String status;
    private List<MultipartFile> images;
    private LocalDate expiryDate; // New field
    
    public boolean hasUpdates() {
        return name != null || description != null || price != null ||
               categoryId != null || subCategory != null || quantity != null || 
               quantityType != null || status != null || (images != null && !images.isEmpty()) ||
               expiryDate != null;
    }
}