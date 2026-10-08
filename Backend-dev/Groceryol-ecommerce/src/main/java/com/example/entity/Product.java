package com.example.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

@Data
@Entity
public class Product {

    @Id
    @GeneratedValue(generator = "custom-product-id")
    @GenericGenerator(
        name = "custom-product-id",
        strategy = "com.example.entity.ProductIdGenerator"
    )
    @Column(name = "id", unique = true, nullable = false, length = 20)
    private String id;

    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 100, message = "Product name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Description is required")
    @Size(min = 10, max = 1000, message = "Description must be between 10 and 1000 characters")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal price;

    @NotNull(message = "Category is required")
    @ManyToOne 
    @JoinColumn(name = "category_id")
    private Category category;

    @NotBlank(message = "Subcategory is required")
    @Size(max = 50, message = "Subcategory must be less than 50 characters")
    private String subCategory;

    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity cannot be negative")
    private Long quantity;

    @NotBlank(message = "Quantity type is required")
    @Size(max = 12, message = "Quantity type must be at most 12 characters")
    private String quantityType;

    @NotBlank(message = "Status is required")
    @Size(max = 20, message = "Status must be at most 20 characters")
    private String status;

    @Size(max = 255, message = "Image URL must be at most 255 characters")
    private String image1;

    @Size(max = 255, message = "Image URL must be at most 255 characters")
    private String image2;

    @Size(max = 255, message = "Image URL must be at most 255 characters")
    private String image3;
    
    @Size(max = 255, message = "Image URL must be at most 255 characters")
    private String image4;

    @Size(max = 255, message = "Image URL must be at most 255 characters")
    private String image5;

    private LocalDate expiryDate; // New optional field
}