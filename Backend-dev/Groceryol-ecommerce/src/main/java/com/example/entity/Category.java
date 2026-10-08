package com.example.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Entity
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "Category name is required")
    @Size(max = 50, message = "Category name must be less than 50 characters")
    private String name;

    @Size(max = 255, message = "Description must be less than 255 characters")
    private String description;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "category_sub_categories",
        joinColumns = @JoinColumn(name = "category_id")
    )
    @Column(name = "main_category")
    @NotEmpty(message = "At least one main category is required")
    private List<@NotBlank @Size(max = 50) String> subCategories = new ArrayList<>();

    // New field for subcategory discounts
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "subcategory_discounts",
        joinColumns = @JoinColumn(name = "category_id")
    )
    @MapKeyColumn(name = "subcategory")
    @Column(name = "discount_percentage")
    private Map<String, Double> subcategoryDiscounts = new HashMap<>();

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "^(ACTIVE|INACTIVE)$", message = "Status must be ACTIVE or INACTIVE")
    private String status;
}