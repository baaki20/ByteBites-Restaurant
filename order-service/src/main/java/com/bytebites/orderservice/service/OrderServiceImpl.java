package com.bytebites.orderservice.service;

import com.bytebites.orderservice.dto.CustomApiResponse;
import com.bytebites.orderservice.dto.MenuItemServiceResponse;
import com.bytebites.orderservice.dto.OrderItemRequest;
import com.bytebites.orderservice.dto.OrderItemResponse;
import com.bytebites.orderservice.dto.OrderRequest;
import com.bytebites.orderservice.dto.OrderSummaryResponse;
import com.bytebites.orderservice.dto.OrderStatusUpdateRequest;
import com.bytebites.orderservice.dto.RestaurantServiceResponse;
import com.bytebites.orderservice.event.OrderItemDetails;
import com.bytebites.orderservice.event.OrderPlacedEvent;
import com.bytebites.orderservice.exception.ResourceNotFoundException;
import com.bytebites.orderservice.exception.UnauthorizedAccessException;
import com.bytebites.orderservice.model.Order;
import com.bytebites.orderservice.model.OrderItem;
import com.bytebites.orderservice.enums.OrderStatus;
import com.bytebites.orderservice.repository.OrderRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    @Value("${restaurant.service.url}")
    private String restaurantServiceUrl;

    private static final String ORDER_EVENTS_TOPIC = "order-events-topic";

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedAccessException("User is not authenticated.");
        }
        return authentication.getName();
    }

    @Override
    public String placeOrder(OrderRequest orderRequest) {
        if (orderRequest.getOrderItems() == null || orderRequest.getOrderItems().isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }

        Order order = Order.builder()
                .userEmail(getCurrentUserEmail())
                .restaurantId(String.valueOf(orderRequest.getRestaurantId()))
                .deliveryAddress(orderRequest.getDeliveryAddress())
                .status(OrderStatus.PENDING)
                .orderDate(LocalDateTime.now())
                .lastUpdated(LocalDateTime.now())
                .build();

        log.info("Calling restaurant-service for restaurant ID: {} with Circuit Breaker.", orderRequest.getRestaurantId());
        CustomApiResponse<RestaurantServiceResponse> restaurantApiResponse = getRestaurantServiceDetails(String.valueOf(orderRequest.getRestaurantId()))
                .block();

        if (restaurantApiResponse == null || !restaurantApiResponse.isSuccess() || restaurantApiResponse.getData() == null) {
            log.error("Failed to retrieve restaurant details or fallback returned null/failure for restaurantId: {}", orderRequest.getRestaurantId());
            throw new RuntimeException("Cannot create order: Restaurant details unavailable due to service issue or invalid response.");
        }
        order.setRestaurantName(restaurantApiResponse.getData().name());

        WebClient restaurantWebClient = webClientBuilder.baseUrl(restaurantServiceUrl).build();
        BigDecimal calculatedTotalAmount = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : orderRequest.getOrderItems()) {
            if (itemRequest.getQuantity() <= 0) {
                throw new IllegalArgumentException("Quantity must be positive for menu item: " + itemRequest.getMenuItemId());
            }

            CustomApiResponse<MenuItemServiceResponse> menuItemApiResponse = restaurantWebClient.get()
                    .uri("/api/restaurants/{restaurantId}/menu-items/{menuItemId}",
                            orderRequest.getRestaurantId(), itemRequest.getMenuItemId())
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .flatMap((Function<? super String, ? extends Mono<? extends Throwable>>) errorBody -> Mono.error(new ResourceNotFoundException(
                                            "Menu item not found with ID: " + itemRequest.getMenuItemId() + ". Error: " + errorBody))))
                    .bodyToMono(new ParameterizedTypeReference<CustomApiResponse<MenuItemServiceResponse>>() {})
                    .block();

            if (menuItemApiResponse == null || !menuItemApiResponse.isSuccess() || menuItemApiResponse.getData() == null) {
                throw new ResourceNotFoundException("Menu item not found or unavailable with ID: " + itemRequest.getMenuItemId());
            }

            MenuItemServiceResponse menuItem = menuItemApiResponse.getData();

            OrderItem orderItem = OrderItem.builder()
                    .menuItemId(menuItem.id())
                    .menuItemName(menuItem.name())
                    .quantity(itemRequest.getQuantity())
                    .price(menuItem.price())
                    .build();

            order.addOrderItem(orderItem);

            calculatedTotalAmount = calculatedTotalAmount.add(
                    menuItem.price().multiply(BigDecimal.valueOf(itemRequest.getQuantity()))
            );
        }

        order.setTotalAmount(calculatedTotalAmount);

        Order savedOrder = orderRepository.save(order);
        log.info("Order placed successfully with ID: {}", savedOrder.getId());

        List<OrderItemDetails> itemDetails = savedOrder.getOrderItems().stream()
                .map(item -> new OrderItemDetails(
                        item.getMenuItemId(),
                        item.getMenuItemName(),
                        item.getQuantity(),
                        item.getPrice()
                ))
                .collect(Collectors.toList());

        OrderPlacedEvent event = new OrderPlacedEvent(
                savedOrder.getId(),
                savedOrder.getUserEmail(),
                savedOrder.getRestaurantId(),
                savedOrder.getRestaurantName(),
                savedOrder.getTotalAmount(),
                savedOrder.getDeliveryAddress(),
                savedOrder.getOrderDate(),
                itemDetails
        );

        kafkaTemplate.send(ORDER_EVENTS_TOPIC, event.orderId(), event);
        log.info("OrderPlacedEvent published for Order ID: {}", savedOrder.getId());

        return savedOrder.getId();
    }

    @CircuitBreaker(name = "restaurantServiceCircuitBreaker", fallbackMethod = "getRestaurantFallback")
    public Mono<CustomApiResponse<RestaurantServiceResponse>> getRestaurantServiceDetails(String restaurantId) {
        log.info("Attempting to get restaurant details from restaurant-service for ID: {}", restaurantId);
        return webClientBuilder.baseUrl(restaurantServiceUrl).build()
                .get()
                .uri("/api/restaurants/{restaurantId}", restaurantId)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class).flatMap(errorBody -> {
                            log.error("Error response from restaurant-service (status {}): {}", clientResponse.statusCode(), errorBody);
                            return Mono.error(new RuntimeException("Restaurant service returned error: " + errorBody));
                        }))
                .bodyToMono(new ParameterizedTypeReference<CustomApiResponse<RestaurantServiceResponse>>() {})
                .doOnError(e -> log.error("WebClient call to restaurant-service failed: {}", e.getMessage()));
    }

    public Mono<CustomApiResponse<RestaurantServiceResponse>> getRestaurantFallback(String restaurantId, Throwable t) {
        log.warn("Fallback triggered for getRestaurantServiceDetails for restaurantId: {}. Reason: {}", restaurantId, t.getMessage());

        RestaurantServiceResponse fallbackRestaurant = new RestaurantServiceResponse(
                restaurantId,
                "Fallback Restaurant Name (Service Unavailable)",
                "Fallback Address (Service Issue)",
                "Fallback Contact (Service Issue)"
        );

        CustomApiResponse<RestaurantServiceResponse> fallbackApiResponse = new CustomApiResponse<>(
                false,
                "Restaurant service is currently unavailable. Using fallback data.",
                503,
                fallbackRestaurant
        );

        return Mono.just(fallbackApiResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderSummaryResponse getOrderById(String orderId, Long authenticatedUserId, List<String> roles) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (roles.contains("ROLE_CUSTOMER") && !order.getUserEmail().equals(getCurrentUserEmail())) {
            throw new UnauthorizedAccessException("Customers can only view their own orders.");
        }
        if (roles.contains("ROLE_RESTAURANT_OWNER") && !isRestaurantOwnerOfOrder(authenticatedUserId, order.getRestaurantId())) {
            throw new UnauthorizedAccessException("Restaurant owners can only view orders for their owned restaurants.");
        }

        return convertToDto(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderSummaryResponse> getCustomerOrders(Long customerId) {
        List<Order> orders = orderRepository.findByUserEmail(getCurrentUserEmail());
        return orders.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderSummaryResponse> getRestaurantOrders(String restaurantId, Long authenticatedUserId, List<String> roles) {
        if (roles.contains("ROLE_RESTAURANT_OWNER") && !isRestaurantOwnerOfOrder(authenticatedUserId, restaurantId)) {
            throw new UnauthorizedAccessException("Restaurant owners can only view orders for their owned restaurants.");
        }

        List<Order> orders = orderRepository.findByRestaurantId(restaurantId);
        return orders.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    @Override
    public OrderSummaryResponse updateOrderStatus(String orderId, OrderStatusUpdateRequest statusUpdateRequest, Long authenticatedUserId, List<String> roles) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (roles.contains("ROLE_RESTAURANT_OWNER") && !isRestaurantOwnerOfOrder(authenticatedUserId, order.getRestaurantId())) {
            throw new UnauthorizedAccessException("Restaurant owners can only update status for their owned restaurants' orders.");
        }

        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Cannot update status for an order that is already DELIVERED or CANCELLED.");
        }

        order.setStatus(statusUpdateRequest.getNewStatus());
        order.setLastUpdated(LocalDateTime.now());

        Order updatedOrder = orderRepository.save(order);
        return convertToDto(updatedOrder);
    }

    @Override
    public void cancelOrder(String orderId, Long authenticatedUserId, List<String> roles) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (roles.contains("ROLE_CUSTOMER") && !order.getUserEmail().equals(getCurrentUserEmail())) {
            throw new UnauthorizedAccessException("Customers can only cancel their own orders.");
        }

        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Cannot cancel an order that is already DELIVERED or CANCELLED.");
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setLastUpdated(LocalDateTime.now());
        orderRepository.save(order);
    }

    private OrderSummaryResponse convertToDto(Order order) {
        List<OrderItemResponse> itemResponses = order.getOrderItems().stream()
            .map(item -> OrderItemResponse.builder()
                .menuItemId(Long.valueOf(item.getMenuItemId()))
                .quantity(item.getQuantity())
                .build())
            .collect(Collectors.toList());

        return OrderSummaryResponse.builder()
            .orderId(order.getId())
            .customerId(null)
            .restaurantId(order.getRestaurantId())
            .orderItems(itemResponses)
            .totalAmount(order.getTotalAmount())
            .orderDate(order.getOrderDate())
            .build();
    }

    private boolean isRestaurantOwnerOfOrder(Long authenticatedUserId, String restaurantId) {
        log.warn("Placeholder for restaurant ownership check for restaurantId: {} and userId: {}. Implement actual call to restaurant-service.", restaurantId, authenticatedUserId);
        return true;
    }
}