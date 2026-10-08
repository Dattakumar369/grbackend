package com.example.dao;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.entity.DeliveryPerson;
import com.example.entity.Order;
import com.example.entity.User;
import com.example.status.OrderStatus;

public interface OrderRepository extends JpaRepository<Order, Long> {
    
    List<Order> findByUser(User user);
    
    List<Order> findByStatus(OrderStatus status);
    List<Order> findByDeliveryPerson(DeliveryPerson deliveryPerson);
    // Find orders in PROCESSING status with no assigned delivery person
    @Query("SELECT o FROM Order o WHERE o.status = :status AND o.deliveryPerson IS NULL")
    List<Order> findByStatusAndDeliveryPersonIsNull(@Param("status") OrderStatus status);    
    // Find orders assigned to a delivery person with specific statuses
    List<Order> findByDeliveryPersonAndStatusIn(DeliveryPerson deliveryPerson, List<OrderStatus> statuses);    
    @Query("SELECT o FROM Order o WHERE o.deliveryPerson.id = :deliveryPersonId")
    List<Order> findByDeliveryPersonId(@Param("deliveryPersonId") Long deliveryPersonId);
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.id = :id")
    Optional<Order> findByIdWithItems(@Param("id") Long id);
    @Query("SELECT o FROM Order o WHERE o.user.id = :userId AND o.status = :status")
    List<Order> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") OrderStatus status);
    
    
    long countByStatus(OrderStatus status);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = :status")
    BigDecimal sumAmountByStatus(@Param("status") OrderStatus status);
    @Query("SELECT o FROM Order o WHERE o.status IN :statuses")
    List<Order> findByStatusIn(@Param("statuses") List<OrderStatus> statuses);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status IN ('PROCESSING', 'ACCEPTED_BY_DELIVERY', 'SHIPPED', 'OUT_FOR_DELIVERY', 'DELIVERED')")
    BigDecimal sumCompletedPayments();
    
    // Find pending orders older than specified time for cleanup
    @Query("SELECT o FROM Order o WHERE o.status = :status AND o.createdAt < :cutoffTime")
    List<Order> findPendingOrdersOlderThan(@Param("status") OrderStatus status, @Param("cutoffTime") LocalDateTime cutoffTime);
    
    @Query("UPDATE OrderItem oi SET oi.isShipped = true WHERE oi.id IN :itemIds")
    void markItemsAsShipped(@Param("itemIds") List<Long> itemIds);
 }