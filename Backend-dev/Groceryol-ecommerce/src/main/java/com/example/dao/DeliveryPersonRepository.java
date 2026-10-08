package com.example.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.entity.DeliveryPerson;

public interface DeliveryPersonRepository extends JpaRepository<DeliveryPerson, Long> {
    // Basic find methods
    Optional<DeliveryPerson> findByEmail(String email);
    Optional<DeliveryPerson> findByEmailAndStatus(String email, String status);
    
    // Find all active delivery persons (removed availability check)
    @Query("SELECT dp FROM DeliveryPerson dp WHERE dp.status = 'ACTIVE'")
    List<DeliveryPerson> findAllActiveDeliveryPersons();
    
    // Optional: Find delivery persons with the fewest current orders
    @Query("SELECT dp FROM DeliveryPerson dp WHERE dp.status = 'ACTIVE' " +
           "ORDER BY (SELECT COUNT(o) FROM Order o WHERE o.deliveryPerson = dp AND o.status NOT IN ('DELIVERED', 'CANCELLED')) ASC")
    List<DeliveryPerson> findLeastBusyDeliveryPersons();
    
    // Optional: Count active orders for a delivery person
    @Query("SELECT COUNT(o) FROM Order o WHERE o.deliveryPerson = ?1 AND o.status NOT IN ('DELIVERED', 'CANCELLED')")
    long countActiveOrdersForDeliveryPerson(DeliveryPerson deliveryPerson);
 }