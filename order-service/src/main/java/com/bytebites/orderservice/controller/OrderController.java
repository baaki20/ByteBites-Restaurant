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


    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    public OrderResponse placeOrder(@RequestBody OrderRequest orderRequest,
                                    @RequestHeader("X-Customer-ID") Long customerId) {
        return orderService.placeOrder(orderRequest, customerId);
    }

    @GetMapping("/{orderId}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('ROLE_CUSTOMER', 'ROLE_RESTAURANT_OWNER', 'ROLE_ADMIN')")
    public OrderResponse getOrderById(@PathVariable Long orderId,
                                      @RequestHeader("X-User-ID") Long authenticatedUserId,
                                      @RequestHeader("X-User-Roles") List<String> roles) {
        return orderService.getOrderById(orderId, authenticatedUserId, roles);
    }

    @GetMapping("/my-orders")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    public List<OrderResponse> getCustomerOrders(@RequestHeader("X-Customer-ID") Long customerId) {
        return orderService.getCustomerOrders(customerId);
    }

    @GetMapping("/restaurant/{restaurantId}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('ROLE_RESTAURANT_OWNER', 'ROLE_ADMIN')")
    public List<OrderResponse> getRestaurantOrders(@PathVariable Long restaurantId,
                                                   @RequestHeader("X-User-ID") Long authenticatedUserId,
                                                   @RequestHeader("X-User-Roles") List<String> roles) {
        return orderService.getRestaurantOrders(restaurantId, authenticatedUserId, roles);
    }
}