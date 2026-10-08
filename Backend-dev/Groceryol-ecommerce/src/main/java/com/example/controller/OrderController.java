package com.example.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.dto.OrderDTO;
import com.example.dto.OrderHistoryDTO;
import com.example.dto.OrderHistoryMapper;
import com.example.dto.OrderItemDTO;
import com.example.dto.OrderMapper;
import com.example.dto.ShippingResponse;
import com.example.entity.Address;
import com.example.entity.Order;
import com.example.entity.OrderItem;
import com.example.entity.UserAddress;
import com.example.exception.BusinessException;
import com.example.service.OrderService;
import com.example.service.UserAddressService;
import com.example.status.OrderStatus;

import jakarta.persistence.EntityNotFoundException;

@RestController
@RequestMapping("/orders")
public class OrderController {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);
    
    private final OrderService orderService;
    private final UserAddressService userAddressService;
    @Autowired
    private   OrderHistoryMapper orderHistoryMapper;

    @Autowired
    private OrderMapper orderMapper;
    public OrderController(OrderService orderService, UserAddressService userAddressService) {
        this.orderService = orderService;
        this.userAddressService = userAddressService;
    }

    // ====================== USER APIs ====================== //

    /**
     * Create a new order (User only)
     * @param userId ID of the user placing the order
     * @param street Street address
     * @param city City
     * @param state State/province
     * @param postalCode Postal/ZIP code
     * @param country Country
     * @return Created order
     */
    @PostMapping("/create-with-saved-address/{userId}")
    public ResponseEntity<?> createOrderWithSavedAddress(
            @PathVariable Long userId,
            @RequestParam Long addressId) {
        try {
            Order order = orderService.createOrderWithSavedAddress(userId, addressId);
            return ResponseEntity.ok(orderMapper.toDTO(order));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

// Modified create order endpoint
    @PostMapping("/create/{userId}")
    public ResponseEntity<OrderDTO> createOrder(
            @PathVariable Long userId,
            @RequestParam String street,
            @RequestParam String city,
            @RequestParam String state,
            @RequestParam String postalCode,
            @RequestParam String country,
            @RequestParam(defaultValue = "false") boolean saveAddress,
            @RequestParam(required = false) String addressLabel) {
        
        // Create address using setter methods
        Address shippingAddress = new Address();
        shippingAddress.setStreet(street);
        shippingAddress.setCity(city);
        shippingAddress.setState(state);
        shippingAddress.setPostalCode(postalCode);
        shippingAddress.setCountry(country);
        
        Order order = orderService.createOrder(userId, shippingAddress, saveAddress, addressLabel);
        return ResponseEntity.ok(orderMapper.toDTO(order));
    }

// New endpoint to get user's addresses
@GetMapping("/{userId}/addresses")
public List<UserAddress> getUserAddresses(@PathVariable Long userId) {
    return userAddressService.getUserAddresses(userId);
}
    /**
     * Get order details (User, Admin, Delivery Person)
     * Note: Users can only access their own orders
     * @param orderId ID of the order
     * @return Order details
     */
@GetMapping("/{orderId}")
public ResponseEntity<OrderDTO> getOrderById(@PathVariable Long orderId) {
    Order order = orderService.getOrderById(orderId);
    return ResponseEntity.ok(orderMapper.toDTO(order));
}

@GetMapping("/user/{userId}")
public ResponseEntity<List<OrderDTO>> getOrdersByUser(@PathVariable Long userId) {
    List<Order> orders = orderService.getOrdersByUser(userId);
    List<OrderDTO> dtos = orders.stream()
        .map(orderMapper::toDTO)
        .collect(Collectors.toList());
    return ResponseEntity.ok(dtos);
}

    /**
     * Cancel an order (User, Admin)
     * Note: Users can only cancel PENDING orders
     * @param orderId ID of the order to cancel
     * @throws IOException 
     */
    @PostMapping("/{orderId}/cancel")
    public void cancelOrder(@PathVariable Long orderId) throws IOException {
        logger.info("Cancelling order ID: {}", orderId);
        orderService.updateOrderStatus(orderId, OrderStatus.CANCELLED, "Order cancelled by user", null);
        logger.info("Order {} cancelled successfully", orderId);
    }

    // ====================== ADMIN APIs ====================== //

    /**
     * Get all order history (Admin only)
     * @return List of all order history records
     */
    @GetMapping("/history/all")
    public List<OrderHistoryDTO> getAllOrderHistory() {
        return orderHistoryMapper.toDTOList(orderService.getAllOrderHistory());
    }


    /**
     * Manually process an order (Admin only)
     * Used when automatic processing fails
     * @param orderId ID of the order to process
     */
    @PostMapping("/{orderId}/process")
    public void processOrder(@PathVariable Long orderId) {
        logger.info("Admin manually processing order ID: {}", orderId);
        orderService.processOrder(orderId);
        logger.info("Order {} processed successfully", orderId);
    }

    /**
     * Get order history (Admin, Delivery Person, User)
     * Note: Users can only see their own order history
     * @param orderId ID of the order
     * @return List of status changes with timestamps
     */
    @GetMapping("/{orderId}/history")
    public List<OrderHistoryDTO> getOrderHistory(@PathVariable Long orderId) {
        return orderHistoryMapper.toDTOList(orderService.getOrderHistory(orderId));
    }
    
    
    /**
     * Get total count of all orders (Admin only)
     * @return Total number of orders
     */
    @GetMapping("/count")
    public ResponseEntity<Long> getTotalOrderCount() {
        long count = orderService.getTotalOrderCount();
        return ResponseEntity.ok(count);
    }

    /**
     * Get total amount of completed payments (Admin only)
     * @return Total amount of completed payments
     */
    @GetMapping("/completed-payments-total")
    public ResponseEntity<BigDecimal> getTotalCompletedPaymentAmount() {
        try {
            BigDecimal total = orderService.getTotalCompletedPaymentAmount();
            logger.info("Fetched total completed payments amount: {}", total);
            return ResponseEntity.ok(total);
        } catch (Exception e) {
            logger.error("Failed to fetch completed payments total", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get count of pending payment orders (Admin only)
     * @return Number of pending payment orders
     */
    @GetMapping("/pending-payments/count")
    public ResponseEntity<Long> getPendingPaymentOrderCount() {
        try {
            long count = orderService.getPendingPaymentOrderCount();
            logger.info("Fetched pending payment orders count: {}", count);
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            logger.error("Failed to fetch pending payment orders count", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get list of all pending payment orders (Admin only)
     * @return List of pending payment orders
     */
    @GetMapping("/pending-payments/orders")
    public ResponseEntity<List<OrderDTO>> getPendingPaymentOrders() {
        try {
            List<OrderDTO> orders = orderService.getPendingPaymentOrders();
            logger.info("Fetched {} pending payment orders", orders.size());
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            logger.error("Failed to fetch pending payment orders", e);
            return ResponseEntity.internalServerError().build();
        }
    }

  
   

    /**
     * Get total amount of pending payments (Admin only)
     * @return Total amount of pending payments
     */
    @GetMapping("/pending-payments/amount")
    public ResponseEntity<BigDecimal> getPendingPaymentAmount() {
        BigDecimal total = orderService.getPendingPaymentAmount();
        return ResponseEntity.ok(total);
    }
    
    /**
     * Get all orders that are pending delivery (not yet delivered)
     * Includes: PROCESSING, ACCEPTED_BY_DELIVERY, SHIPPED, OUT_FOR_DELIVERY
     * @return List of pending delivery orders
     */
    @GetMapping("/pending-delivery")
    public ResponseEntity<List<OrderDTO>> getPendingDeliveryOrders() {
        try {
            List<Order> orders = orderService.getPendingDeliveryOrders();
            List<OrderDTO> dtos = orders.stream()
                .map(orderMapper::toDTO)
                .collect(Collectors.toList());
            logger.info("Fetched {} pending delivery orders", dtos.size());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            logger.error("Failed to fetch pending delivery orders", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get all completed orders (DELIVERED status)
     * @return List of completed orders
     */
    @GetMapping("/completed")
    public ResponseEntity<List<OrderDTO>> getCompletedOrders() {
        try {
            List<Order> orders = orderService.getCompletedOrders();
            List<OrderDTO> dtos = orders.stream()
                .map(orderMapper::toDTO)
                .collect(Collectors.toList());
            logger.info("Fetched {} completed orders", dtos.size());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            logger.error("Failed to fetch completed orders", e);
            return ResponseEntity.internalServerError().build();
        }
    }

     
    
    // ====================== DELIVERY PERSON APIs ====================== //

    /**
     * Get all available orders for delivery persons (orders in PROCESSING status)
     * @return List of available orders
     */
    @GetMapping("/available")
    public List<OrderDTO> getAvailableOrdersForDelivery() {
        logger.debug("Fetching available orders for delivery");
        return orderService.getAvailableOrdersForDelivery().stream()
            .map(orderMapper::toDTO)
            .collect(Collectors.toList());
    }
    /**
     * Accept an order (Delivery Person only)
     * @param orderId ID of the order to accept
     * @param deliveryPersonId ID of the delivery person
     */
    @PostMapping("/{orderId}/accept/{deliveryPersonId}")
    public void acceptOrder(
            @PathVariable Long orderId,
            @PathVariable Long deliveryPersonId) {
        
        logger.info("Delivery person {} accepting order {}", deliveryPersonId, orderId);
        orderService.acceptOrderByDeliveryPerson(orderId, deliveryPersonId);
        logger.info("Order {} accepted by delivery person {}", orderId, deliveryPersonId);
    }

    /**
     * Reject an order (Delivery Person only)
     * @param orderId ID of the order to reject
     * @param deliveryPersonId ID of the delivery person
     * @param reason Reason for rejection
     */
    @PostMapping("/{orderId}/reject/{deliveryPersonId}")
    public void rejectOrder(
            @PathVariable Long orderId,
            @PathVariable Long deliveryPersonId,
            @RequestParam String reason) {
        
        logger.info("Delivery person {} rejecting order {}. Reason: {}", 
            deliveryPersonId, orderId, reason);
        
        orderService.rejectOrderByDeliveryPerson(orderId, deliveryPersonId, reason);
        logger.info("Order {} rejected by delivery person {}", orderId, deliveryPersonId);
    }

    /**
     * Get orders assigned to a delivery person
     * @param deliveryPersonId ID of the delivery person
     * @return List of assigned orders
     */
   

    @GetMapping("/delivery/{deliveryPersonId}")
    public List<OrderDTO> getOrdersByDeliveryPerson(@PathVariable Long deliveryPersonId) {
        logger.debug("Fetching orders for delivery person ID: {}", deliveryPersonId);
        return orderService.getOrdersByDeliveryPerson(deliveryPersonId).stream()
            .map(orderMapper::toDTO)
            .collect(Collectors.toList());
    }

    /**
     * Update order status (Delivery Person, Admin)
     * Allowed statuses for delivery:
     * - ACCEPTED_BY_DELIVERY (when accepting)
     * - SHIPPED (when dispatched)
     * - OUT_FOR_DELIVERY (when on the way)
     * - DELIVERED (when completed)
     * - REJECTED_BY_DELIVERY (if unable to deliver)
     * @param orderId ID of the order
     * @param status New status
     * @param notes Optional notes (e.g., "Left at front desk")
     */
// In OrderController.java
@GetMapping("/{orderId}/items")
public ResponseEntity<List<OrderItemDTO>> getOrderItems(
        @PathVariable Long orderId) {
    try {
        List<OrderItem> items = orderService.getOrderItems(orderId);
        List<OrderItemDTO> dtos = items.stream()
            .map(orderMapper::toOrderItemDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    } catch (EntityNotFoundException e) {
        return ResponseEntity.notFound().build();
    }
}

@PostMapping(value = "/{orderId}/status", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ResponseEntity<?> updateOrderStatus(
        @PathVariable Long orderId,
        @RequestParam OrderStatus status,
        @RequestParam(required = false) String notes,
        @RequestParam(value = "deliveryProof", required = false) MultipartFile deliveryProofImage,
        @RequestParam(value = "shippedItemIds", required = false) List<Long> shippedItemIds) {
    
    try {
        // Delivery proof validation
        if (status == OrderStatus.DELIVERED && deliveryProofImage == null) {
            return ResponseEntity.badRequest().body("Delivery proof image is required for DELIVERED status");
        }
        
        // SHIPPED status requires proper validation
        if (status == OrderStatus.SHIPPED) {
            // If shippedItemIds provided, use item-based shipping
            if (shippedItemIds != null && !shippedItemIds.isEmpty()) {
                try {
                    ShippingResponse shippingResponse = orderService.shipOrderItems(orderId, shippedItemIds, notes);
                    
                    return ResponseEntity.ok(Map.of(
                        "message", "Order shipped successfully",
                        "orderId", orderId,
                        "status", "SHIPPED",
                        "trackingNumber", shippingResponse.getTrackingNumber(),
                        "shippedItems", shippingResponse.getShippedItemsCount()
                    ));
                } catch (BusinessException e) {
                    // If all items were already shipped, return a more informative response
                    if (e.getMessage().contains("already shipped")) {
                        return ResponseEntity.badRequest().body(Map.of(
                            "error", "Items already shipped",
                            "message", e.getMessage(),
                            "orderId", orderId,
                            "suggestion", "Use direct status change or check shipping status first"
                        ));
                    }
                    throw e; // Re-throw other business exceptions
                }
            } else {
                // Direct status change to SHIPPED (mark all items as shipped)
                orderService.updateOrderStatus(orderId, status, notes, deliveryProofImage);
                
                return ResponseEntity.ok(Map.of(
                    "message", "Order status updated to SHIPPED",
                    "orderId", orderId,
                    "status", "SHIPPED"
                ));
            }
        } 
        // OUT_FOR_DELIVERY requires all items shipped
     // In the status update endpoint
        else if (status == OrderStatus.OUT_FOR_DELIVERY) {
            // Refresh the order to get latest shipping status
            Order currentOrder = orderService.getOrderById(orderId);
            
            boolean allItemsShipped = currentOrder.getItems().stream()
                    .allMatch(OrderItem::isShipped);
            
            if (!allItemsShipped) {
                // Get details of unshipped items
                List<String> unshippedItems = currentOrder.getItems().stream()
                        .filter(item -> !item.isShipped())
                        .map(item -> "Item " + item.getId() + " (" + item.getProduct().getName() + ")")
                        .collect(Collectors.toList());
                
                return ResponseEntity.badRequest().body(
                    "All items must be shipped before marking as OUT_FOR_DELIVERY. " +
                    "Unshipped items: " + String.join(", ", unshippedItems)
                );
            }
            
            orderService.updateOrderStatus(orderId, status, notes, deliveryProofImage);
            
            return ResponseEntity.ok(Map.of(
                "message", "Order status updated to OUT_FOR_DELIVERY",
                "orderId", orderId,
                "status", "OUT_FOR_DELIVERY"
            ));
        }
        // For other statuses
        else {
            orderService.updateOrderStatus(orderId, status, notes, deliveryProofImage);
            
            return ResponseEntity.ok(Map.of(
                "message", "Order status updated successfully",
                "orderId", orderId,
                "status", status
            ));
        }
    } catch (EntityNotFoundException e) {
        return ResponseEntity.notFound().build();
    } catch (BusinessException | IOException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}    




@GetMapping("/{orderId}/shipping-status")
public ResponseEntity<?> getShippingStatus(@PathVariable Long orderId) {
    try {
        Order order = orderService.getOrderById(orderId);
        Map<String, Object> response = new HashMap<>();
        
        response.put("orderId", orderId);
        response.put("orderStatus", order.getStatus());
        response.put("totalItems", order.getItems().size());
        
        List<Map<String, Object>> itemsStatus = order.getItems().stream()
            .map(item -> {
                Map<String, Object> itemInfo = new HashMap<>();
                itemInfo.put("itemId", item.getId());
                itemInfo.put("productName", item.getProduct().getName());
                itemInfo.put("quantity", item.getQuantity());
                itemInfo.put("isShipped", item.isShipped());
                itemInfo.put("trackingNumber", item.getShippingTrackingNumber());
                itemInfo.put("shippedAt", item.getShippedAt());
                return itemInfo;
            })
            .collect(Collectors.toList());
        
        response.put("items", itemsStatus);
        
        boolean allShipped = order.getItems().stream()
            .allMatch(OrderItem::isShipped);
        response.put("allItemsShipped", allShipped);
        
        // Add helpful information for shipping
        List<Long> unshippedItemIds = order.getItems().stream()
            .filter(item -> !item.isShipped())
            .map(OrderItem::getId)
            .collect(Collectors.toList());
        response.put("unshippedItemIds", unshippedItemIds);
        
        List<Long> shippedItemIds = order.getItems().stream()
            .filter(OrderItem::isShipped)
            .map(OrderItem::getId)
            .collect(Collectors.toList());
        response.put("shippedItemIds", shippedItemIds);
        
        return ResponseEntity.ok(response);
        
    } catch (EntityNotFoundException e) {
        return ResponseEntity.notFound().build();
    }
}

/**
     * Manual cleanup of pending orders (Admin only)
     * @param hoursThreshold Number of hours after which pending orders should be cleaned up
     * @return Response with cleanup results
     */
    @PostMapping("/cleanup-pending")
    public ResponseEntity<?> cleanupPendingOrders(
            @RequestParam(defaultValue = "1") int hoursThreshold) {
        try {
            logger.info("Manual cleanup of pending orders older than {} hours requested", hoursThreshold);
            orderService.cleanupPendingOrders(hoursThreshold);
            
            return ResponseEntity.ok().body(
                "Successfully cleaned up pending orders older than " + hoursThreshold + " hours"
            );
        } catch (Exception e) {
            logger.error("Failed to cleanup pending orders: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                "Failed to cleanup pending orders: " + e.getMessage()
            );
        }
    }
}