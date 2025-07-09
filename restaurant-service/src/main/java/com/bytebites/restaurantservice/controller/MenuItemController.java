package com.bytebites.restaurantservice.controller;

import com.bytebites.restaurantservice.dto.MenuItemRequest;
import com.bytebites.restaurantservice.dto.MenuItemResponse;
import com.bytebites.restaurantservice.service.MenuItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/restaurants/{restaurantId}/menu-items")
@RequiredArgsConstructor
@Slf4j
public class MenuItemController {

    private final MenuItemService menuItemService;

    private UUID getOwnerIdFromJwt(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ROLE_RESTAURANT_OWNER')")
    public MenuItemResponse createMenuItem(@PathVariable UUID restaurantId,
                                           @Valid @RequestBody MenuItemRequest request,
                                           @AuthenticationPrincipal Jwt jwt) {
        UUID ownerId = getOwnerIdFromJwt(jwt);
        MDC.put("ownerId", ownerId.toString());
        MDC.put("restaurantId", restaurantId.toString());

        log.info("Received request to create menu item for restaurant ID: {}", restaurantId);
        try {
            MenuItemResponse response = menuItemService.createMenuItem(restaurantId, request, ownerId);
            log.info("Successfully created menu item with ID: {} for restaurant ID: {}", response.getId(), restaurantId);
            return response;
        } catch (Exception e) {
            log.error("Failed to create menu item for restaurant ID: {}. Error: {}", restaurantId, e.getMessage(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("isAuthenticated()")
    public List<MenuItemResponse> getMenuItemsByRestaurant(@PathVariable UUID restaurantId) {
        MDC.put("restaurantId", restaurantId.toString());

        log.info("Received request to get all menu items for restaurant ID: {}", restaurantId);
        try {
            List<MenuItemResponse> menuItems = menuItemService.getMenuItemsByRestaurant(restaurantId);
            log.info("Successfully retrieved {} menu items for restaurant ID: {}", menuItems.size(), restaurantId);
            return menuItems;
        } catch (Exception e) {
            log.error("Failed to retrieve menu items for restaurant ID: {}. Error: {}", restaurantId, e.getMessage(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/{menuItemId}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("isAuthenticated()")
    public MenuItemResponse getMenuItemById(@PathVariable UUID restaurantId,
                                            @PathVariable UUID menuItemId) {
        MDC.put("restaurantId", restaurantId.toString());
        MDC.put("menuItemId", menuItemId.toString());

        log.info("Received request to get menu item ID: {} for restaurant ID: {}", menuItemId, restaurantId);
        try {
            MenuItemResponse menuItem = menuItemService.getMenuItemById(restaurantId, menuItemId);
            log.info("Successfully retrieved menu item ID: {} for restaurant ID: {}", menuItemId, restaurantId);
            return menuItem;
        } catch (Exception e) {
            log.error("Failed to retrieve menu item ID: {} for restaurant ID: {}. Error: {}", menuItemId, restaurantId, e.getMessage(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }

    @PutMapping("/{menuItemId}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ROLE_RESTAURANT_OWNER')")
    public MenuItemResponse updateMenuItem(@PathVariable UUID restaurantId,
                                           @PathVariable UUID menuItemId,
                                           @Valid @RequestBody MenuItemRequest request,
                                           @AuthenticationPrincipal Jwt jwt) {
        UUID ownerId = getOwnerIdFromJwt(jwt);
        MDC.put("ownerId", ownerId.toString());
        MDC.put("restaurantId", restaurantId.toString());
        MDC.put("menuItemId", menuItemId.toString());

        log.info("Received request to update menu item ID: {} for restaurant ID: {}", menuItemId, restaurantId);
        try {
            MenuItemResponse response = menuItemService.updateMenuItem(restaurantId, menuItemId, request, ownerId);
            log.info("Successfully updated menu item with ID: {} for restaurant ID: {}", response.getId(), restaurantId);
            return response;
        } catch (Exception e) {
            log.error("Failed to update menu item ID: {} for restaurant ID: {}. Error: {}", menuItemId, restaurantId, e.getMessage(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }

    @DeleteMapping("/{menuItemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ROLE_RESTAURANT_OWNER')")
    public void deleteMenuItem(@PathVariable UUID restaurantId,
                               @PathVariable UUID menuItemId,
                               @AuthenticationPrincipal Jwt jwt) {
        UUID ownerId = getOwnerIdFromJwt(jwt);
        MDC.put("ownerId", ownerId.toString());
        MDC.put("restaurantId", restaurantId.toString());
        MDC.put("menuItemId", menuItemId.toString());

        log.info("Received request to delete menu item ID: {} for restaurant ID: {}", menuItemId, restaurantId);
        try {
            menuItemService.deleteMenuItem(restaurantId, menuItemId, ownerId);
            log.info("Successfully deleted menu item ID: {} for restaurant ID: {}", menuItemId, restaurantId);
        } catch (Exception e) {
            log.error("Failed to delete menu item ID: {} for restaurant ID: {}. Error: {}", menuItemId, restaurantId, e.getMessage(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }
}