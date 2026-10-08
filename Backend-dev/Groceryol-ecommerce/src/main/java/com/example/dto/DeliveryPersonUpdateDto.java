package com.example.dto;

import org.springframework.web.multipart.MultipartFile;

public class DeliveryPersonUpdateDto {
    private String firstName;
    private String lastName;
    private Long mobileNumber;
    private String vehicleType;
    private String licenseNumber;
    private String vehicleRegistration;
    private MultipartFile licenseFrontImage;
    private MultipartFile licenseBackImage;
    private MultipartFile vehicleImage;
    private MultipartFile profileImage;

    // Getters and Setters
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

    public MultipartFile getLicenseFrontImage() {
        return licenseFrontImage;
    }

    public void setLicenseFrontImage(MultipartFile licenseFrontImage) {
        this.licenseFrontImage = licenseFrontImage;
    }

    public MultipartFile getLicenseBackImage() {
        return licenseBackImage;
    }

    public void setLicenseBackImage(MultipartFile licenseBackImage) {
        this.licenseBackImage = licenseBackImage;
    }

    public MultipartFile getVehicleImage() {
        return vehicleImage;
    }

    public void setVehicleImage(MultipartFile vehicleImage) {
        this.vehicleImage = vehicleImage;
    }

    public MultipartFile getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(MultipartFile profileImage) {
        this.profileImage = profileImage;
    }
}