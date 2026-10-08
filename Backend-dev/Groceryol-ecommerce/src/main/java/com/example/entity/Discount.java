package com.example.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
public class Discount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    private String description;
    private BigDecimal percentage;
    private BigDecimal minimumAmount;
    private boolean active;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    
    @Column(name = "is_currently_active")
    private boolean currentlyActive;
    }