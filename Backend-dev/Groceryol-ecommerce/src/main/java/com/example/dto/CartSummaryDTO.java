package com.example.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class CartSummaryDTO {
    private BigDecimal subtotal;          // sum of all items' subtotals
    private BigDecimal totalItemDiscount; // sum of all items' discounts
    private BigDecimal cartDiscount;      // 10% of subtotal if >= $100
    private BigDecimal grandTotal;       // subtotal - totalItemDiscount - cartDiscount
    private String cartDiscountMessage;  // e.g., "10% cart discount"
    private List<CartResponseDTO> items;
}