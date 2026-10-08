package com.example.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.dao.CategoryRepository;
import com.example.dao.ProductRepository;
import com.example.dto.ProductMapper;
import com.example.dto.ProductResponseDTO;
import com.example.dto.SearchResponseDTO;
import com.example.entity.Category;
import com.example.entity.Product;

@Service
public class SearchService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;  // Use ProductMapper instead of ProductService

    public SearchService(ProductRepository productRepository, 
                        CategoryRepository categoryRepository,
                        ProductMapper productMapper) {  // Inject ProductMapper
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productMapper = productMapper;
    }

    public SearchResponseDTO searchAll(String query) {
        // Search products
        List<Product> products = productRepository.searchProducts(
                query != null ? query.toLowerCase() : null);
        
        // Search categories
        List<Category> categories = categoryRepository.searchCategories(
                query != null ? query.toLowerCase() : null);
        
        return new SearchResponseDTO(
                products.stream()
                        .map(productMapper::mapToResponseDto)  // Use the mapper
                        .collect(Collectors.toList()),
                categories
        );
    }

    public List<ProductResponseDTO> searchProducts(
            String query, 
            Double minPrice, 
            Double maxPrice,
            Integer minRating,
            String category,
            String subCategory) {
        
        List<Product> products = productRepository.searchProductsWithFilters(
                query != null ? query.toLowerCase() : null,
                minPrice,
                maxPrice,
                minRating,
                category,
                subCategory);
        
        return products.stream()
                .map(productMapper::mapToResponseDto)  // Use the mapper
                .collect(Collectors.toList());
    }
}