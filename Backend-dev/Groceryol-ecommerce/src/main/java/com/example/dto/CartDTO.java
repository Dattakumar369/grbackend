 package com.example.dto;

import lombok.Data;

@Data
public class CartDTO {
    private Long userId;
    private String productId;
    private Long productQuantity;
}