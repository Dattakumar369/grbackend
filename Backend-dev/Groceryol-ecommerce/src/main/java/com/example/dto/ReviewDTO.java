 package com.example.dto;

import lombok.Data;

@Data
public class ReviewDTO {
    private String productId;
    private Long userId;
    private Integer star;
    private String review;
}