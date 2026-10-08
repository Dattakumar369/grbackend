package com.example.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.dao.CartRepository;
import com.example.dao.DeliveryPersonRepository;
import com.example.dao.OrderHistoryRepository;
import com.example.dao.OrderItemRepository;
import com.example.dao.OrderRepository;
import com.example.dao.ProductRepository;
import com.example.dao.UserRepository;
import com.example.dto.CartSummaryDTO;
import com.example.dto.OrderDTO;
import com.example.dto.OrderMapper;
import com.example.dto.ShippingResponse;
import com.example.dto.ShippingResponseDTO;
import com.example.entity.Address;
import com.example.entity.Cart;
import com.example.entity.DeliveryPerson;
import com.example.entity.Order;
import com.example.entity.OrderHistory;
import com.example.entity.OrderItem;
import com.example.entity.Product;
import com.example.entity.User;
import com.example.entity.UserAddress;
import com.example.exception.BusinessException;
import com.example.exception.ResourceNotFoundException;
import com.example.status.OrderStatus;

@Service
public class OrderService {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
    
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final DeliveryPersonRepository deliveryPersonRepository;
    private final OrderHistoryRepository orderHistoryRepository;
    private final NotificationService notificationService;
    private final PaymentService paymentService;
    private final UserAddressService userAddressService;
    private final OrderMapper orderMapper;

    private final S3Service  s3Service;
    private final CartService cartService;

    private final TrackingNumberService trackingNumberService;

    public OrderService(OrderRepository orderRepository,
                      OrderItemRepository orderItemRepository,
                      CartRepository cartRepository,
                      ProductRepository productRepository,
                      UserRepository userRepository,
                      DeliveryPersonRepository deliveryPersonRepository,
                      OrderHistoryRepository orderHistoryRepository,
                      NotificationService notificationService,
                      PaymentService paymentService,
                      UserAddressService userAddressService,
                      OrderMapper orderMapper,
                      S3Service s3Service,
                      CartService cartService,
                      TrackingNumberService trackingNumberService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.deliveryPersonRepository = deliveryPersonRepository;
        this.orderHistoryRepository = orderHistoryRepository;
        this.notificationService = notificationService;
        this.paymentService = paymentService;
        this.userAddressService= userAddressService;
        this.orderMapper = orderMapper;
        this.s3Service = s3Service;;
        this.cartService = cartService;
this.trackingNumberService =trackingNumberService;

    }

    // ================== Public API Methods ================== //
    
    /**
     * Creates a new order from user's cart items
     * @param userId The ID of the user placing the order
     * @param shippingAddress The shipping address for the order
     * @return The created order
     * @throws ResourceNotFoundException if user not found
     * @throws BusinessException if cart is empty or address is invalid
     */
@Transactional
public Order createOrder(Long userId, Address shippingAddress, boolean saveAddress, String addressLabel) {
    logger.info("Creating order for user: {}", userId);
    
    try {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        if (saveAddress) {
            boolean makeDefault = userAddressService.getUserAddresses(userId).isEmpty();
            userAddressService.addAddress(userId, shippingAddress, addressLabel, makeDefault);
        }
        
        CartSummaryDTO cartSummary = cartService.getCartSummaryByUserId(userId);
        List<Cart> cartItems = cartRepository.findByUserId(userId);
        
        if (cartItems.isEmpty()) {
            logger.warn("Attempt to create order with empty cart for user: {}", userId);
            throw new BusinessException("Cannot create order - cart is empty");
        }
        
        validateAddress(shippingAddress);
        BigDecimal totalAmount = cartSummary.getGrandTotal();
        
        Order order = buildOrder(user, shippingAddress, totalAmount);
        Order savedOrder = orderRepository.save(order);
        
        createOrderItems(cartItems, savedOrder);
        paymentService.createPayment(savedOrder, "RAZORPAY", totalAmount);
        
        // DON'T clear cart here - moved to payment verification
        
        recordOrderHistory(savedOrder, savedOrder.getStatus(), "Order created with RAZORPAY");
        notificationService.sendOrderCreatedNotification(user, savedOrder);
        
        logger.info("Order created successfully. Order ID: {}", savedOrder.getId());
        return savedOrder;
    } catch (Exception e) {
        logger.error("Failed to create order for user: {}. Error: {}", userId, e.getMessage(), e);
        throw e;
    }
}

@Transactional
public Order createOrderWithSavedAddress(Long userId, Long addressId) {
    logger.info("Creating order for user: {} with saved address: {}", userId, addressId);
    
    UserAddress userAddress = userAddressService.getAddressForUser(userId, addressId);
    CartSummaryDTO cartSummary = cartService.getCartSummaryByUserId(userId);
    List<Cart> cartItems = cartRepository.findByUserId(userId);
    
    if (cartItems.isEmpty()) {
        throw new BusinessException("Cannot create order - cart is empty");
    }
    
    validateAddress(userAddress.getAddress());
    BigDecimal totalAmount = cartSummary.getGrandTotal();
    
    Order order = buildOrder(userAddress.getUser(), userAddress.getAddress(), totalAmount);
    Order savedOrder = orderRepository.save(order);
    
    createOrderItems(cartItems, savedOrder);
    paymentService.createPayment(savedOrder, "RAZORPAY", totalAmount);
    
    // Don't clear cart here - will be done after payment verification
    
    recordOrderHistory(savedOrder, savedOrder.getStatus(), "Order created with RAZORPAY");
    notificationService.sendOrderCreatedNotification(userAddress.getUser(), savedOrder);
    
    return savedOrder;
}
/**
     * Processes an order after payment completion
     * @param orderId The ID of the order to process
     * @throws ResourceNotFoundException if order not found
     * @throws BusinessException if order cannot be processed
     */
    @Transactional
    public void processOrder(Long orderId) {
        logger.info("Processing order: {}", orderId);
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        
        validateOrderForProcessing(order);
        
        order.setStatus(OrderStatus.PROCESSING);
        orderRepository.save(order);
        
        recordOrderHistory(order, order.getStatus(), "Order processing started");
        notificationService.sendOrderProcessingNotification(order.getUser(), order);
        
        assignDeliveryPerson(order);
        logger.info("Order {} processing completed", orderId);
    }
    
    
    
    @Transactional
    public void cancelOrder(Long orderId) {
        logger.info("Cancelling order: {}", orderId);
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        
        if (order.getStatus() == OrderStatus.DELIVERED || 
            order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException("Order cannot be cancelled in current state");
        }
        
        // Restore product quantities if order was already processed
        if (order.getStatus() == OrderStatus.PROCESSING || 
            order.getStatus() == OrderStatus.ACCEPTED_BY_DELIVERY ||
            order.getStatus() == OrderStatus.SHIPPED) {
            restoreProductQuantities(order);
        }
        
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        
        recordOrderHistory(order, order.getStatus(), "Order cancelled by user");
        notificationService.sendOrderCancelledNotification(order.getUser(), order);
        
        logger.info("Order {} cancelled successfully", orderId);
    }

    private void restoreProductQuantities(Order order) {
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setQuantity(product.getQuantity() + item.getQuantity());
            productRepository.save(product);
            
            logger.debug("Restored quantity for product {} (ID: {}). New quantity: {}", 
                product.getName(), product.getId(), product.getQuantity());
        }
    }
    /**
     * Updates order status with validation of status transition
     * @param orderId The order ID
     * @param newStatus The new status to set
     * @param notes Additional notes about the status change
     * @throws ResourceNotFoundException if order not found
     * @throws BusinessException if invalid status transition
     */
@Transactional
public void updateOrderStatus(Long orderId, OrderStatus newStatus, String notes, MultipartFile deliveryProofImage) 
        throws IOException {
    logger.info("Updating status for order: {} to {}", orderId, newStatus);
    
    Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    
    OrderStatus oldStatus = order.getStatus();
    validateStatusTransition(oldStatus, newStatus);
    
    // Handle delivery proof for DELIVERED status
    if (newStatus == OrderStatus.DELIVERED) {
        if (deliveryProofImage == null || deliveryProofImage.isEmpty()) {
            throw new BusinessException("Delivery proof image is required for DELIVERED status");
        }
        
        // Create folder path for order delivery proofs
        String folderPath = String.format("orders/%d/delivery-proofs/", orderId);
        String imageKey = s3Service.uploadFile(deliveryProofImage, folderPath);
        
        // Delete old proof if exists
        if (order.getDeliveryProofImageKey() != null) {
            s3Service.deleteFile(order.getDeliveryProofImageKey());
        }
        
        order.setDeliveryProofImageKey(imageKey);
        notes = notes != null ? 
            notes + " | Delivery proof attached" : 
            "Delivery proof attached";
    }
    
    order.setStatus(newStatus);
    orderRepository.save(order);
    
    recordOrderHistory(order, newStatus, notes);
    handleStatusSpecificActions(order, oldStatus, newStatus);
    
    logger.debug("Status updated for order: {} from {} to {}", orderId, oldStatus, newStatus);
}    
    /**
     * Completes payment for an order and starts processing
     * @param orderId The order ID
     * @throws ResourceNotFoundException if order not found
     * @throws BusinessException if order not in payment pending state
     */
@Transactional
public void completePayment(Long orderId) {
    logger.info("Completing payment for order: {}", orderId);
    
    Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    
    if (order.getStatus() != OrderStatus.PAYMENT_PENDING) {
        logger.warn("Attempt to complete payment for order {} in invalid state: {}", 
            orderId, order.getStatus());
        throw new BusinessException("Order is not in payment pending state");
    }
    
    // Update product quantities with additional validation
    updateProductQuantities(order);
    
    // Change status directly to PROCESSING instead of PAYMENT_COMPLETED
    order.setStatus(OrderStatus.PROCESSING);
    orderRepository.save(order);
    
    recordOrderHistory(order, order.getStatus(), "Payment completed and order processing started");
    
    // Assign delivery person immediately
    assignDeliveryPerson(order);
    
    logger.info("Payment completed for order: {}", orderId);
}

@Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processOrderInNewTransaction(Long orderId) {
        processOrder(orderId);
    }

    /**
     * Marks payment as failed for an order
     * @param orderId The order ID
     * @throws ResourceNotFoundException if order not found
     * @throws BusinessException if order not in payment pending state
     */
    @Transactional
    public void failPayment(Long orderId) {
        logger.info("Marking payment as failed for order: {}", orderId);
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        
        if (order.getStatus() != OrderStatus.PAYMENT_PENDING) {
            logger.warn("Attempt to fail payment for order {} in invalid state: {}", 
                orderId, order.getStatus());
            throw new BusinessException("Order is not in payment pending state");
        }
        
        order.setStatus(OrderStatus.PAYMENT_FAILED);
        orderRepository.save(order);
        
        recordOrderHistory(order, order.getStatus(), "Payment failed");
        notificationService.sendPaymentFailedNotification(order.getUser(), order);
        
        logger.info("Payment failed for order: {}", orderId);
    }
    
    // ================== Delivery Person Methods ================== //
    
    /**
     * Gets available orders that delivery persons can accept
     * @return List of available orders
     */
    public List<Order> getAvailableOrdersForDelivery() {
        logger.debug("Fetching available orders for delivery persons");
        return orderRepository.findByStatusAndDeliveryPersonIsNull(OrderStatus.PROCESSING);
    }
    
    /**
     * Allows a delivery person to accept an order
     * @param orderId The order ID to accept
     * @param deliveryPersonId The delivery person ID
     * @throws ResourceNotFoundException if order or delivery person not found
     * @throws BusinessException if order cannot be accepted
     */
@Transactional
public void acceptOrderByDeliveryPerson(Long orderId, Long deliveryPersonId) {
    logger.info("Delivery person {} accepting order {}", deliveryPersonId, orderId);
    
    Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    
    DeliveryPerson deliveryPerson = deliveryPersonRepository.findById(deliveryPersonId)
            .orElseThrow(() -> new ResourceNotFoundException("Delivery person not found"));
    
    // No availability check needed
    
    order.setStatus(OrderStatus.ACCEPTED_BY_DELIVERY);
    order.setDeliveryPerson(deliveryPerson);
    orderRepository.save(order);
    
    recordOrderHistory(order, order.getStatus(), 
        "Order accepted by delivery person: " + deliveryPerson.getFirstName());
    
    notificationService.sendOrderStatusUpdateNotification(
        order.getUser(), order, OrderStatus.PROCESSING);
    
    logger.info("Order {} accepted by delivery person {}", orderId, deliveryPersonId);
}

    /**
     * Allows a delivery person to reject an order
     * @param orderId The order ID to reject
     * @param deliveryPersonId The delivery person ID
     * @param reason The reason for rejection
     * @throws ResourceNotFoundException if order or delivery person not found
     * @throws BusinessException if order cannot be rejected
     */
@Transactional
public void rejectOrderByDeliveryPerson(Long orderId, Long deliveryPersonId, String reason) {
    logger.info("Delivery person {} rejecting order {}", deliveryPersonId, orderId);
    
    Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    
    DeliveryPerson deliveryPerson = deliveryPersonRepository.findById(deliveryPersonId)
            .orElseThrow(() -> new ResourceNotFoundException("Delivery person not found"));
    
    if (order.getDeliveryPerson() == null || 
        !order.getDeliveryPerson().getId().equals(deliveryPersonId)) {
        logger.warn("Order {} not assigned to delivery person {}", orderId, deliveryPersonId);
        throw new BusinessException("This order is not assigned to you");
    }
    
    order.setStatus(OrderStatus.REJECTED_BY_DELIVERY);
    order.setDeliveryPerson(null);
    orderRepository.save(order);
    
    // No availability to update
    
    recordOrderHistory(order, order.getStatus(), 
        "Order rejected by delivery person. Reason: " + reason);
    
    notificationService.sendOrderStatusUpdateNotification(
        order.getUser(), order, OrderStatus.ACCEPTED_BY_DELIVERY);
    
    assignDeliveryPerson(order);
    logger.info("Order {} rejected by delivery person {}", orderId, deliveryPersonId);
}    
    
    // ================== Query Methods ================== //
    
    public Order getOrderById(Long orderId) {
        logger.debug("Fetching order by ID: {}", orderId);
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    }
    
    public List<Order> getOrdersByUser(Long userId) {
        logger.debug("Fetching orders for user: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return orderRepository.findByUser(user);
    }
    
    public List<OrderHistory> getOrderHistory(Long orderId) {
        logger.debug("Fetching order history for order: {}", orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return orderHistoryRepository.findByOrderOrderByCreatedAtDesc(order);
    }

    public List<Order> getOrdersByDeliveryPerson(Long deliveryPersonId) {
        logger.debug("Fetching orders for delivery person: {}", deliveryPersonId);
        DeliveryPerson deliveryPerson = deliveryPersonRepository.findById(deliveryPersonId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery person not found"));
        return orderRepository.findByDeliveryPerson(deliveryPerson);
    }

    public Order getOrderDetailsForUser(Long orderId, Long userId) {
        logger.debug("Fetching order {} details for user {}", orderId, userId);
        Order order = getOrderById(orderId);
        if (!order.getUser().getId().equals(userId)) {
            logger.warn("User {} unauthorized to access order {}", userId, orderId);
            throw new BusinessException("User is not authorized to access this order");
        }
        return order;
    }

    public List<OrderHistory> getAllOrderHistory() {  // Return entities, not DTOs
        logger.debug("Fetching all order history");
        return orderHistoryRepository.findAllByOrderByCreatedAtDesc();
    }

    // ================== Private Helper Methods ================== //
    
    @Transactional
    protected synchronized void assignDeliveryPerson(Order order) {
        logger.debug("Assigning delivery person for order: {}", order.getId());
        
        List<DeliveryPerson> availableDeliveryPersons = deliveryPersonRepository.findAllActiveDeliveryPersons();
        
        if (availableDeliveryPersons.isEmpty()) {
            logger.warn("No available delivery persons for order: {}", order.getId());
            recordOrderHistory(order, order.getStatus(), "No delivery persons available for assignment");
            return;
        }
        
        DeliveryPerson deliveryPerson = availableDeliveryPersons.get(0);
        
        order.setDeliveryPerson(deliveryPerson);
        order.setStatus(OrderStatus.ACCEPTED_BY_DELIVERY);
        orderRepository.save(order);
        
        deliveryPerson.setAvailable(false);
        deliveryPersonRepository.save(deliveryPerson);
        
        recordOrderHistory(order, order.getStatus(), 
            "Assigned to delivery person: " + deliveryPerson.getFirstName());
        
        notificationService.sendDeliveryAssignmentNotification(deliveryPerson, order);
        notificationService.sendOrderAcceptedNotification(order.getUser(), order);
        
        logger.info("Assigned delivery person {} to order {}", deliveryPerson.getId(), order.getId());
    }
    
    private void validateAddress(Address address) {
        if (address.getStreet() == null || address.getStreet().isBlank() ||
            address.getCity() == null || address.getCity().isBlank() ||
            address.getState() == null || address.getState().isBlank() ||
            address.getPostalCode() == null || address.getPostalCode().isBlank() ||
            address.getCountry() == null || address.getCountry().isBlank()) {
            logger.warn("Invalid address provided: {}", address);
            throw new BusinessException("All address fields are required");
        }
    }
    
    private BigDecimal calculateTotalAmount(List<Cart> cartItems) {
        return cartItems.stream()
                .map(cart -> cart.getProductPrice().multiply(BigDecimal.valueOf(cart.getProductQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    private Order buildOrder(User user, Address shippingAddress, BigDecimal totalAmount) {
        Order order = new Order();
        order.setUser(user);
        order.setTotalAmount(totalAmount);
        order.setShippingAddress(shippingAddress);
        order.setStatus(OrderStatus.PAYMENT_PENDING);
        return order;
    }
    
    private void createOrderItems(List<Cart> cartItems, Order order) {
        for (Cart cartItem : cartItems) {
            Product product = cartItem.getProduct();
            validateProductAvailability(product, cartItem.getProductQuantity());
            
            OrderItem orderItem = buildOrderItem(order, cartItem);
            orderItemRepository.save(orderItem);
        }
    }
    
    private OrderItem buildOrderItem(Order order, Cart cartItem) {
        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setProduct(cartItem.getProduct());
        orderItem.setQuantity(cartItem.getProductQuantity());
        orderItem.setPrice(cartItem.getProductPrice());
        orderItem.setTotalPrice(cartItem.getTotalPrice());
        return orderItem;
    }
    
    private void validateProductAvailability(Product product, Long requestedQuantity) {
        if (product.getQuantity() < requestedQuantity) {
            logger.warn("Insufficient stock for product {} (ID: {}). Available: {}, Requested: {}", 
                product.getName(), product.getId(), product.getQuantity(), requestedQuantity);
            throw new BusinessException(
                String.format("Insufficient stock for product %s. Available: %d, Requested: %d",
                    product.getName(), product.getQuantity(), requestedQuantity));
        }
    }
@Transactional
public void updateProductQuantities(Order order) {
    for (OrderItem item : order.getItems()) {
        Product product = item.getProduct();
        long requestedQuantity = item.getQuantity();
        long currentQuantity = product.getQuantity();
        
        if (currentQuantity < requestedQuantity) {
            throw new BusinessException(
                String.format("Insufficient stock for product %s. Available: %d, Requested: %d",
                    product.getName(), currentQuantity, requestedQuantity));
        }
        
        product.setQuantity(currentQuantity - requestedQuantity);
        productRepository.save(product);
        
        logger.debug("Updated quantity for product {} (ID: {}). New quantity: {}", 
            product.getName(), product.getId(), product.getQuantity());
    }
}    
    private void validateOrderForProcessing(Order order) {
        if (order.getStatus() != OrderStatus.PAYMENT_COMPLETED && 
            order.getStatus() != OrderStatus.PROCESSING) {
            logger.warn("Invalid order status for processing: {}", order.getStatus());
            throw new BusinessException("Order cannot be processed in current state");
        }
    }
    
    private void validateStatusTransition(OrderStatus current, OrderStatus newStatus) {
        if (!isValidStatusTransition(current, newStatus)) {
            logger.warn("Invalid status transition from {} to {}", current, newStatus);
            throw new BusinessException(
                String.format("Invalid status transition from %s to %s", 
                    current, newStatus));
        }
    }
    
    private boolean isValidStatusTransition(OrderStatus current, OrderStatus newStatus) {
        switch (current) {
        case PENDING: 
            return newStatus == OrderStatus.PAYMENT_PENDING || 
                   newStatus == OrderStatus.CANCELLED;
        case PAYMENT_PENDING: 
            return newStatus == OrderStatus.PROCESSING ||  // Direct to PROCESSING
                   newStatus == OrderStatus.PAYMENT_FAILED ||
                   newStatus == OrderStatus.CANCELLED;
        case PROCESSING: 
            return newStatus == OrderStatus.ACCEPTED_BY_DELIVERY || 
                   newStatus == OrderStatus.CANCELLED;       case ACCEPTED_BY_DELIVERY: return newStatus == OrderStatus.SHIPPED || 
                                           newStatus == OrderStatus.REJECTED_BY_DELIVERY;
            case SHIPPED: return newStatus == OrderStatus.OUT_FOR_DELIVERY;
            case OUT_FOR_DELIVERY: return newStatus == OrderStatus.DELIVERED;
            case REJECTED_BY_DELIVERY: return newStatus == OrderStatus.PROCESSING;
            default: return false;
        }
    }
    
    private void handleStatusSpecificActions(Order order, OrderStatus oldStatus, OrderStatus newStatus) {
        switch (newStatus) {
            case CANCELLED:
                if (oldStatus == OrderStatus.PROCESSING || 
                    oldStatus == OrderStatus.ACCEPTED_BY_DELIVERY ||
                    oldStatus == OrderStatus.SHIPPED) {
                    restoreProductQuantities(order);
                }
                break;
            case SHIPPED:
                // When order status is changed to SHIPPED, mark all items as shipped
                markAllItemsAsShipped(order);
                break;
            default:
                notificationService.sendOrderStatusUpdateNotification(
                    order.getUser(), order, oldStatus);
                break;
        }
    }
    
    /**
     * Marks all items in an order as shipped
     * @param order The order whose items should be marked as shipped
     */
    private void markAllItemsAsShipped(Order order) {
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            String trackingNumber = trackingNumberService.generateTrackingNumber(order.getId());
            
            for (OrderItem item : order.getItems()) {
                if (!item.isShipped()) {
                    item.setShipped(true);
                    item.setShippedAt(LocalDateTime.now());
                    item.setShippingTrackingNumber(trackingNumber);
                    orderItemRepository.save(item);
                    logger.info("Marked item {} as shipped with tracking {}", item.getId(), trackingNumber);
                }
            }
            
            logger.info("All items marked as shipped for order {}", order.getId());
        }
    }
    
    private void handleDeliveryRejection(Order order) {
        DeliveryPerson deliveryPerson = order.getDeliveryPerson();
        if (deliveryPerson != null) {
            deliveryPerson.setAvailable(true);
            deliveryPersonRepository.save(deliveryPerson);
            order.setDeliveryPerson(null);
            order.setStatus(OrderStatus.PROCESSING);
            orderRepository.save(order);
            
            recordOrderHistory(order, order.getStatus(), "Delivery rejected, reassigning");
            logger.info("Delivery rejected for order {}. Reassigning...", order.getId());
            assignDeliveryPerson(order);
        }
    }
    
    private void handleDeliveryCompletion(Order order) {
        notificationService.sendOrderDeliveredNotification(order.getUser(), order);
        logger.info("Order {} delivered successfully", order.getId());
    }
    
 // In OrderService.java
    @Transactional
    public void recordOrderHistory(Order order, OrderStatus status, String notes) {
        OrderHistory history = new OrderHistory();
        history.setOrder(order);
        history.setStatus(status);
        history.setNotes(notes);
        history.setDeliveryPerson(order.getDeliveryPerson());
        orderHistoryRepository.save(history);
        logger.debug("Recorded order history for order {}: {} - {}", 
            order.getId(), status, notes);
    }
    
    public long getTotalOrderCount() {
        return orderRepository.count();
    }

     
    public BigDecimal getTotalCompletedPaymentAmount() {
        return orderRepository.sumCompletedPayments();
    }
    public long getPendingPaymentOrderCount() {
        return orderRepository.countByStatus(OrderStatus.PAYMENT_PENDING);
    }

     
    public BigDecimal getPendingPaymentAmount() {
        return orderRepository.sumAmountByStatus(OrderStatus.PAYMENT_PENDING);
    }
    public List<OrderDTO> getPendingPaymentOrders() {
        List<Order> orders = orderRepository.findByStatus(OrderStatus.PAYMENT_PENDING);
        return orders.stream()
                .map(orderMapper::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get all orders that are not yet delivered (pending delivery)
     * Includes: PROCESSING, ACCEPTED_BY_DELIVERY, SHIPPED, OUT_FOR_DELIVERY
     * @return List of pending delivery orders
     */
    public List<Order> getPendingDeliveryOrders() {
        List<OrderStatus> pendingStatuses = Arrays.asList(
            OrderStatus.PROCESSING,
            OrderStatus.ACCEPTED_BY_DELIVERY,
            OrderStatus.SHIPPED,
            OrderStatus.OUT_FOR_DELIVERY
        );
        return orderRepository.findByStatusIn(pendingStatuses);
    }

    /**
     * Get all completed orders (DELIVERED status)
     * @return List of completed orders
     */
    public List<Order> getCompletedOrders() {
        return orderRepository.findByStatus(OrderStatus.DELIVERED);
    }

    
    
    
    @Transactional
    public Order prepareForPaymentRetry(Long orderId) {
        logger.info("Preparing order {} for payment retry", orderId);
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        
        if (order.getStatus() != OrderStatus.PAYMENT_PENDING && 
            order.getStatus() != OrderStatus.PAYMENT_FAILED) {
            throw new BusinessException("Order is not in a state that allows payment retry");
        }
        
        // Reset order status if it was PAYMENT_FAILED
        if (order.getStatus() == OrderStatus.PAYMENT_FAILED) {
            order.setStatus(OrderStatus.PAYMENT_PENDING);
            orderRepository.save(order);
            
            recordOrderHistory(
                order, 
                OrderStatus.PAYMENT_PENDING, 
                "Preparing for payment retry"
            );
        }
        
        return order;
    }
    
    
 
 // Add this dependency to OrderService constructor

    // Update the shipOrderItems method
@Transactional
public ShippingResponse shipOrderItems(Long orderId, List<Long> shippedItemIds, String notes) {
    logger.info("Shipping items for order: {} with item IDs: {}", orderId, shippedItemIds);
    
    Order order = orderRepository.findByIdWithItems(orderId) // Make sure to fetch items
            .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    
    logger.info("Order {} status: {}, items count: {}", orderId, order.getStatus(), 
        order.getItems() != null ? order.getItems().size() : 0);
    
    // Log all item details for debugging
    if (order.getItems() != null) {
        for (OrderItem item : order.getItems()) {
            logger.info("Item ID: {}, Product: {}, Shipped: {}, Tracking: {}", 
                item.getId(), 
                item.getProduct() != null ? item.getProduct().getName() : "NULL", 
                item.isShipped(), 
                item.getShippingTrackingNumber());
        }
    }
    
    // Validate order is in correct status
    if (order.getStatus() != OrderStatus.ACCEPTED_BY_DELIVERY && 
        order.getStatus() != OrderStatus.SHIPPED) {
        throw new BusinessException("Order must be in ACCEPTED_BY_DELIVERY status to ship items");
    }
    
    // Generate automatic tracking number
    String trackingNumber = trackingNumberService.generateTrackingNumber(orderId);
    
    // Mark items as shipped
    int shippedCount = 0;
    int alreadyShippedCount = 0;
    List<Long> alreadyShippedItemIds = new ArrayList<>();
    
    logger.info("Processing {} items for shipping. Requested item IDs: {}", 
        order.getItems().size(), shippedItemIds);
    
    for (OrderItem item : order.getItems()) {
        logger.info("Checking item ID: {} against requested IDs: {}", item.getId(), shippedItemIds);
        
        if (shippedItemIds.contains(item.getId())) {
            logger.info("Item ID {} found in requested list. Current shipped status: {}", 
                item.getId(), item.isShipped());
            
            if (!item.isShipped()) {
                item.setShipped(true);
                item.setShippedAt(LocalDateTime.now());
                item.setShippingTrackingNumber(trackingNumber);
                orderItemRepository.save(item); // Explicitly save each item
                shippedCount++;
                logger.info("Marked item {} as shipped with tracking {}", item.getId(), trackingNumber);
            } else {
                alreadyShippedCount++;
                alreadyShippedItemIds.add(item.getId());
                logger.info("Item {} was already shipped", item.getId());
            }
        } else {
            logger.info("Item ID {} not in requested list, skipping", item.getId());
        }
    }
    
    logger.info("Shipping summary - Shipped: {}, Already shipped: {}, Total items: {}", 
        shippedCount, alreadyShippedCount, order.getItems().size());
    
    // If all requested items were already shipped, provide a more informative response
    if (shippedCount == 0 && alreadyShippedCount > 0) {
        String message = String.format("All requested items (IDs: %s) were already shipped. No new items were marked as shipped.", 
            alreadyShippedItemIds.toString());
        logger.warn(message);
        throw new BusinessException(message);
    }
    
    // If no items were found in the request (shouldn't happen due to validation)
    if (shippedCount == 0 && alreadyShippedCount == 0) {
        throw new BusinessException("No valid items found to ship. Please check the item IDs provided.");
    }
    
    // Refresh the order to get updated items
    order = orderRepository.findByIdWithItems(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found after shipping"));
    
    // Check if ALL items are now shipped
    boolean allItemsShipped = order.getItems().stream()
            .allMatch(OrderItem::isShipped);
    
    // Update order status based on shipping completion
    if (allItemsShipped) {
        order.setStatus(OrderStatus.SHIPPED);
        logger.info("All items shipped, order status updated to SHIPPED");
    } else {
        // If only some items are shipped, keep status as ACCEPTED_BY_DELIVERY
        // or partial shipping status if you have one
        logger.info("Partial shipping: {} of {} items shipped", shippedCount, order.getItems().size());
    }
    
    orderRepository.save(order);
    
    String historyNotes = String.format("Shipped %d items with tracking: %s. %s", 
        shippedCount, trackingNumber, notes != null ? notes : "");
    
    recordOrderHistory(order, order.getStatus(), historyNotes);
    
    return ShippingResponse.builder()
            .message("Items shipped successfully")
            .orderId(orderId)
            .trackingNumber(trackingNumber)
            .shippedItemsCount(shippedCount)
            .totalItems(order.getItems().size())
            .allItemsShipped(allItemsShipped)
            .build();
}
    
    public ShippingResponseDTO getShippingDetails(Long orderId) {
    Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    
    List<OrderItem> shippedItems = order.getItems().stream()
            .filter(OrderItem::isShipped)
            .collect(Collectors.toList());
    
    List<OrderItem> pendingItems = order.getItems().stream()
            .filter(item -> !item.isShipped())
            .collect(Collectors.toList());
    
    // Group by tracking number
    Map<String, List<OrderItem>> itemsByTracking = shippedItems.stream()
            .collect(Collectors.groupingBy(item -> 
                item.getShippingTrackingNumber() != null ? 
                item.getShippingTrackingNumber() : "UNTRACKED"));
    
    return ShippingResponseDTO.builder()
            .orderId(orderId)
            .orderStatus(order.getStatus())
            .shippedItems(shippedItems)
            .pendingItems(pendingItems)
            .itemsByTrackingNumber(itemsByTracking)
            .totalItems(order.getItems().size())
            .shippedItemsCount(shippedItems.size())
            .pendingItemsCount(pendingItems.size())
            .build();
}
public boolean checkAllItemsShipped(Long orderId) {
    Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    
    if (order.getItems() == null || order.getItems().isEmpty()) {
        return false;
    }
    
    // Debug logging to see what's happening
    logger.info("Checking shipping status for order {} with {} items", orderId, order.getItems().size());
    
    for (OrderItem item : order.getItems()) {
        logger.info("Item {} - Shipped: {}, Tracking: {}", 
            item.getId(), item.isShipped(), item.getShippingTrackingNumber());
        if (!item.isShipped()) {
            logger.warn("Item {} is not shipped yet", item.getId());
            return false;
        }
    }
    
    logger.info("All {} items are shipped for order {}", order.getItems().size(), orderId);
    return true;
}
 // In OrderService.java
    public List<OrderItem> getOrderItems(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        return order.getItems(); // This assumes Order has a @OneToMany relationship with OrderItem
    }
    
    /**
     * Cleanup pending orders older than specified hours
     * @param hoursThreshold Number of hours after which pending orders should be cleaned up
     */
    @Transactional
    public void cleanupPendingOrders(int hoursThreshold) {
        logger.info("Cleaning up pending orders older than {} hours", hoursThreshold);
        
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(hoursThreshold);
        
        // Clean up both PENDING and PAYMENT_PENDING orders
        List<Order> pendingOrders = orderRepository.findPendingOrdersOlderThan(OrderStatus.PENDING, cutoffTime);
        List<Order> paymentPendingOrders = orderRepository.findPendingOrdersOlderThan(OrderStatus.PAYMENT_PENDING, cutoffTime);
        
        // Combine both lists
        List<Order> allPendingOrders = new ArrayList<>();
        allPendingOrders.addAll(pendingOrders);
        allPendingOrders.addAll(paymentPendingOrders);
        
        int cleanedCount = 0;
        for (Order order : allPendingOrders) {
            try {
                logger.info("Cleaning up pending order ID: {} for user ID: {} with status: {}", 
                    order.getId(), order.getUser().getId(), order.getStatus());
                
                // Update order status to CANCELLED
                order.setStatus(OrderStatus.CANCELLED);
                orderRepository.save(order);
                
                // Record order history with specific status info
                String statusInfo = order.getStatus() == OrderStatus.PAYMENT_PENDING ? 
                    "payment pending" : "pending";
                recordOrderHistory(
                    order, 
                    OrderStatus.CANCELLED, 
                    "Order automatically cancelled after " + hoursThreshold + " hours of " + statusInfo + " status"
                );
                
                // Return items to inventory if they exist
                if (order.getItems() != null && !order.getItems().isEmpty()) {
                    for (OrderItem item : order.getItems()) {
                        Product product = item.getProduct();
                        if (product != null) {
                            product.setQuantity(product.getQuantity() + item.getQuantity());
                            productRepository.save(product);
                            logger.debug("Restored {} units of product {} to inventory", 
                                item.getQuantity(), product.getId());
                        }
                    }
                }
                
                // Clear cart items if they exist
                if (order.getUser() != null) {
                    List<Cart> userCartItems = cartRepository.findByUserId(order.getUser().getId());
                    if (!userCartItems.isEmpty()) {
                        cartRepository.deleteAll(userCartItems);
                        logger.debug("Cleared cart for user ID: {}", order.getUser().getId());
                    }
                }
                
                cleanedCount++;
                
            } catch (Exception e) {
                logger.error("Error cleaning up pending order ID: {}", order.getId(), e);
            }
        }
        
        logger.info("Successfully cleaned up {} pending orders ({} PENDING, {} PAYMENT_PENDING)", 
            cleanedCount, pendingOrders.size(), paymentPendingOrders.size());
    }
}