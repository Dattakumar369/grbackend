package com.example.dto;

public class UserLoginResponseJWT {
    private boolean status;
    private String message;
    private String jwtToken;
    private UserDto user;
    private DeliveryPersonDto deliveryPerson;

    // Getters and Setters
    public boolean isStatus() {
        return status;
    }
    public void setStatus(boolean status) {
        this.status = status;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
    public String getJwtToken() {
        return jwtToken;
    }
    public void setJwtToken(String jwtToken) {
        this.jwtToken = jwtToken;
    }
    public UserDto getUser() {
        return user;
    }
    public void setUser(UserDto user) {
        this.user = user;
    }
    public DeliveryPersonDto getDeliveryPerson() {
        return deliveryPerson;
    }
    public void setDeliveryPerson(DeliveryPersonDto deliveryPerson) {
        this.deliveryPerson = deliveryPerson;
    }
}