package com.bytebites.orderservice.event;

import java.math.BigDecimal;

public record OrderItemDetails(
        String menuItemId,
        String menuItemName,
        Integer quantity,
        BigDecimal price
) {}
