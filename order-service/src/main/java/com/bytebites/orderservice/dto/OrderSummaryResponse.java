package com.bytebites.orderservice.dto;

import com.bytebites.orderservice.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderSummaryResponse {
    private String orderId;
    private Long customerId;
    private String restaurantId;
    private List<OrderItemResponse> orderItems;
    private BigDecimal totalAmount;
    private OrderStatus orderStatus;
    private LocalDateTime orderDate;
}