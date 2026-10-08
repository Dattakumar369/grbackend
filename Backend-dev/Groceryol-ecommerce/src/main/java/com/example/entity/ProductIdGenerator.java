package com.example.entity;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.jdbc.Work;
import com.example.entity.Product;
import com.example.entity.Category;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProductIdGenerator implements IdentifierGenerator {

    private static final Logger logger = LoggerFactory.getLogger(ProductIdGenerator.class);
    private static final String CREATE_TABLE_SQL = 
        "CREATE TABLE IF NOT EXISTS product_sequence (" +
        "prefix VARCHAR(4) PRIMARY KEY, " +
        "next_val BIGINT NOT NULL)";

    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object object) {
        Product product = (Product) object;
        Category category = product.getCategory();
        String subCategory = product.getSubCategory();
        
        // Validate category and subcategory
        if (category == null || category.getName() == null || subCategory == null) {
            throw new IllegalStateException("Category and subcategory must be set before ID generation");
        }
        
        String prefix = generatePrefix(category.getName(), subCategory);
        
        // Use Hibernate's doWork to get a connection
        SequenceHolder holder = new SequenceHolder();
        session.doWork(connection -> {
            ensureTableExists(connection);
            holder.sequence = getNextSequence(connection, prefix);
        });
        
        // Format with leading zeros (e.g., 0001)
        return String.format("%s%04d", prefix, holder.sequence);
    }
    
    private void ensureTableExists(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(CREATE_TABLE_SQL);
            logger.debug("Ensured product_sequence table exists");
        } catch (SQLException e) {
            logger.error("Failed to create product_sequence table", e);
            throw e;
        }
    }
    
    private long getNextSequence(Connection connection, String prefix) throws SQLException {
        // First try to get existing sequence
        try {
            return tryGetExistingSequence(connection, prefix);
        } catch (SQLException e) {
            if (isTableMissingError(e)) {
                // Table might have been created after initial check, try one more time
                try {
                    return tryGetExistingSequence(connection, prefix);
                } catch (SQLException e2) {
                    logger.error("Failed to get sequence after table creation", e2);
                    throw e2;
                }
            }
            throw e;
        }
    }
    
    private long tryGetExistingSequence(Connection connection, String prefix) throws SQLException {
        // Try to get the next sequence value
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT next_val FROM product_sequence WHERE prefix = ? FOR UPDATE")) {
            ps.setString(1, prefix);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long nextVal = rs.getLong(1);
                    
                    // Update the sequence
                    try (PreparedStatement updatePs = connection.prepareStatement(
                            "UPDATE product_sequence SET next_val = ? WHERE prefix = ?")) {
                        updatePs.setLong(1, nextVal + 1);
                        updatePs.setString(2, prefix);
                        updatePs.executeUpdate();
                    }
                    return nextVal;
                }
            }
        }
        
        // If no sequence exists, create one
        try (PreparedStatement insertPs = connection.prepareStatement(
                "INSERT INTO product_sequence (prefix, next_val) VALUES (?, ?)")) {
            insertPs.setString(1, prefix);
            insertPs.setLong(2, 2); // Start with 2 because we'll return 1 for the first product
            insertPs.executeUpdate();
        }
        
        return 1;
    }
    
    private boolean isTableMissingError(SQLException e) {
        // Check for common "table doesn't exist" error messages across different databases
        String message = e.getMessage().toLowerCase();
        return message.contains("doesn't exist") || 
               message.contains("does not exist") ||
               message.contains("exist") ||
               message.contains("no such table");
    }
    
    private String generatePrefix(String categoryName, String subCategory) {
        // Take first 2 letters of category and first 2 letters of subcategory
        String catPrefix = categoryName.substring(0, Math.min(2, categoryName.length())).toUpperCase();
        String subPrefix = subCategory.substring(0, Math.min(2, subCategory.length())).toUpperCase();
        return catPrefix + subPrefix;
    }
    
    // Helper class to hold the sequence value
    private static class SequenceHolder {
        long sequence = 1;
    }
}