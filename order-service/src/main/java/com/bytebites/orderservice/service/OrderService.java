package com.bytebites.orderservice.service;

import com.bytebites.orderservice.dto.OrderRequest;
import com.bytebites.orderservice.dto.OrderSummaryResponse;
import com.bytebites.orderservice.dto.OrderStatusUpdateRequest;

import java.util.List;

public interface OrderService {
    String placeOrder(OrderRequest orderRequest);
    OrderSummaryResponse getOrderById(String orderId, Long authenticatedUserId, List<String> roles);
    List<OrderSummaryResponse> getCustomerOrders(Long customerId);
    List<OrderSummaryResponse> getRestaurantOrders(String restaurantId, Long authenticatedUserId, List<String> roles);
    OrderSummaryResponse updateOrderStatus(String orderId, OrderStatusUpdateRequest statusUpdateRequest, Long authenticatedUserId, List<String> roles);
    void cancelOrder(String orderId, Long authenticatedUserId, List<String> roles);
}