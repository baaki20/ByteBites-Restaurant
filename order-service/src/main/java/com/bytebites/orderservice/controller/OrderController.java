package com.bytebites.orderservice.controller;

import com.bytebites.orderservice.dto.OrderRequest;
import com.bytebites.orderservice.dto.OrderResponse;
import com.bytebites.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // This method will extract user details from the JWT processed by the API Gateway.
    // The Gateway should forward customerId and roles as headers.
    // We'll need a way to extract these from the request. For now, let's assume they are available.
    // We'll refine this when we discuss JWT integration specifically for microservices.
    // A common pattern is to use a custom argument resolver or a filter in the service itself.
    // For now, we'll pass them explicitly as parameters, simulating their extraction.

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    public OrderResponse placeOrder(@RequestBody OrderRequest orderRequest,
                                    @RequestHeader("X-Customer-ID") Long customerId) {
        // The API Gateway validates JWT and adds X-Customer-ID header.
        return orderService.placeOrder(orderRequest, customerId);
    }

    @GetMapping("/{orderId}")
    @ResponseStatus(HttpStatus.OK)
    // RBAC: Resource owner only
    @PreAuthorize("hasAnyRole('ROLE_CUSTOMER', 'ROLE_RESTAURANT_OWNER', 'ROLE_ADMIN')")
    public OrderResponse getOrderById(@PathVariable Long orderId,
                                      @RequestHeader("X-User-ID") Long authenticatedUserId,
                                      @RequestHeader("X-User-Roles") List<String> roles) {
        // The API Gateway validates JWT and adds X-User-ID and X-User-Roles headers.
        return orderService.getOrderById(orderId, authenticatedUserId, roles);
    }

    @GetMapping("/my-orders")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    public List<OrderResponse> getCustomerOrders(@RequestHeader("X-Customer-ID") Long customerId) {
        // Customers can only see their orders
        return orderService.getCustomerOrders(customerId);
    }

    @GetMapping("/restaurant/{restaurantId}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('ROLE_RESTAURANT_OWNER', 'ROLE_ADMIN')")
    public List<OrderResponse> getRestaurantOrders(@PathVariable Long restaurantId,
                                                   @RequestHeader("X-User-ID") Long authenticatedUserId,
                                                   @RequestHeader("X-User-Roles") List<String> roles) {
        // Restaurant owners can view orders for their restaurants
        return orderService.getRestaurantOrders(restaurantId, authenticatedUserId, roles);
    }
}