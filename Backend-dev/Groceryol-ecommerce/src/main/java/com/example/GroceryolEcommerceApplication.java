package com.example;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.entity.User;
import com.example.service.UserService;

@SpringBootApplication
@EnableCaching
public class GroceryolEcommerceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GroceryolEcommerceApplication.class, args);
    }

    @Bean
    public CommandLineRunner createAdminUser(UserService userService, PasswordEncoder passwordEncoder) {
        return args -> {
            String adminEmail = "admin@example.com";
            String adminPassword = "Dattu@123";
            String adminRole = "Admin";
            String adminLastName = "Admin";
            Long adminMobileNumber = 9876543210L;

            if (userService.findByEmailAndRole(adminEmail, adminRole) == null) {
                User adminUser = new User();
                adminUser.setEmail(adminEmail);
                adminUser.setPassword(passwordEncoder.encode(adminPassword));
                adminUser.setRole(adminRole);
                adminUser.setFirstName("Dattu");
                adminUser.setLastName(adminLastName);
                adminUser.setMobileNumber(adminMobileNumber);
                adminUser.setStatus("Active");
                userService.save(adminUser);
            }
        };
    }
}