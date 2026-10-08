package com.example.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;
@Entity
@Data
public class Cart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String nameOfuser;
    private Long productQuantity;
    private BigDecimal productPrice;
    private BigDecimal totalPrice;
    private BigDecimal subtotal;       // Price before discounts
    private BigDecimal discountAmount; // Total discount applied
     
    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    
    @Column
    private String sessionId; // For guest carts
    
    private LocalDateTime createdAt = LocalDateTime.now();
}