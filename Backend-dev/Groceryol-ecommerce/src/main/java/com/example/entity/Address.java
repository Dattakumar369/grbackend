package com.example.entity;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Data
@Embeddable
public class Address {
    private String street;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    
    @Override
    public String toString() {
        return String.format("%s, %s, %s %s, %s", 
            street, city, state, postalCode, country);
    }
}