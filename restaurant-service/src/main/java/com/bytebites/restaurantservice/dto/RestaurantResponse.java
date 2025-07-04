package com.bytebites.restaurantservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestaurantResponse {
    private UUID id;
    private String name;
    private String address;
    private String phoneNumber;
    private String email;
    private UUID ownerId;
    private List<MenuItemResponse> menuItems;
}