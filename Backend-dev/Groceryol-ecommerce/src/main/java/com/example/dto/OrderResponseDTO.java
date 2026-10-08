package com.example.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.example.status.OrderStatus;

import lombok.Data;

@Data
public class OrderResponseDTO {
    private Long id;
    private Long userId;
    private String userName;
    private BigDecimal totalAmount;
    private String shippingAddress;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long deliveryPersonId;
    private String deliveryPersonName;
    private List<OrderItemDTO> items;
    private PaymentDTO payment;
    private List<OrderHistoryDTO> history;
}