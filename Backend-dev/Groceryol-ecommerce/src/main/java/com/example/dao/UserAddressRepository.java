package com.example.dao;

import com.example.entity.User;
import com.example.entity.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {
    List<UserAddress> findByUser(User user);
    List<UserAddress> findByUserAndActiveTrue(User user);
    Optional<UserAddress> findByUserAndIsDefaultTrue(User user);
}