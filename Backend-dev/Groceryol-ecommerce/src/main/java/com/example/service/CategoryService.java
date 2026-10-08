package com.example.service;

 
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.dao.CategoryRepository;
import com.example.dao.ProductRepository;
import com.example.dto.CategoryDTO;
import com.example.entity.Category;
import com.example.entity.Product;
import com.example.exception.ResourceNotFoundException;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private static final Logger logger = LoggerFactory.getLogger(CategoryService.class);


    @Transactional
    public Category createCategory(CategoryDTO categoryDTO) {
        Category category = new Category();
        mapDtoToEntity(categoryDTO, category);
        return categoryRepository.save(category);
    }

    public Category getCategoryById(Integer id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

@Transactional
public Category updateCategory(Integer id, CategoryDTO categoryDTO) {
    Category existingCategory = getCategoryById(id);

    // Update basic fields
    if (categoryDTO.getName() != null) {
        existingCategory.setName(categoryDTO.getName());
    }
    if (categoryDTO.getDescription() != null) {
        existingCategory.setDescription(categoryDTO.getDescription());
    }
    if (categoryDTO.getStatus() != null) {
        existingCategory.setStatus(categoryDTO.getStatus());
    }

    // Handle subcategory updates
    if (categoryDTO.getSubCategories() != null) {
        handleSubcategoryUpdates(existingCategory, categoryDTO.getSubCategories());
    }

    // Handle discounts
    if (categoryDTO.getSubcategoryDiscounts() != null) {
        validateDiscounts(categoryDTO.getSubcategoryDiscounts());
        existingCategory.setSubcategoryDiscounts(new HashMap<>(categoryDTO.getSubcategoryDiscounts()));
    }

    return categoryRepository.save(existingCategory);
}

private void handleSubcategoryUpdates(Category existingCategory, List<String> newSubcategories) {
    List<String> currentSubcategories = existingCategory.getSubCategories();
    Map<String, String> renameMapping = detectRenamedSubcategories(currentSubcategories, newSubcategories);

    // Update products with renamed subcategories
    renameMapping.forEach((oldName, newName) -> {
        List<Product> products = productRepository.findByCategoryAndSubCategory(
            existingCategory.getId(), oldName);
        if (!products.isEmpty()) {
            products.forEach(p -> p.setSubCategory(newName));
            productRepository.saveAll(products);
            logger.info("Updated {} products from subcategory '{}' to '{}'", 
                products.size(), oldName, newName);
        }
    });

    // Set the new subcategories list
    existingCategory.setSubCategories(newSubcategories);
}

private Map<String, String> detectRenamedSubcategories(
        List<String> currentSubcategories, 
        List<String> newSubcategories) {
    Map<String, String> renameMapping = new HashMap<>();

    // Simple heuristic: if one subcategory is removed and one added, 
    // consider it a rename (for single subcategory case)
    if (currentSubcategories.size() == 1 && newSubcategories.size() == 1 &&
        !currentSubcategories.equals(newSubcategories)) {
        renameMapping.put(currentSubcategories.get(0), newSubcategories.get(0));
    }
    // For multiple subcategories, you'd need a more sophisticated approach
    // like tracking changes or requiring explicit rename mapping

    return renameMapping;
}
    @Transactional
    public void removeSubcategoryDiscount(Integer id, String subcategory) {
        Category category = getCategoryById(id);
        if (!category.getSubCategories().contains(subcategory)) {
            throw new IllegalArgumentException("Subcategory " + subcategory + " does not exist in this category");
        }
        category.getSubcategoryDiscounts().remove(subcategory);
        categoryRepository.save(category);
    }

    public Map<String, Double> getSubcategoryDiscounts(Integer id) {
        return getCategoryById(id).getSubcategoryDiscounts();
    }

    public List<String> getAllSubCategories() {
        return categoryRepository.findAll().stream()
                .flatMap(category -> category.getSubCategories().stream())
                .distinct()
                .collect(Collectors.toList());
    }
    @Transactional
    public void deleteCategory(Integer id) {
        Category category = getCategoryById(id);
        if (!productRepository.findByCategory(category).isEmpty()) {
            throw new IllegalStateException("Cannot delete category with associated products");
        }
        categoryRepository.delete(category);
    }

    private void mapDtoToEntity(CategoryDTO dto, Category entity) {
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setStatus(dto.getStatus());
        entity.setSubCategories(dto.getSubCategories());
        
        if (dto.getSubcategoryDiscounts() != null) {
            validateDiscounts(dto.getSubcategoryDiscounts());
            entity.setSubcategoryDiscounts(new HashMap<>(dto.getSubcategoryDiscounts()));
        } else {
            entity.setSubcategoryDiscounts(new HashMap<>());
        }
    }

    private void validateDiscounts(Map<String, Double> discounts) {
        discounts.forEach((subcategory, discount) -> {
            if (discount < 0 || discount > 100) {
                throw new IllegalArgumentException(
                    "Discount percentage must be between 0 and 100 for subcategory: " + subcategory);
            }
        });
    }

    private void validateSubcategoryRemoval(Category existingCategory, List<String> newSubcategories) {
        existingCategory.getSubCategories().stream()
            .filter(sc -> !newSubcategories.contains(sc))
            .forEach(subcategory -> {
                if (!productRepository.findByCategoryAndSubCategory(
                        existingCategory.getId(), subcategory).isEmpty()) {
                    throw new IllegalStateException(
                        "Cannot remove subcategory '" + subcategory + "' as it has associated products");
                }
            });
    }
}