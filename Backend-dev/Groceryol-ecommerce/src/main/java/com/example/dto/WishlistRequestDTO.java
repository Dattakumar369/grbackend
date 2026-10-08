package com.example.dto;

import lombok.Data;

@Data
public class WishlistRequestDTO {
    private Long userId;
    private String productId;
}

