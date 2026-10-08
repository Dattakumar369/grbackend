package com.example.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.dao.DeliveryPersonRepository;
import com.example.dao.UserRepository;
import com.example.dto.DeliveryPersonDto;
import com.example.dto.DeliveryPersonRegistrationDto;
import com.example.dto.DeliveryPersonUpdateDto;
import com.example.dto.UserDto;
import com.example.dto.UserLoginRequest;
import com.example.dto.UserLoginResponseJWT;
import com.example.entity.DeliveryPerson;
import com.example.entity.User;
import com.example.exception.EmailAlreadyExistsException;
import com.example.exception.ResourceNotFoundException;
import com.example.security.CustomUserDetails;
import com.example.security.JwtUtils;

@Service
@Transactional
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private DeliveryPersonRepository deliveryPersonRepository;
    
    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;
    
    @Autowired
    private S3Service s3Service;

    public User registerUser(User user) {
        logger.info("Attempting to register user with email: {}", user.getEmail());
        
        user.setId(null);

        if (userRepository.findByEmail(user.getEmail()).isPresent() || 
            deliveryPersonRepository.findByEmail(user.getEmail()).isPresent()) {
            logger.warn("Registration failed - Email already exists: {}", user.getEmail());
            try {
                emailService.sendRegistrationFailureEmail(user.getEmail(), 
                    "The email address is already registered with us.");
            } catch (Exception e) {
                logger.error("Failed to send registration failure email: {}", e.getMessage());
            }
            throw new EmailAlreadyExistsException("Email already exists!");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setStatus("Active");
        user.setRole("Buyer");
        
        User savedUser = userRepository.save(user);
        logger.info("User registered successfully with ID: {}", savedUser.getId());
        
        try {
            emailService.sendRegistrationSuccessEmail(
                savedUser.getEmail(), 
                savedUser.getFirstName() + " " + savedUser.getLastName(),
                "Buyer"
            );
        } catch (Exception e) {
            logger.error("Failed to send registration success email: {}", e.getMessage());
        }
        
        return savedUser;
    }

 public DeliveryPerson registerDeliveryPerson(DeliveryPersonRegistrationDto deliveryPersonDto) throws IOException {
        logger.info("Attempting to register delivery person with email: {}", deliveryPersonDto.getEmail());
        
        // Check if email exists
        if (userRepository.findByEmail(deliveryPersonDto.getEmail()).isPresent() || 
            deliveryPersonRepository.findByEmail(deliveryPersonDto.getEmail()).isPresent()) {
            logger.warn("Delivery person registration failed - Email already exists: {}", deliveryPersonDto.getEmail());
            try {
                emailService.sendRegistrationFailureEmail(deliveryPersonDto.getEmail(), 
                    "The email address is already registered with us.");
            } catch (Exception e) {
                logger.error("Failed to send registration failure email: {}", e.getMessage());
            }
            throw new EmailAlreadyExistsException("Email already exists!");
        }

        // Create folder path for this delivery person
        String dpFolderPath = buildDeliveryPersonFolderPath(deliveryPersonDto.getEmail());

        // Upload images to S3 with organized folder structure
        String licenseFrontImageKey = null;
        String licenseBackImageKey = null;
        String vehicleImageKey = null;
        String profileImageKey = null;
        
        if (deliveryPersonDto.getLicenseFrontImage() != null && !deliveryPersonDto.getLicenseFrontImage().isEmpty()) {
            licenseFrontImageKey = s3Service.uploadFile(
                deliveryPersonDto.getLicenseFrontImage(), 
                dpFolderPath + "license/front/"
            );
        }
        if (deliveryPersonDto.getLicenseBackImage() != null && !deliveryPersonDto.getLicenseBackImage().isEmpty()) {
            licenseBackImageKey = s3Service.uploadFile(
                deliveryPersonDto.getLicenseBackImage(), 
                dpFolderPath + "license/back/"
            );
        }
        if (deliveryPersonDto.getVehicleImage() != null && !deliveryPersonDto.getVehicleImage().isEmpty()) {
            vehicleImageKey = s3Service.uploadFile(
                deliveryPersonDto.getVehicleImage(), 
                dpFolderPath + "vehicle/"
            );
        }
        if (deliveryPersonDto.getProfileImage() != null && !deliveryPersonDto.getProfileImage().isEmpty()) {
            profileImageKey = s3Service.uploadFile(
                deliveryPersonDto.getProfileImage(), 
                dpFolderPath + "profile/"
            );
        }

        // Create DeliveryPerson entity
        DeliveryPerson deliveryPerson = new DeliveryPerson();
        deliveryPerson.setEmail(deliveryPersonDto.getEmail());
        deliveryPerson.setPassword(passwordEncoder.encode(deliveryPersonDto.getPassword()));
        deliveryPerson.setFirstName(deliveryPersonDto.getFirstName());
        deliveryPerson.setLastName(deliveryPersonDto.getLastName());
        deliveryPerson.setMobileNumber(deliveryPersonDto.getMobileNumber());
        deliveryPerson.setVehicleType(deliveryPersonDto.getVehicleType());
        deliveryPerson.setLicenseNumber(deliveryPersonDto.getLicenseNumber());
        deliveryPerson.setVehicleRegistration(deliveryPersonDto.getVehicleRegistration());
        deliveryPerson.setStatus("PENDING_APPROVAL"); // Start with pending approval
        deliveryPerson.setRole("DELIVERY_PERSON");
        deliveryPerson.setAvailable(false); // Not available until approved
        deliveryPerson.setLicenseFrontImageKey(licenseFrontImageKey);
        deliveryPerson.setLicenseBackImageKey(licenseBackImageKey);
        deliveryPerson.setVehicleImageKey(vehicleImageKey);
        deliveryPerson.setProfileImageKey(profileImageKey);
        
        DeliveryPerson savedDP = deliveryPersonRepository.save(deliveryPerson);
        logger.info("Delivery person registered successfully with ID: {}", savedDP.getId());
        
        try {
            emailService.sendRegistrationSuccessEmail(
                savedDP.getEmail(), 
                savedDP.getFirstName() + " " + savedDP.getLastName(),
                "Delivery Person"
            );
        } catch (Exception e) {
            logger.error("Failed to send registration success email: {}", e.getMessage());
        }
        
        return savedDP;
    }
   

public User updateUser(Long id, User user) {
        logger.info("Updating user with ID: {}", id);
        if (!userRepository.existsById(id)) {
            logger.error("User not found with ID: {}", id);
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        user.setId(id);
        User updatedUser = userRepository.save(user);
        
        try {
            emailService.sendProfileUpdateEmail(
                updatedUser.getEmail(),
                updatedUser.getFirstName() + " " + updatedUser.getLastName()
            );
        } catch (Exception e) {
            logger.error("Failed to send profile update email: {}", e.getMessage());
        }
        
        return updatedUser;
    }
    
    public DeliveryPerson findDeliveryPersonById(Long id) throws ResourceNotFoundException {
        logger.info("Fetching delivery person with ID: {}", id);
        return deliveryPersonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery person not found with id: " + id));
    }

public DeliveryPerson updateDeliveryPerson(Long id, DeliveryPersonUpdateDto deliveryPersonDto) throws IOException {
        logger.info("Updating delivery person with ID: {}", id);

        DeliveryPerson existingDP = deliveryPersonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery person not found with id: " + id));

        // Get folder path for this delivery person
        String dpFolderPath = buildDeliveryPersonFolderPath(existingDP.getEmail());

        // Update basic fields
        if (deliveryPersonDto.getFirstName() != null) {
            existingDP.setFirstName(deliveryPersonDto.getFirstName());
        }
        if (deliveryPersonDto.getLastName() != null) {
            existingDP.setLastName(deliveryPersonDto.getLastName());
        }
        if (deliveryPersonDto.getMobileNumber() != null) {
            existingDP.setMobileNumber(deliveryPersonDto.getMobileNumber());
        }
        if (deliveryPersonDto.getVehicleType() != null) {
            existingDP.setVehicleType(deliveryPersonDto.getVehicleType());
        }
        if (deliveryPersonDto.getLicenseNumber() != null) {
            existingDP.setLicenseNumber(deliveryPersonDto.getLicenseNumber());
        }
        if (deliveryPersonDto.getVehicleRegistration() != null) {
            existingDP.setVehicleRegistration(deliveryPersonDto.getVehicleRegistration());
        }

        // Handle image updates
        if (deliveryPersonDto.getLicenseFrontImage() != null && !deliveryPersonDto.getLicenseFrontImage().isEmpty()) {
            // Delete old image if exists
            if (existingDP.getLicenseFrontImageKey() != null && s3Service.fileExists(existingDP.getLicenseFrontImageKey())) {
                s3Service.deleteFile(existingDP.getLicenseFrontImageKey());
            }
            // Upload new image
            existingDP.setLicenseFrontImageKey(
                s3Service.uploadFile(deliveryPersonDto.getLicenseFrontImage(), dpFolderPath + "license/front/")
            );
        }
        
        if (deliveryPersonDto.getLicenseBackImage() != null && !deliveryPersonDto.getLicenseBackImage().isEmpty()) {
            if (existingDP.getLicenseBackImageKey() != null && s3Service.fileExists(existingDP.getLicenseBackImageKey())) {
                s3Service.deleteFile(existingDP.getLicenseBackImageKey());
            }
            existingDP.setLicenseBackImageKey(
                s3Service.uploadFile(deliveryPersonDto.getLicenseBackImage(), dpFolderPath + "license/back/")
            );
        }
        
        if (deliveryPersonDto.getVehicleImage() != null && !deliveryPersonDto.getVehicleImage().isEmpty()) {
            if (existingDP.getVehicleImageKey() != null && s3Service.fileExists(existingDP.getVehicleImageKey())) {
                s3Service.deleteFile(existingDP.getVehicleImageKey());
            }
            existingDP.setVehicleImageKey(
                s3Service.uploadFile(deliveryPersonDto.getVehicleImage(), dpFolderPath + "vehicle/")
            );
        }
        
        if (deliveryPersonDto.getProfileImage() != null && !deliveryPersonDto.getProfileImage().isEmpty()) {
            if (existingDP.getProfileImageKey() != null && s3Service.fileExists(existingDP.getProfileImageKey())) {
                s3Service.deleteFile(existingDP.getProfileImageKey());
            }
            existingDP.setProfileImageKey(
                s3Service.uploadFile(deliveryPersonDto.getProfileImage(), dpFolderPath + "profile/")
            );
        }

        DeliveryPerson updatedDP = deliveryPersonRepository.save(existingDP);

        try {
            emailService.sendProfileUpdateEmail(
                    updatedDP.getEmail(),
                    updatedDP.getFirstName() + " " + updatedDP.getLastName()
            );
        } catch (Exception e) {
            logger.error("Failed to send profile update email: {}", e.getMessage());
        }

        return updatedDP;
    }
private String buildDeliveryPersonFolderPath(String email) {
    // Normalize email for folder path (replace @ and . with -)
    String normalizedEmail = email.replace("@", "-at-").replace(".", "-");
    return String.format("delivery-persons/%s/", normalizedEmail);
}
   
public User getUserById(Long id) {
        logger.info("Fetching user by ID: {}", id);
        return userRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("User not found with ID: {}", id);
                    return new ResourceNotFoundException("User not found with id: " + id);
                });
    }

    public void deleteUser(Long id) {
        logger.info("Deleting user with ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("User not found with ID: {}", id);
                    return new ResourceNotFoundException("User not found with id: " + id);
                });
        
        userRepository.deleteById(id);
        logger.info("User deleted successfully with ID: {}", id);
        
        try {
            emailService.sendAccountDeletionEmail(
                user.getEmail(),
                user.getFirstName() + " " + user.getLastName()
            );
        } catch (Exception e) {
            logger.error("Failed to send account deletion email: {}", e.getMessage());
        }
    }
    
    public void deleteDeliveryPerson(Long id) {
        logger.info("Deleting delivery person with ID: {}", id);
        DeliveryPerson dp = deliveryPersonRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Delivery person not found with ID: {}", id);
                    return new ResourceNotFoundException("Delivery person not found with id: " + id);
                });
        
        deliveryPersonRepository.deleteById(id);
        logger.info("Delivery person deleted successfully with ID: {}", id);
        
        try {
            emailService.sendAccountDeletionEmail(
                dp.getEmail(),
                dp.getFirstName() + " " + dp.getLastName()
            );
        } catch (Exception e) {
            logger.error("Failed to send account deletion email: {}", e.getMessage());
        }
    }
    
    
    private void deleteDeliveryPersonImages(DeliveryPerson dp) {
        logger.debug("Deleting images for delivery person ID: {}", dp.getId());
        
        if (dp.getLicenseFrontImageKey() != null && s3Service.fileExists(dp.getLicenseFrontImageKey())) {
            s3Service.deleteFile(dp.getLicenseFrontImageKey());
        }
        if (dp.getLicenseBackImageKey() != null && s3Service.fileExists(dp.getLicenseBackImageKey())) {
            s3Service.deleteFile(dp.getLicenseBackImageKey());
        }
        if (dp.getVehicleImageKey() != null && s3Service.fileExists(dp.getVehicleImageKey())) {
            s3Service.deleteFile(dp.getVehicleImageKey());
        }
        if (dp.getProfileImageKey() != null && s3Service.fileExists(dp.getProfileImageKey())) {
            s3Service.deleteFile(dp.getProfileImageKey());
        }
    }
    
    public List<User> getAllUsers() {
        logger.info("Fetching all users");
        return userRepository.findAll();
    }
    
    public User findByEmail(String email) {
        logger.info("Fetching user by email: {}", email);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.error("User not found with email: {}", email);
                    return new ResourceNotFoundException("User not found with email: " + email);
                });
    }
    
    public DeliveryPerson findDeliveryPersonByEmail(String email) {
        logger.info("Fetching delivery person by email: {}", email);
        return deliveryPersonRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.error("Delivery person not found with email: {}", email);
                    return new ResourceNotFoundException("Delivery person not found with email: " + email);
                });
    }

    public ResponseEntity<UserLoginResponseJWT> login(UserLoginRequest loginRequest) {
        logger.info("Login attempt for email: {}", loginRequest.getEmailId());
        UserLoginResponseJWT response = new UserLoginResponseJWT();
        
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getEmailId(),
                    loginRequest.getPassword()
                )
            );

            String jwtToken = jwtUtils.generateToken(loginRequest.getEmailId());
            response.setJwtToken(jwtToken);
            response.setStatus(true);
            response.setMessage("Login successful");

            Object principal = authentication.getPrincipal();
            if (principal instanceof CustomUserDetails) {
                CustomUserDetails userDetails = (CustomUserDetails) principal;
                
                if (userDetails.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_DELIVERY_PERSON"))) {
                    
                    DeliveryPerson dp = deliveryPersonRepository.findByEmail(loginRequest.getEmailId())
                        .orElseThrow(() -> new ResourceNotFoundException("Delivery person not found"));
                    response.setDeliveryPerson(DeliveryPersonDto.toDeliveryPersonDtoEntity(dp));
                } 
                else if (userDetails.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"))) {
                    
                    User admin = userRepository.findByEmail(loginRequest.getEmailId())
                        .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));
                    response.setUser(UserDto.toUserDtoEntity(admin));
                }
                else {
                    User user = userRepository.findByEmail(loginRequest.getEmailId())
                        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                    response.setUser(UserDto.toUserDtoEntity(user));
                }
            }

            logger.info("Login successful for email: {}", loginRequest.getEmailId());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Login failed for email: {} - Reason: {}", loginRequest.getEmailId(), e.getMessage());
            response.setMessage("Authentication failed: " + e.getMessage());
            response.setStatus(false);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }
    
    private void authenticateUser(UserLoginRequest loginRequest, String role) {
        List<GrantedAuthority> authorities = Arrays.asList(new SimpleGrantedAuthority("ROLE_" + role));
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getEmailId(),
                loginRequest.getPassword(), authorities));
    }
    
    public User getByemail(String email) {
        return userRepository.findByEmail(email).get();
    }
    
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    public User findByEmailAndRole(String email, String role) {
        return userRepository.findByEmailAndRole(email, role);
    }
    
    public User save(User user) {
        return userRepository.save(user);
    }
    
    // Delivery Person specific methods
    public List<DeliveryPerson> getAllDeliveryPersons() {
        return deliveryPersonRepository.findAll();
    }
    
    public DeliveryPerson getDeliveryPersonById(Long id) {
        return deliveryPersonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery person not found with id: " + id));
    }
    
    public boolean changeUserPassword(String email, String newPassword) {
        logger.info("Attempting to change password for user with email: {}", email);
        
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            logger.info("Password changed successfully for user with email: {}", email);
            
            try {
                emailService.sendPasswordChangeEmail(
                    user.getEmail(),
                    user.getFirstName() + " " + user.getLastName()
                );
            } catch (Exception e) {
                logger.error("Failed to send password change email: {}", e.getMessage());
            }
            
            return true;
        }
        
        Optional<DeliveryPerson> dpOptional = deliveryPersonRepository.findByEmail(email);
        if (dpOptional.isPresent()) {
            DeliveryPerson dp = dpOptional.get();
            dp.setPassword(passwordEncoder.encode(newPassword));
            deliveryPersonRepository.save(dp);
            logger.info("Password changed successfully for delivery person with email: {}", email);
            
            try {
                emailService.sendPasswordChangeEmail(
                    dp.getEmail(),
                    dp.getFirstName() + " " + dp.getLastName()
                );
            } catch (Exception e) {
                logger.error("Failed to send password change email: {}", e.getMessage());
            }
            
            return true;
        }
        
        logger.warn("Password change failed - No user found with email: {}", email);
        return false;
    }

    public boolean emailExists(String email) {
        return userRepository.findByEmail(email).isPresent() || 
               deliveryPersonRepository.findByEmail(email).isPresent();
    }
}