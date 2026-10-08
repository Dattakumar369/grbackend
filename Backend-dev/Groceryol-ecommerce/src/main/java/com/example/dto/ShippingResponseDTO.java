package com.example.dto;

import com.example.entity.OrderItem;
import com.example.status.OrderStatus;
import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class ShippingResponseDTO {
    private Long orderId;
    private OrderStatus orderStatus;
    private List<OrderItem> shippedItems;
    private List<OrderItem> pendingItems;
    private Map<String, List<OrderItem>> itemsByTrackingNumber;
    private int totalItems;
    private int shippedItemsCount;
    private int pendingItemsCount;
}