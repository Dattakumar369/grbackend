package com.example.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import com.example.service.OrderService;

/**
 * Scheduler for automatically cleaning up pending orders after a specified time threshold.
 * This helps maintain database cleanliness and frees up inventory for other customers.
 */
@Configuration
@EnableScheduling
public class PendingOrderCleanupScheduler {

    private static final Logger logger = LoggerFactory.getLogger(PendingOrderCleanupScheduler.class);
    
    @Autowired
    private OrderService orderService;
    
    @Value("${pending.order.cleanup.hours.threshold:1}")
    private int hoursThreshold;
    
    /**
     * Scheduled task to cleanup pending orders older than the configured threshold.
     * Runs every 15 minutes to ensure timely cleanup.
     */
    @Scheduled(fixedRate = 3600000) // 15 minutes = 900,000 milliseconds
    public void cleanupPendingOrders() {
        logger.info("Starting scheduled pending order cleanup (threshold: {} hours)", hoursThreshold);
        try {
            orderService.cleanupPendingOrders(hoursThreshold);
            logger.info("Scheduled pending order cleanup completed successfully");
        } catch (Exception e) {
            logger.error("Error during scheduled pending order cleanup", e);
        }
    }
    
    /**
     * Alternative cron-based scheduling - runs every hour at minute 0
     * Uncomment this method and comment out the fixedRate method above if you prefer cron-based scheduling
     */
    /*
    @Scheduled(cron = "0 0 * * * ?") // Runs every hour at minute 0
    public void cleanupPendingOrdersCron() {
        logger.info("Starting scheduled pending order cleanup (threshold: {} hours)", hoursThreshold);
        try {
            orderService.cleanupPendingOrders(hoursThreshold);
            logger.info("Scheduled pending order cleanup completed successfully");
        } catch (Exception e) {
            logger.error("Error during scheduled pending order cleanup", e);
        }
    }
    */
}
