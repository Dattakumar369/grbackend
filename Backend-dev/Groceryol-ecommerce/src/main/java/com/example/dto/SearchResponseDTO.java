package com.example.dto;

import com.example.entity.Category;
import lombok.Data;

import java.util.List;

@Data
public class SearchResponseDTO {
    private List<ProductResponseDTO> products;
    private List<Category> categories;

    public SearchResponseDTO(List<ProductResponseDTO> products, 
                           List<Category> categories) {
        this.products = products;
        this.categories = categories;
    }
}