package com.bytebites.restaurantservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuItemResponse {
    private UUID id;
    private String name;
    private String description;
    private BigDecimal price;
    private boolean available;
    private UUID restaurantId;
}