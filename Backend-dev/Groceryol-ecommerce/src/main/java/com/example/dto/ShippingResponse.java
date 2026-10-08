package com.example.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShippingResponse {
    private String message;
    private Long orderId;
    private String trackingNumber;
    private int shippedItemsCount;
    private int totalItems;
    private boolean allItemsShipped;
}