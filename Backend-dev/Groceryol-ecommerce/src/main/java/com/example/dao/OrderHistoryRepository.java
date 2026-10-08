package com.example.dao;

import com.example.entity.Order;
import com.example.entity.OrderHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderHistoryRepository extends JpaRepository<OrderHistory, Long> {
    
    List<OrderHistory> findByOrderOrderByCreatedAtDesc(Order order);
    List<OrderHistory> findAllByOrderByCreatedAtDesc();

}