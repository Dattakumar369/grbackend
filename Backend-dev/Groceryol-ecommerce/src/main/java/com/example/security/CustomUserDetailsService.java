package com.example.security;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import com.example.dao.DeliveryPersonRepository;
import com.example.entity.DeliveryPerson;
import com.example.entity.User;
import com.example.service.UserService;

@Component
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    @Lazy
    private UserService userService;
    
    @Autowired
    @Lazy
    private DeliveryPersonRepository deliveryPersonRepository;
    
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Try to find as User first
        Optional<User> userOpt = userService.getUserByEmail(email);
        if (userOpt.isPresent()) {
            return new CustomUserDetails(userOpt.get());
        }
        
        // If not found as User, try as DeliveryPerson
        Optional<DeliveryPerson> deliveryPersonOpt = deliveryPersonRepository.findByEmail(email);
        if (deliveryPersonOpt.isPresent()) {
            return new CustomUserDetails(deliveryPersonOpt.get());
        }
        
        throw new UsernameNotFoundException("User not found with email: " + email);
    }
}