package com.example.service;

import com.example.dao.UserAddressRepository;
import com.example.dao.UserRepository;
import com.example.entity.Address;
import com.example.entity.User;
import com.example.entity.UserAddress;
import com.example.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserAddressService {
    
    private final UserAddressRepository userAddressRepository;
    private final UserRepository userRepository;
    
    public UserAddressService(UserAddressRepository userAddressRepository, 
                            UserRepository userRepository) {
        this.userAddressRepository = userAddressRepository;
        this.userRepository = userRepository;
    }
    
    @Transactional
    public UserAddress addAddress(Long userId, Address address, String label, boolean makeDefault) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        UserAddress userAddress = new UserAddress();
        userAddress.setUser(user);
        userAddress.setAddress(address);
        userAddress.setAddressLabel(label);
        userAddress.setActive(true);
        
        if (makeDefault) {
            // Unset any existing default address
            userAddressRepository.findByUserAndIsDefaultTrue(user)
                    .ifPresent(existingDefault -> {
                        existingDefault.setIsDefault(false);
                        userAddressRepository.save(existingDefault);
                    });
            userAddress.setIsDefault(true);
        }
        
        return userAddressRepository.save(userAddress);
    }
    
    @Transactional
    public void setDefaultAddress(Long userId, Long addressId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        UserAddress newDefault = userAddressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + addressId));
        
        if (!newDefault.getUser().getId().equals(userId)) {
            throw new SecurityException("Address does not belong to user");
        }
        
        // Unset existing default
        userAddressRepository.findByUserAndIsDefaultTrue(user)
                .ifPresent(existingDefault -> {
                    existingDefault.setIsDefault(false);
                    userAddressRepository.save(existingDefault);
                });
        
        // Set new default
        newDefault.setIsDefault(true);
        userAddressRepository.save(newDefault);
    }
    
    @Transactional
    public void deactivateAddress(Long userId, Long addressId) {
        UserAddress address = userAddressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + addressId));
        
        if (!address.getUser().getId().equals(userId)) {
            throw new SecurityException("Address does not belong to user");
        }
        
        address.setActive(false);
        if (address.getIsDefault()) {
            address.setIsDefault(false);
            // Optionally set another address as default
        }
        
        userAddressRepository.save(address);
    }
    
    public List<UserAddress> getUserAddresses(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return userAddressRepository.findByUserAndActiveTrue(user);
    }
    
    public UserAddress getDefaultAddress(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return userAddressRepository.findByUserAndIsDefaultTrue(user)
                .orElseThrow(() -> new ResourceNotFoundException("No default address found for user: " + userId));
    }
    
    public UserAddress getAddressForUser(Long userId, Long addressId) {
        UserAddress address = userAddressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + addressId));
        
        if (!address.getUser().getId().equals(userId)) {
            throw new SecurityException("Address does not belong to user");
        }
        
        return address;
    }
}