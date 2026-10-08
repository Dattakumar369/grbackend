package com.example.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentDTO {
    private Long id;
    private String transactionId;
    private String paymentType;
    private BigDecimal amount;
    private String status;
    private LocalDateTime paymentDate;
}