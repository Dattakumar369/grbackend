package com.example.dto;

import lombok.Data;

@Data
public class UserOrderDto {
	private Long id;
	private String firstName;
	private String lastName;
	private String emailId;
    
    private Long mobileNo;
}
