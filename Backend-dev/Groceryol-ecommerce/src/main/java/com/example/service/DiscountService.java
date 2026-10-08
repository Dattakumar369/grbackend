package com.example.service;

import com.example.dao.DiscountRepository;
import com.example.entity.Discount;
import com.example.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DiscountService {

    private final DiscountRepository discountRepository;

    public List<Discount> getAllDiscounts() {
        return discountRepository.findAll();
    }

    public List<Discount> getActiveDiscounts() {
        LocalDateTime now = LocalDateTime.now();
        return discountRepository.findActiveDiscounts(now);
    }


    public List<Discount> getApplicableDiscounts() {
        LocalDateTime now = LocalDateTime.now();
        return discountRepository.findApplicableCartDiscounts(now);
    }

    public Discount createDiscount(Discount discount) {
        return discountRepository.save(discount);
    }

    @Transactional
    public Discount updateDiscount(Long id, Discount discount) {
        Discount existingDiscount = discountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Discount not found with id: " + id));
        
        existingDiscount.setName(discount.getName());
        existingDiscount.setDescription(discount.getDescription());
        existingDiscount.setPercentage(discount.getPercentage());
        existingDiscount.setMinimumAmount(discount.getMinimumAmount());
        existingDiscount.setActive(discount.isActive());
        existingDiscount.setStartDate(discount.getStartDate());
        existingDiscount.setEndDate(discount.getEndDate());
        
        return discountRepository.save(existingDiscount);
    }

    @Transactional
    public void deleteDiscount(Long id) {
        discountRepository.deleteById(id);
    }

    @Transactional
    public Discount activateDiscount(Long discountId) {
        // First deactivate all other discounts
        discountRepository.deactivateAllDiscounts();
        
        // Then activate the selected one
        Discount discount = discountRepository.findById(discountId)
            .orElseThrow(() -> new ResourceNotFoundException("Discount not found"));
        
        discount.setCurrentlyActive(true);
        return discountRepository.save(discount);
    }

    @Transactional
    public void deactivateDiscount(Long discountId) {
        Discount discount = discountRepository.findById(discountId)
            .orElseThrow(() -> new ResourceNotFoundException("Discount not found"));
        
        discount.setCurrentlyActive(false);
        discountRepository.save(discount);
    }
}