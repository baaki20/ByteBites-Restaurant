package com.bytebites.orderservice.dto;

import com.bytebites.orderservice.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderStatusUpdateRequest {
    @NotNull(message = "Order status cannot be null")
    private OrderStatus newStatus;
}