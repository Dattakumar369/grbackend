// src/main/java/com/example/controller/UserController.java
package com.example.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.dto.DeliveryPersonDto;
import com.example.dto.DeliveryPersonRegistrationDto;
import com.example.dto.DeliveryPersonUpdateDto;
import com.example.dto.UpdateUserDTO;
import com.example.dto.UserDto;
import com.example.dto.UserLoginRequest;
import com.example.dto.UserLoginResponseJWT;
import com.example.entity.DeliveryPerson;
import com.example.entity.User;
import com.example.exception.ResourceNotFoundException;
import com.example.service.EmailService;
import com.example.service.S3Service;
import com.example.service.UserService;

import jakarta.mail.MessagingException;

@RestController
@RequestMapping("/groceryol")
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private S3Service s3Service;

    
    private final Map<String, String> otpStore = new ConcurrentHashMap<>();

    @PostMapping("/login")
    public ResponseEntity<UserLoginResponseJWT> login(@RequestBody UserLoginRequest userLoginRequest) {
        logger.info("Received login request for email: {}", userLoginRequest.getEmailId());
        return userService.login(userLoginRequest);
    }

    // User endpoints
    @PostMapping("/users/register")
    public ResponseEntity<?> registerUser(@RequestBody User user) {
        try {
            User registeredUser = userService.registerUser(user);
            logger.info("User registered successfully with ID: {}", registeredUser.getId());
            return buildSuccessResponse("User registered successfully.", registeredUser);
        } catch (Exception e) {
            logger.error("User registration failed: {}", e.getMessage());
            return buildErrorResponse("Failed to register user.", e);
        }
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable("id") Long id, @RequestBody UpdateUserDTO updateUserDTO) {
        try {
            User existingUser = userService.getUserById(id);
            existingUser.setFirstName(updateUserDTO.getFirstName());
            existingUser.setLastName(updateUserDTO.getLastName());
            existingUser.setMobileNumber(updateUserDTO.getMobileNumber());

            User updatedUser = userService.updateUser(id, existingUser);
            logger.info("User updated successfully with ID: {}", id);
            return buildSuccessResponse("User updated successfully.", updatedUser);
        } catch (Exception e) {
            logger.error("Failed to update user with ID {}: {}", id, e.getMessage());
            return buildErrorResponse("Failed to update user.", e);
        }
    }



    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable("id") Long id) {
        try {
            userService.deleteUser(id);
            logger.info("User deleted successfully with ID: {}", id);
            return buildSuccessResponse("User deleted successfully.", null);
        } catch (Exception e) {
            logger.error("Failed to delete user with ID {}: {}", id, e.getMessage());
            return buildErrorResponse("Failed to delete user.", e);
        }
    }

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        try {
            List<User> users = userService.getAllUsers();
            List<UserDto> userDtos = users.stream()
                .map(user -> new UserDto(
                    user.getId(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getEmail(),
                    user.getRole(),
                    user.getMobileNumber(),
                    user.getStatus()
                ))
                .collect(Collectors.toList());
            logger.info("Fetched {} users successfully", users.size());
            return buildSuccessResponse("Users fetched successfully.", userDtos);
        } catch (Exception e) {
            logger.error("Failed to fetch users: {}", e.getMessage());
            return buildErrorResponse("Failed to fetch users.", e);
        }
    }
    @GetMapping("/users/email/{email}")
    public ResponseEntity<?> getUserByEmail(@PathVariable String email) {
        try {
            User user = userService.findByEmail(email);
            logger.info("User found with email: {}", email);
            return buildSuccessResponse("User found successfully.", user);
        } catch (ResourceNotFoundException e) {
            logger.warn("User not found with email: {}", email);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "User not found."));
        } catch (Exception e) {
            logger.error("Failed to fetch user with email {}: {}", email, e.getMessage());
            return buildErrorResponse("Failed to find user.", e);
        }
    }

    // Delivery Person endpoints
    @PostMapping(value = "/delivery-persons/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> registerDeliveryPerson(
            @ModelAttribute DeliveryPersonRegistrationDto deliveryPersonDto) {
        try {
            DeliveryPerson registeredDP = userService.registerDeliveryPerson(deliveryPersonDto);
            logger.info("Delivery person registered successfully with ID: {}", registeredDP.getId());
            return buildSuccessResponse("Delivery person registered successfully.", registeredDP);
        } catch (Exception e) {
            logger.error("Delivery person registration failed: {}", e.getMessage());
            return buildErrorResponse("Failed to register delivery person.", e);
        }
    }
    
    @GetMapping("/delivery-persons/{id}/images")
    public ResponseEntity<?> getDeliveryPersonImages(@PathVariable Long id) {
        try {
            DeliveryPerson dp = userService.findDeliveryPersonById(id);
            
            Map<String, String> imageUrls = new HashMap<>();
            if (dp.getLicenseFrontImageKey() != null) {
                imageUrls.put("licenseFrontImageUrl", s3Service.getFileUrl(dp.getLicenseFrontImageKey()));
            }
            if (dp.getLicenseBackImageKey() != null) {
                imageUrls.put("licenseBackImageUrl", s3Service.getFileUrl(dp.getLicenseBackImageKey()));
            }
            if (dp.getVehicleImageKey() != null) {
                imageUrls.put("vehicleImageUrl", s3Service.getFileUrl(dp.getVehicleImageKey()));
            }
            if (dp.getProfileImageKey() != null) {
                imageUrls.put("profileImageUrl", s3Service.getFileUrl(dp.getProfileImageKey()));
            }
            
            return buildSuccessResponse("Image URLs retrieved successfully.", imageUrls);
        } catch (ResourceNotFoundException e) {
            logger.warn("Delivery person not found with ID: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "Delivery person not found."));
        } catch (Exception e) {
            logger.error("Failed to fetch delivery person images with ID {}: {}", id, e.getMessage());
            return buildErrorResponse("Failed to fetch delivery person images.", e);
        }
    }
@PutMapping(value = "/delivery-persons/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ResponseEntity<?> updateDeliveryPerson(
        @PathVariable Long id,
        @ModelAttribute DeliveryPersonUpdateDto deliveryPersonDto) {
    try {
        DeliveryPerson updatedDP = userService.updateDeliveryPerson(id, deliveryPersonDto);
        logger.info("Delivery person updated successfully with ID: {}", id);
        return ResponseEntity.ok(DeliveryPersonDto.toDeliveryPersonDtoEntity(updatedDP));
    } catch (ResourceNotFoundException e) {
        logger.error("Delivery person not found with ID: {}", id);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Delivery person not found.");
    } catch (Exception e) {
        logger.error("Failed to update delivery person with ID {}: {}", id, e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update delivery person.");
    }
}

    @DeleteMapping("/delivery-persons/{id}")
    public ResponseEntity<?> deleteDeliveryPerson(@PathVariable Long id) {
        try {
            userService.deleteDeliveryPerson(id);
            logger.info("Delivery person deleted successfully with ID: {}", id);
            return buildSuccessResponse("Delivery person deleted successfully.", null);
        } catch (Exception e) {
            logger.error("Failed to delete delivery person with ID {}: {}", id, e.getMessage());
            return buildErrorResponse("Failed to delete delivery person.", e);
        }
    }

    @GetMapping("/delivery-persons")
    public ResponseEntity<?> getAllDeliveryPersons() {
        try {
            List<DeliveryPerson> deliveryPersons = userService.getAllDeliveryPersons();
            logger.info("Fetched {} delivery persons successfully", deliveryPersons.size());
            return buildSuccessResponse("Delivery persons fetched successfully.", deliveryPersons);
        } catch (Exception e) {
            logger.error("Failed to fetch delivery persons: {}", e.getMessage());
            return buildErrorResponse("Failed to fetch delivery persons.", e);
        }
    }

    @GetMapping("/delivery-persons/email/{email}")
    public ResponseEntity<?> getDeliveryPersonByEmail(@PathVariable String email) {
        try {
            DeliveryPerson dp = userService.findDeliveryPersonByEmail(email);
            logger.info("Delivery person found with email: {}", email);
            return buildSuccessResponse("Delivery person found successfully.", dp);
        } catch (ResourceNotFoundException e) {
            logger.warn("Delivery person not found with email: {}", email);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "Delivery person not found."));
        } catch (Exception e) {
            logger.error("Failed to fetch delivery person with email {}: {}", email, e.getMessage());
            return buildErrorResponse("Failed to find delivery person.", e);
        }
    }

    // OTP related endpoints
    @PostMapping("/sendotp")
    public ResponseEntity<?> sendOtp(@RequestBody Map<String, String> request) {
        try {
            if (request == null || !request.containsKey("email")) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Email is required."
                ));
            }
            
            String email = request.get("email");
            if (email == null || email.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Invalid email address."
                ));
            }

            String otp = emailService.generateOtp();
            otpStore.put(email, otp);
            emailService.sendOtp(email, otp);
            logger.info("OTP sent successfully to: {}", email);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "OTP sent successfully to " + email
            ));
        } catch (MessagingException e) {
            logger.error("Failed to send OTP to {}: {}", request.get("email"), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Failed to send OTP.",
                "error", e.getMessage()
            ));
        }
    }


    @PostMapping("/validateotp")
    public ResponseEntity<?> validateOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String otp = request.get("otp");

        if (email == null || otp == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Email and OTP are required."
            ));
        }

        String storedOtp = otpStore.get(email);
        if (storedOtp != null && storedOtp.equals(otp)) {
            logger.info("OTP validated successfully for email: {}", email);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "OTP validated successfully!"
            ));
        } else {
            logger.warn("Invalid OTP attempt for email: {}", email);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "success", false,
                    "message", "Invalid OTP."
            ));
        }
    }
    
    
    
    
    
    @PostMapping("/request-password-change")
    public ResponseEntity<?> requestPasswordChange(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            
            if (email == null || email.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Email is required."
                ));
            }
            
            if (!userService.emailExists(email)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "message", "No account found with this email."
                ));
            }
            
            String otp = emailService.generateOtp();
            otpStore.put(email, otp);
            emailService.sendOtp(email, otp);
            logger.info("Password change OTP sent to: {}", email);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "OTP sent successfully for password change."
            ));
        } catch (MessagingException e) {
            logger.error("Failed to send password change OTP: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Failed to send OTP.",
                "error", e.getMessage()
            ));
        }
    }

@PostMapping("/change-password")
public ResponseEntity<?> changePasswordWithOtp(@RequestBody Map<String, String> request) {
    try {
        String email = request.get("email");
        String otp = request.get("otp");
        String newPassword = request.get("newPassword");
        String confirmPassword = request.get("confirmPassword");

        // Validate inputs
        if (email == null || otp == null || newPassword == null || confirmPassword == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Email, OTP, new password, and confirm password are required."
            ));
        }

        if (newPassword.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Password must be at least 6 characters long."
            ));
        }

        if (!newPassword.equals(confirmPassword)) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "New password and confirm password do not match."
            ));
        }

        // Validate OTP
        String storedOtp = otpStore.get(email);
        if (storedOtp == null || !storedOtp.equals(otp)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "success", false,
                "message", "Invalid or expired OTP."
            ));
        }

        // Change password
        boolean success = userService.changeUserPassword(email, newPassword);
        if (success) {
            // Remove used OTP
            otpStore.remove(email);
            logger.info("Password changed successfully for: {}", email);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Password changed successfully."
            ));
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Failed to change password."
            ));
        }
    } catch (Exception e) {
        logger.error("Password change failed: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
            "success", false,
            "message", "Failed to change password.",
            "error", e.getMessage()
        ));
    }
}


    // Helper methods
    private ResponseEntity<?> buildSuccessResponse(String message, Object data) {
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("success", true);
        response.put("message", message);
        if (data != null) {
            response.put("data", data);
        }
        return ResponseEntity.ok(response);
    }

    private ResponseEntity<?> buildErrorResponse(String message, Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", message,
                "error", e.getMessage()
        ));
    }
}