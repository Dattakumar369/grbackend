package com.example.controller;

import java.util.List;

import org.apache.coyote.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.dto.BulkProductUploadDTO;
import com.example.dto.ProductDTO;
import com.example.dto.ProductResponseDTO;
import com.example.service.CategoryService;
import com.example.service.ProductService;

import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller for managing product operations including creation, retrieval,
 * updating, and deletion of products.
 */
@Slf4j
@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;
    private final CategoryService categoryService;

    public ProductController(ProductService productService, CategoryService categoryService) {
        this.productService = productService;
        this.categoryService = categoryService;
    }

    /**
     * Creates a new product
     * @param productDTO DTO containing product details (including potential file uploads)
     * @return ResponseEntity with created product and HTTP status 201
     */
    @PostMapping("/add")
    public ResponseEntity<ProductResponseDTO> createProduct(@ModelAttribute ProductDTO productDTO) {
        log.info("Creating new product with name: {}", productDTO.getName());
        
        ProductResponseDTO createdProduct = productService.createProduct(productDTO);
        
        log.info("Successfully created product with ID: {}", createdProduct.getId());
        return new ResponseEntity<>(createdProduct, HttpStatus.CREATED);
    }

    @GetMapping("/subcategories")
    public ResponseEntity<List<String>> getAllSubCategories() {
        return ResponseEntity.ok(categoryService.getAllSubCategories());
    }

    @GetMapping("/by-subcategory/{subCategory}")
    public ResponseEntity<List<ProductResponseDTO>> getProductsBySubCategory(
            @PathVariable String subCategory) {
        return ResponseEntity.ok(productService.getProductsBySubCategory(subCategory));
    }

    /**
     * Retrieves a product by its ID
     * @param id ID of the product to retrieve
     * @return ResponseEntity with product data and HTTP status 200
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> getProductById(@PathVariable String id) {
        log.debug("Fetching product with ID: {}", id);
        
        ProductResponseDTO product = productService.getProductById(id);
        
        log.debug("Found product with ID: {} - Name: {}", id, product.getName());
        return ResponseEntity.ok(product);
    }

    /**
     * Retrieves all products
     * @return ResponseEntity with list of all products and HTTP status 200
     */
    @GetMapping("/all")
    public ResponseEntity<List<ProductResponseDTO>> getAllProducts() {
        log.debug("Fetching all products");
        
        List<ProductResponseDTO> products = productService.getAllProducts();
        
        log.debug("Retrieved {} products", products.size());
        return ResponseEntity.ok(products);
    }

    /**
     * Updates an existing product
     * @param id ID of the product to update
     * @param productDTO DTO containing updated product details
     * @return ResponseEntity with updated product and HTTP status 200
     * @throws BadRequestException 
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> updateProduct(
            @PathVariable String id,
            @ModelAttribute ProductDTO productDTO) throws BadRequestException {
        
        if (!productDTO.hasUpdates()) {
            throw new BadRequestException("No update fields provided");
        }
        
        ProductResponseDTO updatedProduct = productService.updateProduct(id, productDTO);
        return ResponseEntity.ok(updatedProduct);
    }

    /**
     * Deletes a product by its ID
     * @param id ID of the product to delete
     * @return ResponseEntity with no content and HTTP status 204
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable String id) {
        log.info("Deleting product with ID: {}", id);
        
        productService.deleteProduct(id);
        
        log.info("Successfully deleted product with ID: {}", id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves all products belonging to a specific category
     * @param categoryId ID of the category
     * @return ResponseEntity with list of products and HTTP status 200
     */
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ProductResponseDTO>> getProductsByCategory(
            @PathVariable Integer categoryId) {
        log.debug("Fetching products for category ID: {}", categoryId);
        
        List<ProductResponseDTO> products = productService.getProductsByCategory(categoryId);
        
        log.debug("Found {} products for category ID: {}", products.size(), categoryId);
        return ResponseEntity.ok(products);
    }

    /**
     * Bulk upload products from a zip file containing Excel and images
     * @param bulkUploadDTO DTO containing the zip file
     * @return ResponseEntity with list of created products and HTTP status 201
     */
    @PostMapping("/bulk-upload")
    public ResponseEntity<List<ProductResponseDTO>> bulkUploadProducts(
            @ModelAttribute BulkProductUploadDTO bulkUploadDTO) {
        log.info("Processing bulk product upload");
        List<ProductResponseDTO> createdProducts = productService.bulkCreateProducts(bulkUploadDTO.getZipFile());
        log.info("Successfully created {} products in bulk", createdProducts.size());
        return new ResponseEntity<>(createdProducts, HttpStatus.CREATED);
    }
    
    /**
     * Get product by ID with user-specific wishlist status
     * @param id Product ID
     * @param userId User ID (optional)
     * @return Product response with wishlist status
     */
    @GetMapping("/{id}/user/{userId}")
    public ResponseEntity<ProductResponseDTO> getProductByIdWithUserStatus(
            @PathVariable String id,
            @PathVariable Long userId) {
        log.debug("Fetching product with ID: {} for user ID: {}", id, userId);
        
        ProductResponseDTO product = productService.getProductById(id, userId);
        
        log.debug("Found product with ID: {} - Name: {}", id, product.getName());
        return ResponseEntity.ok(product);
    }
}