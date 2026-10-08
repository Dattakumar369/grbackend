package com.example.controller;

import com.example.dto.ProductResponseDTO;
import com.example.dto.SearchResponseDTO;
import com.example.service.SearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public ResponseEntity<SearchResponseDTO> searchAll(
            @RequestParam(required = false) String query) {
        
        SearchResponseDTO response = searchService.searchAll(query);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/products")
    public ResponseEntity<List<ProductResponseDTO>> searchProducts(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) Integer minRating,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String subCategory) {
        
        List<ProductResponseDTO> products = searchService.searchProducts(
                query, minPrice, maxPrice, minRating, category, subCategory);
        return ResponseEntity.ok(products);
    }
}