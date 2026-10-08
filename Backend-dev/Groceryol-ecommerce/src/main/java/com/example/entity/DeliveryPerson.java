package com.example.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Entity
public class DeliveryPerson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Size(max = 100, message = "Email must be less than 100 characters")
    private String email;

    @Column(nullable = false)
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    private String password;

    @NotBlank(message = "First name is required")
    @Size(max = 50, message = "First name must be less than 50 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 50, message = "Last name must be less than 50 characters")
    private String lastName;

    @NotNull(message = "Mobile number is required")
    private Long mobileNumber;

    @NotBlank(message = "Status is required")
     private String status;

    @NotBlank(message = "Vehicle type is required")
     private String vehicleType;

    @NotBlank(message = "License number is required")
    @Size(min = 5, max = 20, message = "License number must be between 5 and 20 characters")
     private String licenseNumber;

    @NotBlank(message = "Vehicle registration is required")
    @Size(min = 5, max = 20, message = "Vehicle registration must be between 5 and 20 characters")
     private String vehicleRegistration;

    @NotNull(message = "Availability status is required")
    private boolean isAvailable;
    
    @Column(nullable = false)
     private String role = "DELIVERY_PERSON";
    
    
    @Column(nullable = true)
    private String licenseFrontImageKey;
    
    @Column(nullable = true)
    private String licenseBackImageKey;
    
    @Column(nullable = true)
    private String vehicleImageKey;
    
    @Column(nullable = true)
    private String profileImageKey;
    
    
    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    // Getters and Setters remain unchanged
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean isAvailable) {
        this.isAvailable = isAvailable;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

	public String getLicenseFrontImageKey() {
		return licenseFrontImageKey;
	}

	public void setLicenseFrontImageKey(String licenseFrontImageKey) {
		this.licenseFrontImageKey = licenseFrontImageKey;
	}

	public String getLicenseBackImageKey() {
		return licenseBackImageKey;
	}

	public void setLicenseBackImageKey(String licenseBackImageKey) {
		this.licenseBackImageKey = licenseBackImageKey;
	}

	public String getVehicleImageKey() {
		return vehicleImageKey;
	}

	public void setVehicleImageKey(String vehicleImageKey) {
		this.vehicleImageKey = vehicleImageKey;
	}

	public String getProfileImageKey() {
		return profileImageKey;
	}

	public void setProfileImageKey(String profileImageKey) {
		this.profileImageKey = profileImageKey;
	}
    
    
    
}