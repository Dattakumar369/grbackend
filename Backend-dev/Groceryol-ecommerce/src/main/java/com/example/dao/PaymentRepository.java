package com.example.dao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.entity.Payment;
import com.example.status.PaymentStatus;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByGatewayOrderId(String gatewayOrderId);
    Optional<Payment> findByOrder_Id(Long orderId);
    List<Payment> findByOrderUserIdOrderByCreatedAtDesc(Long userId);
    @Query("SELECT p FROM Payment p WHERE p.status = :status AND p.createdAt < :cutoffDate")
    List<Payment> findAbandonedPayments(
        @Param("status") PaymentStatus status,
        @Param("cutoffDate") LocalDateTime cutoffDate);
    
}