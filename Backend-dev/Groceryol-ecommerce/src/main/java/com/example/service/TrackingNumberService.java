package com.example.service;

import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class TrackingNumberService {
    
    private static final AtomicInteger counter = new AtomicInteger(1000);
    private static final String PREFIX = "TRK";
    
    public String generateTrackingNumber(Long orderId) {
        LocalDateTime now = LocalDateTime.now();
        int sequence = counter.getAndIncrement();
        if (sequence > 9999) {
            counter.set(1000);
            sequence = 1000;
        }
        
        return String.format("%s%s%04d%02d%02d%04d",
            PREFIX,
            String.valueOf(orderId).substring(Math.max(0, String.valueOf(orderId).length() - 3)), // Last 3 digits of order ID
            now.getYear() % 100, // Last 2 digits of year
            now.getMonthValue(),
            now.getDayOfMonth(),
            sequence);
    }
}