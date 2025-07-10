package com.bytebites.orderservice.controller;

import com.bytebites.orderservice.dto.CustomApiResponse;
import com.bytebites.orderservice.dto.OrderRequest;
import com.bytebites.orderservice.dto.OrderSummaryResponse;
import com.bytebites.orderservice.dto.OrderStatusUpdateRequest;
import com.bytebites.orderservice.service.OrderServiceImpl;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderServiceImpl orderServiceImpl;

    public OrderController(OrderServiceImpl orderService) {
        this.orderServiceImpl = orderService;
    }

    @GetMapping
    public ResponseEntity<CustomApiResponse<String>> getOrders() {
        return new ResponseEntity<>(
                new CustomApiResponse<>(true, "Orders fetched (placeholder)", HttpStatus.OK.value(), "orders fetched"),
                HttpStatus.OK
        );
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    public ResponseEntity<CustomApiResponse<String>> placeOrder(
            @Valid @RequestBody OrderRequest orderRequest) {
        String createdOrderId = orderServiceImpl.placeOrder(orderRequest);
        return new ResponseEntity<>(
                new CustomApiResponse<>(true, "Order created successfully", HttpStatus.CREATED.value(), createdOrderId),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasAnyRole('ROLE_CUSTOMER', 'ROLE_RESTAURANT_OWNER', 'ROLE_ADMIN')")
    public ResponseEntity<CustomApiResponse<OrderSummaryResponse>> getOrderById(
            @PathVariable String orderId,
            @RequestHeader("X-User-ID") Long authenticatedUserId,
            @RequestHeader("X-User-Roles") List<String> roles) {
        OrderSummaryResponse order = orderServiceImpl.getOrderById(orderId, authenticatedUserId, roles);
        return new ResponseEntity<>(
                new CustomApiResponse<>(true, "Order retrieved successfully", HttpStatus.OK.value(), order),
                HttpStatus.OK
        );
    }

    @GetMapping("/my-orders")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    public ResponseEntity<CustomApiResponse<List<OrderSummaryResponse>>> getCustomerOrders(
            @RequestHeader("X-Customer-ID") Long customerId) {
        List<OrderSummaryResponse> orders = orderServiceImpl.getCustomerOrders(customerId);
        return new ResponseEntity<>(
                new CustomApiResponse<>(true, "Customer orders retrieved successfully", HttpStatus.OK.value(), orders),
                HttpStatus.OK
        );
    }

    @GetMapping("/restaurant/{restaurantId}")
    @PreAuthorize("hasAnyRole('ROLE_RESTAURANT_OWNER', 'ROLE_ADMIN')")
    public ResponseEntity<CustomApiResponse<List<OrderSummaryResponse>>> getRestaurantOrders(
            @PathVariable String restaurantId,
            @RequestHeader("X-User-ID") Long authenticatedUserId,
            @RequestHeader("X-User-Roles") List<String> roles) {
        List<OrderSummaryResponse> orders = orderServiceImpl.getRestaurantOrders(restaurantId, authenticatedUserId, roles);
        return new ResponseEntity<>(
                new CustomApiResponse<>(true, "Orders retrieved for restaurant successfully", HttpStatus.OK.value(), orders),
                HttpStatus.OK
        );
    }

    @PutMapping("/{orderId}/status")
    @PreAuthorize("hasAnyRole('ROLE_RESTAURANT_OWNER', 'ROLE_ADMIN')")
    public ResponseEntity<CustomApiResponse<OrderSummaryResponse>> updateOrderStatus(
            @PathVariable String orderId,
            @Valid @RequestBody OrderStatusUpdateRequest statusUpdateRequest,
            @RequestHeader("X-User-ID") Long authenticatedUserId,
            @RequestHeader("X-User-Roles") List<String> roles) {
        OrderSummaryResponse updatedOrder = orderServiceImpl.updateOrderStatus(orderId, statusUpdateRequest, authenticatedUserId, roles);
        return new ResponseEntity<>(
                new CustomApiResponse<>(true, "Order status updated successfully", HttpStatus.OK.value(), updatedOrder),
                HttpStatus.OK
        );
    }

    @DeleteMapping("/{orderId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ROLE_CUSTOMER') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<CustomApiResponse<Void>> cancelOrder(
            @PathVariable String orderId,
            @RequestHeader("X-User-ID") Long authenticatedUserId,
            @RequestHeader("X-User-Roles") List<String> roles) {
        orderServiceImpl.cancelOrder(orderId, authenticatedUserId, roles);
        return new ResponseEntity<>(
                new CustomApiResponse<>(true, "Order cancelled successfully", HttpStatus.NO_CONTENT.value(), null),
                HttpStatus.NO_CONTENT
        );
    }
}