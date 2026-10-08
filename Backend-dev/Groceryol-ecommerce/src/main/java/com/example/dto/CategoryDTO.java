package com.example.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class CategoryDTO {
    private String name;
    private String description;
    private List<String> subCategories;
    private Map<String, Double> subcategoryDiscounts; // New field
    private String status;
}