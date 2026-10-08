package com.example.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import com.example.service.PaymentService;

 
@Configuration
@EnableScheduling
public class PaymentCleanupScheduler {

    private static final Logger logger = LoggerFactory.getLogger(PaymentCleanupScheduler.class);
    
    @Autowired
    private PaymentService paymentService;
    
    @Value("${payment.cleanup.days.threshold:3}")
    private int daysThreshold;
    
    @Scheduled(cron = "0 0 3 * * ?") // Runs daily at 3 AM
    public void cleanupAbandonedPayments() {
        logger.info("Starting scheduled payment cleanup");
        try {
            paymentService.cleanupAbandonedPayments(daysThreshold);
            logger.info("Scheduled payment cleanup completed");
        } catch (Exception e) {
            logger.error("Error during scheduled payment cleanup", e);
        }
    }
}