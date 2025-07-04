package com.bytebites.orderservice.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;

@Getter
public class OrderPlacedEvent extends ApplicationEvent {
    private final Long orderId;
    private final Long customerId;
    private final Long restaurantId;
    private final BigDecimal totalAmount;

    public OrderPlacedEvent(Object source, Long orderId, Long customerId, Long restaurantId, BigDecimal totalAmount) {
        super(source);
        this.orderId = orderId;
        this.customerId = customerId;
        this.restaurantId = restaurantId;
        this.totalAmount = totalAmount;
    }
}