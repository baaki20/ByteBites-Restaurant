package com.bytebites.restaurantservice.service;

import com.bytebites.restaurantservice.dto.MenuItemRequest;
import com.bytebites.restaurantservice.dto.MenuItemResponse;

import java.util.List;
import java.util.UUID;

public interface MenuItemService {
    MenuItemResponse createMenuItem(UUID restaurantId, MenuItemRequest request, UUID ownerId);
    MenuItemResponse getMenuItemById(UUID restaurantId, UUID menuItemId);
    List<MenuItemResponse> getMenuItemsByRestaurant(UUID restaurantId);
    MenuItemResponse updateMenuItem(UUID restaurantId, UUID menuItemId, MenuItemRequest request, UUID ownerId);
    void deleteMenuItem(UUID restaurantId, UUID menuItemId, UUID ownerId);
}