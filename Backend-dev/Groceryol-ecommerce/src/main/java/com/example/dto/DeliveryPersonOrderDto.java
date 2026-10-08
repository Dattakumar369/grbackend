package com.example.dto;

import com.example.entity.DeliveryPerson;

public class DeliveryPersonOrderDto {
    private Long id;
     private String firstName;
     private Long mobileNumber;
     
    // Getters and Setters
    public static DeliveryPersonDto toDeliveryPersonDtoEntity(DeliveryPerson dp) {
        DeliveryPersonDto dto = new DeliveryPersonDto();
        dto.setId(dp.getId());
         dto.setFirstName(dp.getFirstName());
        dto.setLastName(dp.getLastName());
        dto.setMobileNumber(dp.getMobileNumber());
         dto.setVehicleType(dp.getVehicleType());
        dto.setLicenseNumber(dp.getLicenseNumber());
        dto.setVehicleRegistration(dp.getVehicleRegistration());
         return dto;
    }

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	 

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	 

	public Long getMobileNumber() {
		return mobileNumber;
	}

	public void setMobileNumber(Long mobileNumber) {
		this.mobileNumber = mobileNumber;
	}

	 
 

	 

 }