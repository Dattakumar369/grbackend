// ReviewResponseDTO.java
package com.example.dto;

import lombok.Data;

@Data
public class ReviewResponseDTO {
    private Integer id;
    private String productId;
    private Long userId;
    private Integer star;
    private String review;
}