package com.bytebites.orderservice.dto;

import java.math.BigDecimal;

public record MenuItemServiceResponse(
        String id,
        String name,
        BigDecimal price
) {}