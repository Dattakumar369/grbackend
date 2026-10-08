package com.example.service;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.entity.Address;
import com.example.entity.DeliveryPerson;
import com.example.entity.Order;
import com.example.entity.User;
import com.example.status.OrderStatus;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a");
    private static final NumberFormat CURRENCY_FORMATTER = NumberFormat.getCurrencyInstance(Locale.US);

    private final JavaMailSender mailSender;
    
    @Value("${spring.mail.username}")
    private String fromEmail;
    
    @Value("${app.base-url}")
    private String baseUrl;

    @Autowired
    public NotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Sends order creation notification to user
     */
    @Transactional
    public void sendOrderCreatedNotification(User user, Order order) {
        String subject = "Your Order #" + order.getId() + " Has Been Received";
        String content = buildOrderCreatedContent(user, order);
        sendEmail(user.getEmail(), subject, content);
    }

    /**
     * Sends order processing notification to user
     */
    @Transactional
    public void sendOrderProcessingNotification(User user, Order order) {
        String subject = "Your Order #" + order.getId() + " Is Being Processed";
        String content = buildOrderProcessingContent(user, order);
        sendEmail(user.getEmail(), subject, content);
    }

    /**
     * Sends payment failed notification to user
     */
    @Transactional
    public void sendPaymentFailedNotification(User user, Order order) {
        String subject = "Payment Failed for Order #" + order.getId();
        String content = buildPaymentFailedContent(user, order);
        sendEmail(user.getEmail(), subject, content);
    }

    /**
     * Sends order status update notification to user
     */
    @Transactional
    public void sendOrderStatusUpdateNotification(User user, Order order, OrderStatus previousStatus) {
        String subject = "Order #" + order.getId() + " Status Updated";
        String content = buildStatusUpdateContent(user, order, previousStatus);
        sendEmail(user.getEmail(), subject, content);
    }

    /**
     * Sends delivery assignment notification to delivery person
     */
    @Transactional
    public void sendDeliveryAssignmentNotification(DeliveryPerson deliveryPerson, Order order) {
        String subject = "New Delivery Assignment - Order #" + order.getId();
        String content = buildDeliveryAssignmentContent(deliveryPerson, order);
        sendEmail(deliveryPerson.getEmail(), subject, content);
    }

    /**
     * Sends order accepted notification to user
     */
    @Transactional
    public void sendOrderAcceptedNotification(User user, Order order) {
        String subject = "Order #" + order.getId() + " Accepted by Delivery";
        String content = buildOrderAcceptedContent(user, order);
        sendEmail(user.getEmail(), subject, content);
    }

    /**
     * Sends order delivered notification to user
     */
    @Transactional
    public void sendOrderDeliveredNotification(User user, Order order) {
        String subject = "Your Order #" + order.getId() + " Has Been Delivered";
        String content = buildOrderDeliveredContent(user, order);
        sendEmail(user.getEmail(), subject, content);
    }

    /**
     * Generic email sending method
     */
    private void sendEmail(String to, String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true); // true indicates HTML
            
            mailSender.send(message);
            logger.info("Email notification sent to {}", to);
        } catch (MessagingException e) {
            logger.error("Failed to send email to {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to send email notification", e);
        }
    }

    // ================== Email Content Builders ================== //

    private String buildOrderCreatedContent(User user, Order order) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family: Arial, sans-serif; line-height: 1.6;'>");
        sb.append("<div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 5px;'>");
        sb.append("<h2 style='color: #333;'>Thank you for your order!</h2>");
        sb.append("<p>Hello ").append(user.getFirstName()).append(",</p>");
        sb.append("<p>We've received your order #").append(order.getId()).append(" and it's being processed.</p>");
        
        sb.append("<h3 style='margin-top: 20px;'>Order Summary</h3>");
        sb.append("<table style='width: 100%; border-collapse: collapse;'>");
        sb.append("<tr><td style='padding: 8px; border-bottom: 1px solid #ddd;'><strong>Order Number:</strong></td>")
          .append("<td style='padding: 8px; border-bottom: 1px solid #ddd;'>").append(order.getId()).append("</td></tr>");
        sb.append("<tr><td style='padding: 8px; border-bottom: 1px solid #ddd;'><strong>Order Date:</strong></td>")
          .append("<td style='padding: 8px; border-bottom: 1px solid #ddd;'>")
          .append(order.getCreatedAt().format(DATE_FORMATTER)).append("</td></tr>");
        sb.append("<tr><td style='padding: 8px; border-bottom: 1px solid #ddd;'><strong>Total Amount:</strong></td>")
          .append("<td style='padding: 8px; border-bottom: 1px solid #ddd;'>")
          .append(CURRENCY_FORMATTER.format(order.getTotalAmount())).append("</td></tr>");
        sb.append("</table>");
        
        sb.append("<p style='margin-top: 20px;'>You can view your order details at: ")
          .append("<a href='").append(baseUrl).append("/orders/").append(order.getId()).append("'>")
          .append(baseUrl).append("/orders/").append(order.getId()).append("</a></p>");
        
        sb.append("<p style='margin-top: 20px;'>We'll notify you when your order ships.</p>");
        sb.append("<p>Thank you for shopping with us!</p>");
        sb.append("<p><strong>The Store Team</strong></p>");
        sb.append("</div></body></html>");
        
        return sb.toString();
    }

    private String buildOrderProcessingContent(User user, Order order) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family: Arial, sans-serif; line-height: 1.6;'>");
        sb.append("<div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 5px;'>");
        sb.append("<h2 style='color: #333;'>Your order is being processed</h2>");
        sb.append("<p>Hello ").append(user.getFirstName()).append(",</p>");
        sb.append("<p>Your order #").append(order.getId()).append(" is now being processed.</p>");
        
        sb.append("<h3 style='margin-top: 20px;'>Next Steps</h3>");
        sb.append("<ul>");
        sb.append("<li>We're preparing your items for shipment</li>");
        sb.append("<li>You'll receive another notification when your order ships</li>");
        sb.append("<li>Expected delivery date: ").append(order.getCreatedAt().plusDays(3).format(DATE_FORMATTER)).append("</li>");
        sb.append("</ul>");
        
        sb.append("<p>You can track your order at: ")
          .append("<a href='").append(baseUrl).append("/orders/").append(order.getId()).append("'>")
          .append(baseUrl).append("/orders/").append(order.getId()).append("</a></p>");
        
        sb.append("<p>Thank you for your patience!</p>");
        sb.append("<p><strong>The Store Team</strong></p>");
        sb.append("</div></body></html>");
        
        return sb.toString();
    }

    private String buildPaymentFailedContent(User user, Order order) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family: Arial, sans-serif; line-height: 1.6;'>");
        sb.append("<div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 5px;'>");
        sb.append("<h2 style='color: #d9534f;'>Payment Failed for Order #").append(order.getId()).append("</h2>");
        sb.append("<p>Hello ").append(user.getFirstName()).append(",</p>");
        sb.append("<p>We were unable to process your payment for order #").append(order.getId()).append(".</p>");
        
        sb.append("<h3 style='margin-top: 20px;'>What to do next?</h3>");
        sb.append("<ul>");
        sb.append("<li>Please check your payment details and try again</li>");
        sb.append("<li>Contact your bank if you believe this is an error</li>");
        sb.append("<li>You can retry payment at: ")
          .append("<a href='").append(baseUrl).append("/orders/").append(order.getId()).append("/payment'>")
          .append("Retry Payment</a></li>");
        sb.append("</ul>");
        
        sb.append("<p>If we don't receive payment within 24 hours, your order will be cancelled.</p>");
        sb.append("<p>Need help? Contact our support team at support@example.com</p>");
        sb.append("<p><strong>The Store Team</strong></p>");
        sb.append("</div></body></html>");
        
        return sb.toString();
    }

    private String buildStatusUpdateContent(User user, Order order, OrderStatus previousStatus) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family: Arial, sans-serif; line-height: 1.6;'>");
        sb.append("<div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 5px;'>");
        sb.append("<h2 style='color: #333;'>Order Status Update</h2>");
        sb.append("<p>Hello ").append(user.getFirstName()).append(",</p>");
        sb.append("<p>The status of your order #").append(order.getId()).append(" has changed:</p>");
        
        sb.append("<table style='width: 100%; border-collapse: collapse; margin: 15px 0;'>");
        sb.append("<tr><td style='padding: 8px; border-bottom: 1px solid #ddd; width: 120px;'><strong>Previous Status:</strong></td>")
          .append("<td style='padding: 8px; border-bottom: 1px solid #ddd;'>").append(previousStatus).append("</td></tr>");
        sb.append("<tr><td style='padding: 8px;'><strong>New Status:</strong></td>")
          .append("<td style='padding: 8px;'>").append(order.getStatus()).append("</td></tr>");
        sb.append("</table>");
        
        if (order.getStatus() == OrderStatus.SHIPPED) {
            sb.append("<p>Your order has been shipped and is on its way to you!</p>");
            if (order.getDeliveryPerson() != null) {
                sb.append("<p>Delivery Person: ").append(order.getDeliveryPerson().getFirstName()).append(" ")
                  .append(order.getDeliveryPerson().getLastName()).append("</p>");
            }
        }
        
        sb.append("<p>You can track your order at: ")
          .append("<a href='").append(baseUrl).append("/orders/").append(order.getId()).append("'>")
          .append(baseUrl).append("/orders/").append(order.getId()).append("</a></p>");
        
        sb.append("<p>Thank you for shopping with us!</p>");
        sb.append("<p><strong>The Store Team</strong></p>");
        sb.append("</div></body></html>");
        
        return sb.toString();
    }

    private String buildDeliveryAssignmentContent(DeliveryPerson deliveryPerson, Order order) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family: Arial, sans-serif; line-height: 1.6;'>");
        sb.append("<div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 5px;'>");
        sb.append("<h2 style='color: #333;'>New Delivery Assignment</h2>");
        sb.append("<p>Hello ").append(deliveryPerson.getFirstName()).append(",</p>");
        sb.append("<p>You have been assigned to deliver order #").append(order.getId()).append(".</p>");
        
        sb.append("<h3 style='margin-top: 20px;'>Order Details</h3>");
        sb.append("<table style='width: 100%; border-collapse: collapse;'>");
        sb.append("<tr><td style='padding: 8px; border-bottom: 1px solid #ddd;'><strong>Order Number:</strong></td>")
          .append("<td style='padding: 8px; border-bottom: 1px solid #ddd;'>").append(order.getId()).append("</td></tr>");
        sb.append("<tr><td style='padding: 8px; border-bottom: 1px solid #ddd;'><strong>Customer:</strong></td>")
          .append("<td style='padding: 8px; border-bottom: 1px solid #ddd;'>")
          .append(order.getUser().getFirstName()).append(" ").append(order.getUser().getLastName()).append("</td></tr>");
        sb.append("<tr><td style='padding: 8px; border-bottom: 1px solid #ddd;'><strong>Delivery Address:</strong></td>")
          .append("<td style='padding: 8px; border-bottom: 1px solid #ddd;'>")
          .append(formatAddress(order.getShippingAddress())).append("</td></tr>");
        sb.append("<tr><td style='padding: 8px; border-bottom: 1px solid #ddd;'><strong>Items:</strong></td>")
          .append("<td style='padding: 8px; border-bottom: 1px solid #ddd;'>").append(order.getItems().size()).append("</td></tr>");
        sb.append("<tr><td style='padding: 8px;'><strong>Total Amount:</strong></td>")
          .append("<td style='padding: 8px;'>").append(CURRENCY_FORMATTER.format(order.getTotalAmount())).append("</td></tr>");
        sb.append("</table>");
        
        sb.append("<h3 style='margin-top: 20px;'>Actions Required</h3>");
        sb.append("<ul>");
        sb.append("<li>Please accept or reject this assignment within 1 hour</li>");
        sb.append("<li>You can view order details at: ")
          .append("<a href='").append(baseUrl).append("/delivery/orders/").append(order.getId()).append("'>")
          .append(baseUrl).append("/delivery/orders/").append(order.getId()).append("</a></li>");
        sb.append("</ul>");
        
        sb.append("<p>Thank you for your service!</p>");
        sb.append("<p><strong>The Store Team</strong></p>");
        sb.append("</div></body></html>");
        
        return sb.toString();
    }

    private String buildOrderAcceptedContent(User user, Order order) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family: Arial, sans-serif; line-height: 1.6;'>");
        sb.append("<div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 5px;'>");
        sb.append("<h2 style='color: #333;'>Your Order Has Been Accepted</h2>");
        sb.append("<p>Hello ").append(user.getFirstName()).append(",</p>");
        sb.append("<p>Your order #").append(order.getId()).append(" has been accepted by our delivery team.</p>");
        
        if (order.getDeliveryPerson() != null) {
            sb.append("<p>Your delivery person is: <strong>")
              .append(order.getDeliveryPerson().getFirstName()).append(" ")
              .append(order.getDeliveryPerson().getLastName()).append("</strong></p>");
        }
        
        sb.append("<h3 style='margin-top: 20px;'>Estimated Delivery</h3>");
        sb.append("<p>").append(order.getCreatedAt().plusDays(2).format(DATE_FORMATTER)).append("</p>");
        
        sb.append("<p>You can track your order at: ")
          .append("<a href='").append(baseUrl).append("/orders/").append(order.getId()).append("'>")
          .append(baseUrl).append("/orders/").append(order.getId()).append("</a></p>");
        
        sb.append("<p>Thank you for shopping with us!</p>");
        sb.append("<p><strong>The Store Team</strong></p>");
        sb.append("</div></body></html>");
        
        return sb.toString();
    }

    private String buildOrderDeliveredContent(User user, Order order) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family: Arial, sans-serif; line-height: 1.6;'>");
        sb.append("<div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 5px;'>");
        sb.append("<h2 style='color: #5cb85c;'>Your Order Has Been Delivered</h2>");
        sb.append("<p>Hello ").append(user.getFirstName()).append(",</p>");
        sb.append("<p>We're happy to inform you that your order #").append(order.getId()).append(" has been successfully delivered.</p>");
        
        sb.append("<h3 style='margin-top: 20px;'>Delivery Details</h3>");
        sb.append("<table style='width: 100%; border-collapse: collapse;'>");
        sb.append("<tr><td style='padding: 8px; border-bottom: 1px solid #ddd;'><strong>Delivered On:</strong></td>")
          .append("<td style='padding: 8px; border-bottom: 1px solid #ddd;'>")
          .append(order.getUpdatedAt().format(DATE_FORMATTER)).append("</td></tr>");
        if (order.getDeliveryPerson() != null) {
            sb.append("<tr><td style='padding: 8px; border-bottom: 1px solid #ddd;'><strong>Delivered By:</strong></td>")
              .append("<td style='padding: 8px; border-bottom: 1px solid #ddd;'>")
              .append(order.getDeliveryPerson().getFirstName()).append(" ")
              .append(order.getDeliveryPerson().getLastName()).append("</td></tr>");
        }
        sb.append("<tr><td style='padding: 8px;'><strong>Delivery Address:</strong></td>")
          .append("<td style='padding: 8px;'>").append(formatAddress(order.getShippingAddress())).append("</td></tr>");
        sb.append("</table>");
        
        sb.append("<h3 style='margin-top: 20px;'>What's Next?</h3>");
        sb.append("<p>We hope you're satisfied with your purchase. If you have any questions or need to return an item, please contact our support team.</p>");
        
        sb.append("<p>You can view your order details at: ")
          .append("<a href='").append(baseUrl).append("/orders/").append(order.getId()).append("'>")
          .append(baseUrl).append("/orders/").append(order.getId()).append("</a></p>");
        
        sb.append("<p>Thank you for shopping with us!</p>");
        sb.append("<p><strong>The Store Team</strong></p>");
        sb.append("</div></body></html>");
        
        return sb.toString();
    }

    private String formatAddress(Address address) {
        return String.format("%s, %s, %s, %s %s, %s",
            address.getStreet(),
            address.getCity(),
            address.getState(),
            address.getPostalCode(),
            address.getCountry(),
              "");
    }
    
    
    
    
    /**
     * Sends order cancelled notification to user
     */
    @Transactional
    public void sendOrderCancelledNotification(User user, Order order) {
        String subject = "Your Order #" + order.getId() + " Has Been Cancelled";
        String content = buildOrderCancelledContent(user, order);
        sendEmail(user.getEmail(), subject, content);
    }

    private String buildOrderCancelledContent(User user, Order order) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family: Arial, sans-serif; line-height: 1.6;'>");
        sb.append("<div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 5px;'>");
        sb.append("<h2 style='color: #d9534f;'>Order #").append(order.getId()).append(" Cancelled</h2>");
        sb.append("<p>Hello ").append(user.getFirstName()).append(",</p>");
        sb.append("<p>Your order #").append(order.getId()).append(" has been successfully cancelled.</p>");
        
        sb.append("<h3 style='margin-top: 20px;'>Order Details</h3>");
        sb.append("<table style='width: 100%; border-collapse: collapse;'>");
        sb.append("<tr><td style='padding: 8px; border-bottom: 1px solid #ddd;'><strong>Order Number:</strong></td>")
          .append("<td style='padding: 8px; border-bottom: 1px solid #ddd;'>").append(order.getId()).append("</td></tr>");
        sb.append("<tr><td style='padding: 8px; border-bottom: 1px solid #ddd;'><strong>Order Date:</strong></td>")
          .append("<td style='padding: 8px; border-bottom: 1px solid #ddd;'>")
          .append(order.getCreatedAt().format(DATE_FORMATTER)).append("</td></tr>");
        sb.append("<tr><td style='padding: 8px; border-bottom: 1px solid #ddd;'><strong>Cancellation Date:</strong></td>")
          .append("<td style='padding: 8px; border-bottom: 1px solid #ddd;'>")
          .append(order.getUpdatedAt().format(DATE_FORMATTER)).append("</td></tr>");
        sb.append("<tr><td style='padding: 8px;'><strong>Total Amount:</strong></td>")
          .append("<td style='padding: 8px;'>").append(CURRENCY_FORMATTER.format(order.getTotalAmount())).append("</td></tr>");
        sb.append("</table>");
        
        sb.append("<h3 style='margin-top: 20px;'>Refund Information</h3>");
        sb.append("<p>If you were charged for this order, your refund will be processed within 5-7 business days.</p>");
        
        sb.append("<p>You can view your order history at: ")
          .append("<a href='").append(baseUrl).append("/orders/history").append("'>")
          .append(baseUrl).append("/orders/history").append("</a></p>");
        
        sb.append("<p>If you didn't request this cancellation or have any questions, please contact our support team.</p>");
        sb.append("<p><strong>The Store Team</strong></p>");
        sb.append("</div></body></html>");
        
        return sb.toString();
    }
}