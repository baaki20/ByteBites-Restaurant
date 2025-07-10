package com.bytebites.restaurantservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuItemRequest {
    @NotBlank(message = "Menu item name cannot be empty")
    private String name;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @NotNull(message = "Price cannot be null")
    @DecimalMin(value = "0.01", message = "Price must be greater than or equal to 0.01")
    @Digits(integer = 10, fraction = 2, message = "Price can have at most 10 integer digits and 2 fractional digits")
    private BigDecimal price;

    @NotNull(message = "Availability cannot be null")
    private boolean available;
}