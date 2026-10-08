 package com.example.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
	@Query("SELECT DISTINCT c FROM Category c JOIN c.subCategories mc " +
		       "WHERE LOWER(mc) = LOWER(:subCategory)")
		List<Category> findBySubCategoryIgnoreCase(@Param("subCategory") String subCategory);

	List<Category> findBySubCategoriesContainingIgnoreCase(String subCategory);
	
	
	 @Query("SELECT c FROM Category c WHERE " +
	           "(:query IS NULL OR " +
	           "LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
	           "LOWER(c.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
	           "EXISTS (SELECT 1 FROM c.subCategories sc WHERE LOWER(sc) LIKE LOWER(CONCAT('%', :query, '%'))))")
	    List<Category> searchCategories(@Param("query") String query);
	
}