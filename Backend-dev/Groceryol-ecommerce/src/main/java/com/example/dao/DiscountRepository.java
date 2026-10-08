package com.example.dao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.entity.Discount;

public interface DiscountRepository extends JpaRepository<Discount, Long> {
    
    // Find the single currently active discount
    @Query("SELECT d FROM Discount d WHERE d.currentlyActive = true AND " +
           "d.active = true AND " +
           "(d.startDate IS NULL OR d.startDate <= :now) AND " +
           "(d.endDate IS NULL OR d.endDate >= :now)")
    Optional<Discount> findActiveCartDiscount(@Param("now") LocalDateTime now);
    
    default Optional<Discount> findActiveCartDiscount() {
        return findActiveCartDiscount(LocalDateTime.now());
    }
    
    // Find all active discounts (for admin view)
    @Query("SELECT d FROM Discount d WHERE " +
           "d.active = true AND " +
           "(d.startDate IS NULL OR d.startDate <= :now) AND " +
           "(d.endDate IS NULL OR d.endDate >= :now)")
    List<Discount> findActiveDiscounts(@Param("now") LocalDateTime now);
    
    // Find all applicable discounts (potential discounts that could be activated)
    @Query("SELECT d FROM Discount d WHERE " +
           "d.active = true AND " +
           "(d.startDate IS NULL OR d.startDate <= :now) AND " +
           "(d.endDate IS NULL OR d.endDate >= :now)")
    List<Discount> findApplicableCartDiscounts(@Param("now") LocalDateTime now);
    
    @Modifying
    @Query("UPDATE Discount d SET d.currentlyActive = false")
    void deactivateAllDiscounts();
}