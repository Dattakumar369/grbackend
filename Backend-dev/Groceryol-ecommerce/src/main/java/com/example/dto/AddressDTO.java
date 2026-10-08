package com.example.dto;

import com.example.entity.Address;
import com.example.entity.UserAddress;

import lombok.Data;

@Data
public class AddressDTO {
    private String street;
    private String city;
    private String state;
    private String postalCode;
    private String country;

	 
}