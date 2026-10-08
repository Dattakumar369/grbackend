package com.example.dto;

import com.example.dto.ProductResponseDTO;
import com.example.entity.Product;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ProductMapper {
    
    public ProductResponseDTO mapToResponseDto(Product product) {
        if (product == null) {
            return null;
        }
        
        ProductResponseDTO dto = new ProductResponseDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setExpiryDate(product.getExpiryDate()); // New mapping
        
        if (product.getCategory() != null) {
            dto.setCategoryId(product.getCategory().getId());
            dto.setCategoryName(product.getCategory().getName());
        }
        
        dto.setSubCategory(product.getSubCategory());
        dto.setQuantity(product.getQuantity());
        dto.setQuantityType(product.getQuantityType());
        dto.setStatus(product.getStatus());
        
        List<String> imageUrls = new ArrayList<>();
        if (product.getImage1() != null) imageUrls.add(product.getImage1());
        if (product.getImage2() != null) imageUrls.add(product.getImage2());
        if (product.getImage3() != null) imageUrls.add(product.getImage3());
        if (product.getImage4() != null) imageUrls.add(product.getImage4());
        if (product.getImage5() != null) imageUrls.add(product.getImage5());
        dto.setImageUrls(imageUrls);
        
        return dto;
    }
}