package com.example.dto;

import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.entity.Address;
import com.example.entity.DeliveryPerson;
import com.example.entity.Order;
import com.example.entity.OrderItem;
import com.example.entity.Payment;
import com.example.entity.User;
import com.example.service.S3Service;

@Component 
public class OrderMapper {
	
	 private final S3Service s3Service;

	    @Autowired
	    public OrderMapper(S3Service s3Service) {
	        this.s3Service = s3Service;
	    }
    
    public OrderDTO toDTO(Order order) {
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setStatus(order.getStatus());
        dto.setCreatedAt(order.getCreatedAt());
        
        if (order.getShippingAddress() != null) {
            dto.setShippingAddress(toAddressDTO(order.getShippingAddress()));
        }
        
        if (order.getUser() != null) {
            dto.setUser(toUserOrderDto(order.getUser()));
        }
        
        if (order.getDeliveryPerson() != null) {
            dto.setDeliveryPerson(toDeliveryPersonDTO(order.getDeliveryPerson()));
        }
        
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            dto.setItems(order.getItems().stream()
                .map(this::toOrderItemDTO)
                .collect(Collectors.toList()));
        }
        if (order.getPayment() != null) {
            dto.setPayment(toPaymentDTO(order.getPayment()));
        }

        if (order.getDeliveryProofImageKey() != null) {
            dto.setDeliveryProofImageUrl(s3Service.getFileUrl(order.getDeliveryProofImageKey()));
        }
        return dto;
    }
    
   
    private AddressDTO toAddressDTO(Address address) {
        AddressDTO dto = new AddressDTO();
        dto.setStreet(address.getStreet());
        dto.setCity(address.getCity());
        dto.setState(address.getState());
        dto.setPostalCode(address.getPostalCode());
        dto.setCountry(address.getCountry());
        return dto;
    }

// In OrderMapper.java
public OrderItemDTO toOrderItemDTO(OrderItem item) {
    if (item == null) {
        return null;
    }

    OrderItemDTO dto = new OrderItemDTO();
     dto.setProductId(item.getProduct().getId());
    dto.setProductName(item.getProduct().getName());
    dto.setQuantity(item.getQuantity());
    dto.setPrice(item.getPrice());
    dto.setTotalPrice(item.getTotalPrice());
    dto.setShipped(item.isShipped());
    
    // Add product images if needed
    if (item.getProduct() != null) {
        dto.setImage1(item.getProduct().getImage1());
        dto.setImage2(item.getProduct().getImage2());
        dto.setImage3(item.getProduct().getImage3());
    }
    
    return dto;
}    
    private UserOrderDto toUserOrderDto(User user) {
        UserOrderDto dto = new UserOrderDto();
        dto.setId(user.getId());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setEmailId(user.getEmail());
        dto.setMobileNo(user.getMobileNumber());

        return dto;
    }
    
    private DeliveryPersonOrderDto toDeliveryPersonDTO(DeliveryPerson dp) {
        DeliveryPersonOrderDto dto = new DeliveryPersonOrderDto();
        dto.setId(dp.getId());
        dto.setFirstName(dp.getFirstName() + " " + dp.getLastName());
        dto.setMobileNumber(dp.getMobileNumber());
        return dto;
    }
    
    private PaymentDTO toPaymentDTO(Payment payment) {
        PaymentDTO dto = new PaymentDTO();
        dto.setId(payment.getId());
        dto.setTransactionId(payment.getTransactionId());
        dto.setPaymentType(payment.getPaymentType().name());
        dto.setAmount(payment.getAmount());
        dto.setStatus(payment.getStatus().name());
        dto.setPaymentDate(payment.getCreatedAt());
        return dto;
    }
    
    
    
}