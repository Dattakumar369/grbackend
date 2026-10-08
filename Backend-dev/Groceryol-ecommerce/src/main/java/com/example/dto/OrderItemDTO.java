package com.example.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderItemDTO {
    private String productId;
    private String productName;
    private BigDecimal price;
    private Long quantity;
    private BigDecimal totalPrice;
    private String image1;
    private String image2;
    private String image3;
    private boolean shipped;

}