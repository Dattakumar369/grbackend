 package com.example.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.entity.Category;
import com.example.entity.Product;

public interface ProductRepository extends JpaRepository<Product, String> {
    List<Product> findByCategory(Category category);
    Optional<Product> findByName(String name);
List<Product> findBySubCategory(String subCategory);
    
    @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId AND p.subCategory = :subCategory")
    List<Product> findByCategoryAndSubCategory(
            @Param("categoryId") Integer categoryId,
            @Param("subCategory") String subCategory);

    @Query("SELECT p FROM Product p WHERE " +
    	       "(:query IS NULL OR " +
    	       "LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
    	       "LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
    	       "LOWER(p.category.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
    	       "LOWER(p.subCategory) LIKE LOWER(CONCAT('%', :query, '%')))")
    	List<Product> searchProducts(@Param("query") String query);

    	@Query("SELECT p FROM Product p WHERE " +
    	       "(:query IS NULL OR " +
    	       "LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
    	       "LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
    	       "LOWER(p.category.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
    	       "LOWER(p.subCategory) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
    	       "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
    	       "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
    	       "(:minRating IS NULL OR " +
    	       "(SELECT AVG(r.star) FROM Review r WHERE r.product = p) >= :minRating) AND " +
    	       "(:category IS NULL OR LOWER(p.category.name) = LOWER(:category)) AND " +
    	       "(:subCategory IS NULL OR LOWER(p.subCategory) = LOWER(:subCategory))")
    	List<Product> searchProductsWithFilters(
    	        @Param("query") String query,
    	        @Param("minPrice") Double minPrice,
    	        @Param("maxPrice") Double maxPrice,
    	        @Param("minRating") Integer minRating,
    	        @Param("category") String category,
    	        @Param("subCategory") String subCategory);
    	
    	
    	
    	@Query("SELECT MAX(p.id) FROM Product p WHERE p.id LIKE :prefix")
    	Optional<String> findMaxIdByPrefix(@Param("prefix") String prefix);
}