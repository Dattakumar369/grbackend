package com.example.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.dao.CartRepository;
import com.example.dao.OrderRepository;
import com.example.dao.PaymentRepository;
import com.example.entity.Order;
import com.example.entity.Payment;
import com.example.exception.PaymentProcessingException;
import com.example.exception.ResourceNotFoundException;
import com.example.status.OrderStatus;
import com.example.status.PaymentStatus;
import com.example.status.PaymentType;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;

@Service
public class PaymentService {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);
    @Autowired
    @Lazy  // Add this annotation
    private OrderService orderService;
    private final RazorpayClient razorpayClient;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final String razorpayKeySecret;
    private final String razorpayKeyId;
    @Autowired
    private CartRepository cartRepository;
    public PaymentService(RazorpayClient razorpayClient,
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            
            @Value("${razorpay.key.id}") String razorpayKeyId,  
            @Value("${razorpay.key.secret}") String razorpayKeySecret) {
this.razorpayClient = razorpayClient;
this.paymentRepository = paymentRepository;
this.orderRepository = orderRepository;
this.razorpayKeyId = razorpayKeyId;  // Initialize the field
this.razorpayKeySecret = razorpayKeySecret;
 
}
@Transactional
public Payment createPayment(Order order, String paymentMethod, BigDecimal amount) {
    logger.info("Creating payment for order ID: {}, method: {}, amount: {}", 
        order.getId(), paymentMethod, amount);
    
    Payment payment = new Payment();
    payment.setOrder(order);
    payment.setAmount(amount);
    
    try {
        // Convert payment method to enum (case-insensitive)
        PaymentType type = PaymentType.valueOf(paymentMethod.toUpperCase());
        payment.setPaymentType(type);
        logger.debug("Set payment type to: {}", type);
    } catch (IllegalArgumentException e) {
        logger.error("Invalid payment method: {}", paymentMethod);
        throw new PaymentProcessingException("Invalid payment method: " + paymentMethod);
    }
    
    payment.setStatus(PaymentStatus.PENDING);
    payment.setTransactionId(UUID.randomUUID().toString());
    logger.debug("Set initial payment status to PENDING");
    
    if (payment.getPaymentType() == PaymentType.RAZORPAY) {
        try {
            JSONObject options = new JSONObject();
            options.put("amount", amount.multiply(BigDecimal.valueOf(100)).longValue());
            options.put("currency", "USD");
            options.put("receipt", "order_" + order.getId());
            options.put("payment_capture", 1);
            
            logger.info("Creating Razorpay order for amount: {}", amount);
            com.razorpay.Order razorpayOrder = razorpayClient.orders.create(options);
            
            payment.setGatewayOrderId(razorpayOrder.get("id"));
            payment.setGatewayResponse(razorpayOrder.toString());
            
         } catch (RazorpayException e) {
            logger.error("Razorpay order creation failed", e);
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new PaymentProcessingException("Failed to create Razorpay order: " + e.getMessage());
        }
    }
    
    Payment savedPayment = paymentRepository.save(payment);
    logger.info("Payment created successfully with ID: {}", savedPayment.getId());
    return savedPayment;
}    


@Transactional
public void verifyAndCompletePayment(String razorpayPaymentId, String razorpayOrderId, String razorpaySignature) {
    logger.info("Starting payment verification. PaymentID: {}, OrderID: {}", 
        razorpayPaymentId, razorpayOrderId);
    
    Payment payment = paymentRepository.findByGatewayOrderId(razorpayOrderId)
            .orElseThrow(() -> {
                logger.error("Payment not found for Razorpay order ID: {}", razorpayOrderId);
                return new PaymentProcessingException("Payment not found for order: " + razorpayOrderId);
            });
    
    try {
        // Verify the payment signature
        JSONObject attributes = new JSONObject();
        attributes.put("razorpay_order_id", razorpayOrderId);
        attributes.put("razorpay_payment_id", razorpayPaymentId);
        attributes.put("razorpay_signature", razorpaySignature);
        
        logger.debug("Verifying payment signature...");
        boolean isValidSignature = Utils.verifyPaymentSignature(attributes, razorpayKeySecret);
        
        if (!isValidSignature) {
            logger.error("Invalid signature for payment: {}", razorpayPaymentId);
            throw new PaymentProcessingException("Invalid payment signature");
        }
        
        // Update payment details
        payment.setGatewayPaymentId(razorpayPaymentId);
        payment.setGatewaySignature(razorpaySignature);
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setUpdatedAt(LocalDateTime.now());
        
        Order order = payment.getOrder();
        order.setStatus(OrderStatus.PROCESSING);
        
        paymentRepository.save(payment);
        orderRepository.save(order);
        
        // NOW clear the cart since payment is successful
        cartRepository.deleteByUserId(order.getUser().getId());
        
        // Record history and update quantities
        orderService.recordOrderHistory(order, OrderStatus.PROCESSING, 
            "Payment verified and order processing started");        
        orderService.updateProductQuantities(order);
        
        logger.info("Payment and order updated successfully. Payment ID: {}, Order ID: {}", 
            payment.getId(), order.getId());
        
    } catch (Exception e) {
        logger.error("Payment verification failed", e);
        payment.setStatus(PaymentStatus.FAILED);
        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(payment);
        
        throw new PaymentProcessingException("Payment verification failed: " + e.getMessage(), e);
    }
} 
public Map<String, String> getPaymentDetails(Long orderId) {
        Payment payment = paymentRepository.findByOrder_Id(orderId)
                .orElseThrow(() -> new PaymentProcessingException("Payment not found"));
        
        return Map.of(
            "order_id", payment.getGatewayOrderId(),
            "amount", payment.getAmount().toString(),
            "currency", "USD",
            "key", razorpayKeyId,
            "name", "Your Company Name",
            "description", "Order Payment"
        );
    }
    
    public String checkPaymentStatus(Long orderId) {
        Payment payment = paymentRepository.findByOrder_Id(orderId)
                .orElseThrow(() -> new PaymentProcessingException("Payment not found"));
        
        return payment.getStatus().toString();
    }
    
@Transactional
public void handleWebhookEvent(Map<String, Object> webhookData, String razorpaySignature) {
    logger.info("Received webhook event: {}", webhookData);
    
    try {
        // Verify webhook signature
        String webhookBody = webhookData.toString();
        boolean isValidSignature = Utils.verifyWebhookSignature(
            webhookBody, 
            razorpaySignature, 
            razorpayKeySecret
        );
        
        if (!isValidSignature) {
            logger.error("Invalid webhook signature");
            throw new PaymentProcessingException("Invalid webhook signature");
        }
        
        String event = (String) webhookData.get("event");
        logger.info("Processing webhook event: {}", event);
        
        if ("payment.captured".equals(event)) {
            Map<String, Object> payload = (Map<String, Object>) webhookData.get("payload");
            Map<String, Object> paymentEntity = (Map<String, Object>) payload.get("payment.entity");
            
            String paymentId = (String) paymentEntity.get("id");
            String orderId = (String) paymentEntity.get("order_id");
            String signature = (String) paymentEntity.get("signature");
            
            logger.info("Processing payment.captured event. PaymentID: {}, OrderID: {}", 
                paymentId, orderId);
            
            verifyAndCompletePayment(paymentId, orderId, signature);
        } else {
            logger.info("Ignoring non-payment webhook event: {}", event);
        }
    } catch (Exception e) {
        logger.error("Webhook processing failed", e);
        throw new PaymentProcessingException("Webhook processing failed: " + e.getMessage(), e);
    }
}

@Transactional
public Payment retryPayment(Long orderId, String paymentMethod) {
    logger.info("Retrying payment for order ID: {}", orderId);
    
    Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    
    // Check if order is eligible for payment retry
    if (order.getStatus() != OrderStatus.PAYMENT_PENDING && 
        order.getStatus() != OrderStatus.PAYMENT_FAILED) {
        throw new PaymentProcessingException("Order is not in a state that allows payment retry");
    }
    
    // Find existing payment or create new one
    Payment payment = paymentRepository.findByOrder_Id(orderId)
            .orElseGet(() -> {
                Payment newPayment = new Payment();
                newPayment.setOrder(order);
                newPayment.setAmount(order.getTotalAmount());
                return newPayment;
            });
    
    // Reset payment details
    payment.setStatus(PaymentStatus.PENDING);
    payment.setTransactionId(UUID.randomUUID().toString());
    payment.setUpdatedAt(LocalDateTime.now());
    
    try {
        PaymentType type = PaymentType.valueOf(paymentMethod.toUpperCase());
        payment.setPaymentType(type);
    } catch (IllegalArgumentException e) {
        throw new PaymentProcessingException("Invalid payment method: " + paymentMethod);
    }
    
    // Handle Razorpay specific flow
    if (payment.getPaymentType() == PaymentType.RAZORPAY) {
        try {
            JSONObject options = new JSONObject();
            options.put("amount", order.getTotalAmount().multiply(BigDecimal.valueOf(100)).longValue());
            options.put("currency", "USD");
            options.put("receipt", "order_" + order.getId());
            options.put("payment_capture", 1);
            
            com.razorpay.Order razorpayOrder = razorpayClient.orders.create(options);
            payment.setGatewayOrderId(razorpayOrder.get("id"));
            payment.setGatewayResponse(razorpayOrder.toString());
        } catch (RazorpayException e) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new PaymentProcessingException("Failed to create Razorpay order: " + e.getMessage());
        }
    }
    
    return paymentRepository.save(payment);
}

@Transactional
public void cleanupAbandonedPayments(int daysThreshold) {
    logger.info("Cleaning up abandoned payments older than {} days", daysThreshold);
    
    LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysThreshold);
    List<Payment> abandonedPayments = paymentRepository.findAbandonedPayments(
            PaymentStatus.PENDING, cutoffDate);
    
    for (Payment payment : abandonedPayments) {
        logger.info("Cleaning up abandoned payment ID: {} for order ID: {}", 
            payment.getId(), payment.getOrder().getId());
        
        Order order = payment.getOrder();
        if (order.getStatus() == OrderStatus.PAYMENT_PENDING) {
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
            
            orderService.recordOrderHistory(
                order, 
                OrderStatus.CANCELLED, 
                "Order cancelled due to abandoned payment"
            );
        }
        
        payment.setStatus(PaymentStatus.ABANDONED);
        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(payment);
    }
    
    logger.info("Cleaned up {} abandoned payments", abandonedPayments.size());
}


}







