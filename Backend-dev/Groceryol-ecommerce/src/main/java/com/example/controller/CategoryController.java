package com.example.controller;

import com.example.dto.CategoryDTO;
import com.example.dto.FilteredCategoryResponse;
import com.example.entity.Category;
import com.example.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping("/add")
    public ResponseEntity<Category> createCategory(@RequestBody CategoryDTO categoryDTO) {
        Category createdCategory = categoryService.createCategory(categoryDTO);
        return new ResponseEntity<>(createdCategory, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Category> getCategoryById(@PathVariable Integer id) {
        Category category = categoryService.getCategoryById(id);
        return ResponseEntity.ok(category);
    }

    @GetMapping("/all")
    public ResponseEntity<List<Category>> getAllCategories() {
        List<Category> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(categories);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Category> updateCategory(
            @PathVariable Integer id,
            @RequestBody CategoryDTO categoryDTO) {
        Category updatedCategory = categoryService.updateCategory(id, categoryDTO);
        return ResponseEntity.ok(updatedCategory);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Integer id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    // Additional endpoints for specific operations
    @GetMapping("/{id}/discounts")
    public ResponseEntity<Map<String, Double>> getSubcategoryDiscounts(
            @PathVariable Integer id) {
        Map<String, Double> discounts = categoryService.getSubcategoryDiscounts(id);
        return ResponseEntity.ok(discounts);
    }

    @DeleteMapping("/{id}/discounts/{subcategory}")
    public ResponseEntity<Void> removeSubcategoryDiscount(
            @PathVariable Integer id,
            @PathVariable String subcategory) {
        categoryService.removeSubcategoryDiscount(id, subcategory);
        return ResponseEntity.noContent().build();
    }
}