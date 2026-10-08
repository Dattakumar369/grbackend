package com.example.dto;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.example.entity.OrderHistory;

@Component
public class OrderHistoryMapper {
    
    private final OrderMapper orderMapper;
    
    public OrderHistoryMapper(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    public OrderHistoryDTO toDTO(OrderHistory history) {
        if (history == null) {
            return null;
        }

        OrderHistoryDTO dto = new OrderHistoryDTO();
        dto.setId(history.getId());
        dto.setOrderId(history.getOrder().getId());
        dto.setStatus(history.getStatus());
        dto.setNotes(history.getNotes());
        dto.setCreatedAt(history.getCreatedAt());

        // Map full order details
        if (history.getOrder() != null) {
            OrderDTO orderDTO = orderMapper.toDTO(history.getOrder());
            dto.setTotalAmount(orderDTO.getTotalAmount());
            dto.setShippingAddress(orderDTO.getShippingAddress());
            dto.setItems(orderDTO.getItems());
            dto.setUser(orderDTO.getUser());
            dto.setPayment(orderDTO.getPayment());
            
            // Map delivery person separately to maintain backward compatibility
            if (orderDTO.getDeliveryPerson() != null) {
                dto.setDeliveryPerson(orderDTO.getDeliveryPerson());
                dto.setDeliveryPersonId(orderDTO.getDeliveryPerson().getId());
                dto.setDeliveryPersonName(orderDTO.getDeliveryPerson().getFirstName());
            }
        }

        return dto;
    }

    public List<OrderHistoryDTO> toDTOList(List<OrderHistory> histories) {
        if (histories == null) {
            return null;
        }

        return histories.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
}