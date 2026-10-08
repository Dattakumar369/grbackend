package com.example.dto;

import lombok.Data;

@Data
public class FilteredCategoryResponse {
    private Integer id;
    private String name;
    private String description;
    private String matchingSubCategory;
    private String status;
}