package com.example.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.entity.Payment;
import com.example.exception.PaymentProcessingException;
import com.example.service.PaymentService;
import com.example.status.PaymentType;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // ====================== PAYMENT INITIATION & STATUS ====================== //

    /**
     * Get payment details for initiating Razorpay payment
     * @param orderId The ID of the order to process payment for
     * @return ResponseEntity containing payment details or error message
     */
    @GetMapping("/details")
    public ResponseEntity<Map<String, String>> getPaymentDetails(@RequestParam Long orderId) {
        logger.info("Requesting payment details for order ID: {}", orderId);
        
        try {
            Map<String, String> paymentDetails = paymentService.getPaymentDetails(orderId);
            logger.debug("Successfully retrieved payment details for order: {}", orderId);
            return ResponseEntity.ok(paymentDetails);
        } catch (Exception e) {
            logger.error("Failed to get payment details for order {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of(
                        "error", "Failed to get payment details",
                        "message", e.getMessage()
                    ));
        }
    }

    /**
     * Check payment status for an order
     * @param orderId The ID of the order to check
     * @return ResponseEntity with payment status information
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> checkPaymentStatus(@RequestParam Long orderId) {
        logger.info("Checking payment status for order ID: {}", orderId);
        
        try {
            Map<String, String> paymentDetails = paymentService.getPaymentDetails(orderId);
            String status = paymentDetails.containsKey("error") ? "failed" : "completed";
            
            logger.debug("Payment status for order {}: {}", orderId, status);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "payment_status", status,
                "order_id", orderId.toString()
            ));
        } catch (Exception e) {
            logger.error("Error checking payment status for order {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage(),
                "order_id", orderId.toString()
            ));
        }
    }

    // ====================== PAYMENT PROCESSING ====================== //

    /**
     * Verify payment after successful Razorpay transaction
     * @param paymentId Payment ID from Razorpay
     * @param orderId Order ID associated with the payment
     * @param signature Payment signature for verification
     * @return ResponseEntity indicating verification result
     */
@PostMapping("/verify")
public ResponseEntity<Map<String, String>> verifyPayment(
        @RequestParam String paymentId,
        @RequestParam String orderId,
        @RequestParam String signature) {
    
    logger.info("Payment verification request received. PaymentID: {}, OrderID: {}", 
        paymentId, orderId);
    
    try {
        paymentService.verifyAndCompletePayment(paymentId, orderId, signature);
        logger.info("Payment verification successful for PaymentID: {}", paymentId);
        
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "Payment verified successfully",
            "payment_id", paymentId,
            "order_id", orderId
        ));
    } catch (Exception e) {
        logger.error("Payment verification failed. PaymentID: {}, Error: {}", 
            paymentId, e.getMessage(), e);
        
        return ResponseEntity.badRequest().body(Map.of(
            "status", "error",
            "message", e.getMessage(),
            "payment_id", paymentId,
            "order_id", orderId
        ));
    }
}
    // ====================== WEBHOOK HANDLER ====================== //

    /**
     * Handle Razorpay webhook events
     * @param webhookData Webhook payload from Razorpay containing payment event details
     * @return ResponseEntity acknowledging receipt of webhook
     */
@PostMapping("/payment/webhook")
public ResponseEntity<String> handlePaymentWebhook(
        @RequestBody Map<String, Object> webhookData,
        @RequestHeader("X-Razorpay-Signature") String razorpaySignature) {
    try {
        paymentService.handleWebhookEvent(webhookData, razorpaySignature);
        return ResponseEntity.ok("Webhook processed successfully");
    } catch (PaymentProcessingException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}	






/**
 * Retry a failed or pending payment
 * @param orderId The order ID to retry payment for
 * @param paymentMethod The payment method to use (RAZORPAY, etc.)
 * @return ResponseEntity with new payment details or error
 */
@PostMapping("/retry")
public ResponseEntity<?> retryPayment(
        @RequestParam Long orderId,
        @RequestParam String paymentMethod) {
    
    logger.info("Payment retry request for order ID: {}", orderId);
    
    try {
        Payment payment = paymentService.retryPayment(orderId, paymentMethod);
        
        if (payment.getPaymentType() == PaymentType.RAZORPAY) {
            Map<String, String> paymentDetails = paymentService.getPaymentDetails(orderId);
            return ResponseEntity.ok(paymentDetails);
        }
        
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "Payment retry initiated",
            "order_id", orderId.toString(),
            "payment_method", paymentMethod
        ));
    } catch (Exception e) {
        logger.error("Payment retry failed for order {}: {}", orderId, e.getMessage(), e);
        return ResponseEntity.badRequest().body(Map.of(
            "status", "error",
            "message", e.getMessage(),
            "order_id", orderId.toString()
        ));
    }
}

/**
 * Cleanup abandoned payments (Admin only)
 * @param daysThreshold Number of days to consider a payment abandoned
 * @return ResponseEntity with cleanup results
 */
@PostMapping("/cleanup")
public ResponseEntity<Map<String, Object>> cleanupAbandonedPayments(
        @RequestParam(defaultValue = "1") int daysThreshold) {
    
    logger.info("Cleaning up abandoned payments older than {} days", daysThreshold);
    
    try {
        paymentService.cleanupAbandonedPayments(daysThreshold);
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "Abandoned payments cleanup completed",
            "days_threshold", daysThreshold
        ));
    } catch (Exception e) {
        logger.error("Failed to cleanup abandoned payments: {}", e.getMessage(), e);
        return ResponseEntity.internalServerError().body(Map.of(
            "status", "error",
            "message", e.getMessage(),
            "days_threshold", daysThreshold
        ));
    }
}

}