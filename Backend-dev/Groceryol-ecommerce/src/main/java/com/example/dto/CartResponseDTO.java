package com.example.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CartResponseDTO {
    private Long id;
    private String userName;
    private String productId;
    private String productName;
    private Long productQuantity;
    private BigDecimal productPrice;
    private BigDecimal subtotal;         // quantity * price
    private BigDecimal itemDiscount;    // subcategory discount amount
    private BigDecimal discountedPrice; // subtotal - itemDiscount
    private String itemDiscountMessage; // e.g., "15% subcategory discount"
}