package com.example.dto;

import org.springframework.beans.BeanUtils;

import com.example.entity.User;

import lombok.Data;
@Data
public class UserDto {
	
	private Long id;
	private String firstName;
	private String lastName;
	private String email;
     private String role;
    private Long mobileNumber;
  
	private String status;
	
	  public UserDto() {
	    }
	
	  public UserDto(Long id, String firstName, String lastName, String email, 
              String role, Long mobileNumber, String status) {
     this.id = id;
     this.firstName = firstName;
     this.lastName = lastName;
     this.email = email;
     this.role = role;
     this.mobileNumber = mobileNumber;
     this.status = status;
 }
	 

 


	// Convert Admin entity to AdminDto
	public static UserDto toUserDtoEntity(User user)  { 
		UserDto  adminDto=new UserDto();
		BeanUtils.copyProperties(user,  adminDto, "Mentee");

		return adminDto;
	}

}
