package com.bytebites.restaurantservice.service.impl;

import com.bytebites.restaurantservice.dto.MenuItemRequest;
import com.bytebites.restaurantservice.dto.MenuItemResponse;
import com.bytebites.restaurantservice.model.MenuItem;
import com.bytebites.restaurantservice.model.Restaurant;
import com.bytebites.restaurantservice.repository.MenuItemRepository;
import com.bytebites.restaurantservice.repository.RestaurantRepository;
import com.bytebites.restaurantservice.service.MenuItemService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MenuItemServiceImpl implements MenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final RestaurantRepository restaurantRepository;

    @Override
    @Transactional
    public MenuItemResponse createMenuItem(UUID restaurantId, MenuItemRequest request, UUID ownerId) {
        log.info("Creating menu item for restaurant ID: {} by owner ID: {}", restaurantId, ownerId);
        Restaurant restaurant = restaurantRepository.findByIdAndOwnerId(restaurantId, ownerId)
                .orElseThrow(() -> new EntityNotFoundException("Restaurant not found or not owned by you with ID: " + restaurantId));

        MenuItem menuItem = MenuItem.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .available(request.isAvailable())
                .restaurant(restaurant)
                .build();

        MenuItem savedMenuItem = menuItemRepository.save(menuItem);
        log.info("Menu item created with ID: {} for restaurant ID: {}", savedMenuItem.getId(), restaurantId);
        return mapToMenuItemResponse(savedMenuItem);
    }

    @Override
    @Transactional(readOnly = true)
    public MenuItemResponse getMenuItemById(UUID restaurantId, UUID menuItemId) {
        log.info("Fetching menu item ID: {} for restaurant ID: {}", menuItemId, restaurantId);
        MenuItem menuItem = menuItemRepository.findByIdAndRestaurantId(menuItemId, restaurantId)
                .orElseThrow(() -> new EntityNotFoundException("Menu item not found with ID: " + menuItemId + " for restaurant ID: " + restaurantId));
        return mapToMenuItemResponse(menuItem);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuItemResponse> getMenuItemsByRestaurant(UUID restaurantId) {
        log.info("Fetching all menu items for restaurant ID: {}", restaurantId);
        if (!restaurantRepository.existsById(restaurantId)) {
            throw new EntityNotFoundException("Restaurant not found with ID: " + restaurantId);
        }

        return menuItemRepository.findByRestaurantId(restaurantId).stream()
                .map(this::mapToMenuItemResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public MenuItemResponse updateMenuItem(UUID restaurantId, UUID menuItemId, MenuItemRequest request, UUID ownerId) {
        log.info("Updating menu item ID: {} for restaurant ID: {} by owner ID: {}", menuItemId, restaurantId, ownerId);
        Restaurant restaurant = restaurantRepository.findByIdAndOwnerId(restaurantId, ownerId)
                .orElseThrow(() -> new EntityNotFoundException("Restaurant not found or not owned by you with ID: " + restaurantId));

        MenuItem existingMenuItem = menuItemRepository.findByIdAndRestaurantId(menuItemId, restaurantId)
                .orElseThrow(() -> new EntityNotFoundException("Menu item not found with ID: " + menuItemId + " for restaurant ID: " + restaurantId));

        existingMenuItem.setName(request.getName());
        existingMenuItem.setDescription(request.getDescription());
        existingMenuItem.setPrice(request.getPrice());
        existingMenuItem.setAvailable(request.isAvailable());

        MenuItem updatedMenuItem = menuItemRepository.save(existingMenuItem);
        log.info("Menu item with ID: {} updated successfully.", updatedMenuItem.getId());
        return mapToMenuItemResponse(updatedMenuItem);
    }

    @Override
    @Transactional
    public void deleteMenuItem(UUID restaurantId, UUID menuItemId, UUID ownerId) {
        log.info("Deleting menu item ID: {} for restaurant ID: {} by owner ID: {}", menuItemId, restaurantId, ownerId);
        if (!restaurantRepository.existsByIdAndOwnerId(restaurantId, ownerId)) {
            throw new EntityNotFoundException("Restaurant not found or not owned by you with ID: " + restaurantId);
        }

        if (!menuItemRepository.existsByIdAndRestaurantId(menuItemId, restaurantId)) {
            throw new EntityNotFoundException("Menu item not found with ID: " + menuItemId + " for restaurant ID: " + restaurantId);
        }

        menuItemRepository.deleteById(menuItemId);
        log.info("Menu item with ID: {} deleted successfully.", menuItemId);
    }

    private MenuItemResponse mapToMenuItemResponse(MenuItem menuItem) {
        return MenuItemResponse.builder()
                .id(menuItem.getId())
                .name(menuItem.getName())
                .description(menuItem.getDescription())
                .price(menuItem.getPrice())
                .available(menuItem.isAvailable())
                .restaurantId(menuItem.getRestaurant().getId())
                .build();
    }
}