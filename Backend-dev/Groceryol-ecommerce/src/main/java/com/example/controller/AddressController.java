package com.example.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.dto.UserAddressDTO;
import com.example.entity.Address;
import com.example.entity.UserAddress;
import com.example.service.UserAddressService;

@RestController
@RequestMapping("/users/{userId}/addresses")
public class AddressController {
    
    private final UserAddressService userAddressService;
    
    public AddressController(UserAddressService userAddressService) {
        this.userAddressService = userAddressService;
    }
    
    @PostMapping
    public UserAddressDTO addAddress(
            @PathVariable Long userId,
            @RequestBody Address address,
            @RequestParam(required = false) String label,
            @RequestParam(defaultValue = "false") boolean makeDefault) {
        
        UserAddress newAddress = userAddressService.addAddress(userId, address, label, makeDefault);
        return new UserAddressDTO(newAddress);
    }
    
    @GetMapping
    public List<UserAddressDTO> getUserAddresses(@PathVariable Long userId) {
        return userAddressService.getUserAddresses(userId).stream()
                .map(UserAddressDTO::new)
                .collect(Collectors.toList());
    }
    
    @GetMapping("/default")
    public UserAddressDTO getDefaultAddress(@PathVariable Long userId) {
        return new UserAddressDTO(userAddressService.getDefaultAddress(userId));
    }
    
    @PutMapping("/{addressId}/set-default")
    public void setDefaultAddress(
            @PathVariable Long userId,
            @PathVariable Long addressId) {
        
        userAddressService.setDefaultAddress(userId, addressId);
    }
    
    @DeleteMapping("/{addressId}")
    public void deactivateAddress(
            @PathVariable Long userId,
            @PathVariable Long addressId) {
        
        userAddressService.deactivateAddress(userId, addressId);
    }
}