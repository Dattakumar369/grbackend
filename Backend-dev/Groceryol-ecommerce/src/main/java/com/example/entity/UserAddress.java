package com.example.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "user_addresses")
public class UserAddress {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Embedded
    private Address address;
    
    @Column(nullable = false)
    private Boolean isDefault = false;
    
    @Column(length = 50)
    private String addressLabel; // e.g., "Home", "Work", "Office"
    
    @Column(nullable = false)
    private Boolean active = true;
}