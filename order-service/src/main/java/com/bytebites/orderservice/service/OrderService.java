package com.bytebites.orderservice.service;

import com.bytebites.orderservice.dto.OrderItemResponse;
import com.bytebites.orderservice.dto.OrderRequest;
import com.bytebites.orderservice.dto.OrderResponse;
import com.bytebites.orderservice.entity.Order;
import com.bytebites.orderservice.entity.OrderItem;
import com.bytebites.orderservice.enums.OrderStatus;
import com.bytebites.orderservice.event.OrderPlacedEvent;
import com.bytebites.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher applicationEventPublisher;
    private final StreamBridge streamBridge;


    public OrderResponse placeOrder(OrderRequest orderRequest, Long customerId) {
        Order order = new Order();
        order.setCustomerId(customerId);
        order.setRestaurantId(orderRequest.getRestaurantId());
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(OrderStatus.PENDING);

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = orderRequest.getItems().stream()
                .map(itemRequest -> {
                    BigDecimal itemPrice = new BigDecimal("10.00").multiply(BigDecimal.valueOf(itemRequest.getQuantity()));


                    return OrderItem.builder()
                            .menuItemId(itemRequest.getMenuItemId())
                            .quantity(itemRequest.getQuantity())
                            .itemName("Placeholder Item Name")
                            .pricePerUnit(new BigDecimal("10.00"))
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

        streamBridge.send("orderPlacedOutput", new OrderPlacedEvent(this, savedOrder.getId(), savedOrder.getCustomerId(), savedOrder.getRestaurantId(), savedOrder.getTotalAmount()));
        log.info("OrderPlacedEvent published for Order ID: {}", savedOrder.getId());


        return mapToOrderResponse(savedOrder);
    }
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId, Long authenticatedUserId, List<String> roles) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));

        if (!order.getCustomerId().equals(authenticatedUserId) && !roles.contains("ROLE_ADMIN")) {
            if (roles.contains("ROLE_RESTAURANT_OWNER")) {
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

        if (!roles.contains("ROLE_RESTAURANT_OWNER") && !roles.contains("ROLE_ADMIN")) {
            throw new AccessDeniedException("Only Restaurant Owners or Admins can view restaurant orders.");
        }

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