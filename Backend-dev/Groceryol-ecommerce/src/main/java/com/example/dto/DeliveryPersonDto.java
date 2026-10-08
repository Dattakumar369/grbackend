package com.example.dto;

import com.example.entity.DeliveryPerson;

public class DeliveryPersonDto {
    private Long id;
     private String firstName;
    private String lastName;
    private Long mobileNumber;
     private String vehicleType;
    private String licenseNumber;
    private String vehicleRegistration;
 
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

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public Long getMobileNumber() {
		return mobileNumber;
	}

	public void setMobileNumber(Long mobileNumber) {
		this.mobileNumber = mobileNumber;
	}

	 

	public String getVehicleType() {
		return vehicleType;
	}

	public void setVehicleType(String vehicleType) {
		this.vehicleType = vehicleType;
	}

	public String getLicenseNumber() {
		return licenseNumber;
	}

	public void setLicenseNumber(String licenseNumber) {
		this.licenseNumber = licenseNumber;
	}

	public String getVehicleRegistration() {
		return vehicleRegistration;
	}

	public void setVehicleRegistration(String vehicleRegistration) {
		this.vehicleRegistration = vehicleRegistration;
	}

	 

 }