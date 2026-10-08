package com.example.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.example.status.OrderStatus;
import lombok.Data;

@Data
public class OrderHistoryDTO {
    private Long id;
    private Long orderId;
    private OrderStatus status;
    private String notes;
    private LocalDateTime createdAt;
    
    // Include full order details
    private BigDecimal totalAmount;
    private AddressDTO shippingAddress;
    private List<OrderItemDTO> items;
    private UserOrderDto user;
    private DeliveryPersonOrderDto deliveryPerson;
    private PaymentDTO payment;
    
    // Delivery person specific fields (kept for backward compatibility)
    private Long deliveryPersonId;
    private String deliveryPersonName;
}