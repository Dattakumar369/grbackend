package com.example.dto;

import com.example.entity.Address;
import com.example.entity.UserAddress;

import lombok.Data;

@Data
public class UserAddressDTO {
    private Long id;
    private Address address;
    private Boolean isDefault;
    private String addressLabel;
    private Boolean active;
    
    public UserAddressDTO(UserAddress userAddress) {
        this.id = userAddress.getId();
        this.address = userAddress.getAddress();
        this.isDefault = userAddress.getIsDefault();
        this.addressLabel = userAddress.getAddressLabel();
        this.active = userAddress.getActive();
    }
}