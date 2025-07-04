package com.bytebites.orderservice.service;

import com.bytebites.orderservice.dto.OrderItemResponse;
import com.bytebites.orderservice.dto.OrderRequest;
import com.bytebites.orderservice.dto.OrderResponse;
import com.bytebites.orderservice.entity.Order;
import com.bytebites.orderservice.entity.OrderItem;
import com.bytebites.orderservice.enums.OrderStatus;
import com.bytebites.orderservice.event.OrderPlacedEvent; // We'll create this next
import com.bytebites.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.ApplicationEventPublisher; // For publishing events
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher applicationEventPublisher; // To publish Spring events
    private final StreamBridge streamBridge;

    // This service would ideally call restaurant-service to get menu item details and prices
    // For now, we'll use placeholder logic for price calculation.
    // We will integrate Resilience4j here when we add inter-service communication.

    public OrderResponse placeOrder(OrderRequest orderRequest, Long customerId) {
        Order order = new Order();
        order.setCustomerId(customerId);
        order.setRestaurantId(orderRequest.getRestaurantId());
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(OrderStatus.PENDING);

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = orderRequest.getItems().stream()
                .map(itemRequest -> {
                    // In a real scenario, you'd call restaurant-service here to get real prices
                    // For now, let's assume a placeholder price
                    BigDecimal itemPrice = new BigDecimal("10.00").multiply(BigDecimal.valueOf(itemRequest.getQuantity()));

                    // Correctly update totalAmount
                    // The direct assignment below is not good for sums. Better to use a mutable container
                    // or collect and sum later. Let's stick to summing after building the list.

                    return OrderItem.builder()
                            .menuItemId(itemRequest.getMenuItemId())
                            .quantity(itemRequest.getQuantity())
                            .itemName("Placeholder Item Name") // Placeholder
                            .pricePerUnit(new BigDecimal("10.00")) // Placeholder
                            .order(order)
                            .build();
                }).collect(Collectors.toList());

        totalAmount = orderItems.stream()
                .map(item -> item.getPricePerUnit().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setTotalAmount(totalAmount);
        order.setOrderItems(orderItems);

        Order savedOrder = orderRepository.save(order);
        log.info("Order placed successfully with ID: {}", savedOrder.getId());

        // Publish OrderPlacedEvent to RabbitMQ via StreamBridge
        // The 'orderPlacedOutput' matches the binding name in application.yml
        streamBridge.send("orderPlacedOutput", new OrderPlacedEvent(this, savedOrder.getId(), savedOrder.getCustomerId(), savedOrder.getRestaurantId(), savedOrder.getTotalAmount()));
        log.info("OrderPlacedEvent published for Order ID: {}", savedOrder.getId());


        return mapToOrderResponse(savedOrder);
    }
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId, Long authenticatedUserId, List<String> roles) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId)); // Replace with custom exception

        // RBAC and Resource Ownership Check
        if (!order.getCustomerId().equals(authenticatedUserId) && !roles.contains("ROLE_ADMIN")) {
            // For restaurant owners, we need to check if they own this restaurant
            // This would require a call to restaurant-service or having restaurant owner info in JWT
            // For now, a simplified check:
            if (roles.contains("ROLE_RESTAURANT_OWNER")) {
                // This is a simplification. In a real app, you'd fetch the restaurant associated with the owner
                // from the restaurant-service and compare its ID with order.getRestaurantId()
                // For now, we'll assume a restaurant owner can only see orders for a specific restaurant they own.
                // We'll leave this part slightly loose and enhance when integrating with restaurant-service fully.
                throw new AccessDeniedException("Restaurant owners can only view orders for their owned restaurants.");
            }
            throw new AccessDeniedException("You are not authorized to view this order.");
        }
        return mapToOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getCustomerOrders(Long customerId) {
        List<Order> orders = orderRepository.findByCustomerId(customerId);
        return orders.stream().map(this::mapToOrderResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getRestaurantOrders(Long restaurantId, Long authenticatedUserId, List<String> roles) {
        // This is where you would ideally verify that the authenticatedUserId (who is ROLE_RESTAURANT_OWNER)
        // actually owns the restaurant with restaurantId.
        // For now, a simplified check: if user is ROLE_RESTAURANT_OWNER, allow access to orders for that restaurantId.
        // A more robust check would involve calling restaurant-service:
        // if (roles.contains("ROLE_RESTAURANT_OWNER") && restaurantService.isRestaurantOwnedBy(restaurantId, authenticatedUserId))
        // For simplicity, for now, we assume if you are a restaurant owner role, you can ask for orders for *any* restaurantId
        // but this must be secured more rigorously in the future.
        // The requirement states "Restaurant owners can view orders for their restaurants".
        // This implies an ownership check. Let's add a placeholder for it and emphasize it needs to be robust.

        if (!roles.contains("ROLE_RESTAURANT_OWNER") && !roles.contains("ROLE_ADMIN")) {
            throw new AccessDeniedException("Only Restaurant Owners or Admins can view restaurant orders.");
        }

        // TODO: Implement a robust check here to ensure the authenticatedUserId (if ROLE_RESTAURANT_OWNER)
        // actually owns the restaurant specified by restaurantId. This likely involves calling restaurant-service.
        // For now, we allow any ROLE_RESTAURANT_OWNER or ROLE_ADMIN to view orders by restaurantId.
        // This is a TEMPORARY simplification.
        log.warn("Simplified restaurant order access: A more robust ownership check for restaurantId {} is needed for user {}", restaurantId, authenticatedUserId);


        List<Order> orders = orderRepository.findByRestaurantId(restaurantId);
        return orders.stream().map(this::mapToOrderResponse).collect(Collectors.toList());
    }


    private OrderResponse mapToOrderResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getOrderItems().stream()
                .map(item -> OrderItemResponse.builder()
                        .menuItemId(item.getMenuItemId())
                        .itemName(item.getItemName())
                        .quantity(item.getQuantity())
                        .pricePerUnit(item.getPricePerUnit())
                        .build())
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .customerId(order.getCustomerId())
                .restaurantId(order.getRestaurantId())
                .orderDate(order.getOrderDate())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .items(itemResponses)
                .build();
    }
}