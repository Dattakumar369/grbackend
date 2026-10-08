package com.example.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.example.status.OrderStatus;

import lombok.Data;
@Data
public class OrderDTO {
 private Long id;
 private BigDecimal totalAmount;
 private OrderStatus status;
 private AddressDTO shippingAddress;
 private LocalDateTime createdAt;
 private List<OrderItemDTO> items;
 private UserOrderDto user;
 private DeliveryPersonOrderDto deliveryPerson;
 private PaymentDTO payment;
 private String deliveryProofImageUrl;

 // Constructors, getters, and setters
 public OrderDTO() {}
 
 public OrderDTO(Long id, BigDecimal totalAmount, OrderStatus status, 
               AddressDTO shippingAddress, LocalDateTime createdAt) {
     this.id = id;
     this.totalAmount = totalAmount;
     this.status = status;
     this.shippingAddress = shippingAddress;
     this.createdAt = createdAt;
 }
 
 // Getters and setters for all fields
}

 