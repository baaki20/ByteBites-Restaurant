package com.bytebites.restaurantservice.controller;

import com.bytebites.restaurantservice.dto.MenuItemRequest;
import com.bytebites.restaurantservice.dto.MenuItemResponse;
import com.bytebites.restaurantservice.service.MenuItemService;
import lombok.RequiredArgsConstructor;
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
public class MenuItemController {

    private final MenuItemService menuItemService;

    private UUID getOwnerIdFromJwt(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ROLE_RESTAURANT_OWNER')")
    public MenuItemResponse createMenuItem(@PathVariable UUID restaurantId,
                                           @RequestBody MenuItemRequest request,
                                           @AuthenticationPrincipal Jwt jwt) {
        UUID ownerId = getOwnerIdFromJwt(jwt);
        return menuItemService.createMenuItem(restaurantId, request, ownerId);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("isAuthenticated()")
    public List<MenuItemResponse> getMenuItemsByRestaurant(@PathVariable UUID restaurantId) {
        return menuItemService.getMenuItemsByRestaurant(restaurantId);
    }

    @GetMapping("/{menuItemId}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("isAuthenticated()")
    public MenuItemResponse getMenuItemById(@PathVariable UUID restaurantId,
                                            @PathVariable UUID menuItemId) {
        return menuItemService.getMenuItemById(restaurantId, menuItemId);
    }

    @PutMapping("/{menuItemId}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ROLE_RESTAURANT_OWNER')")
    public MenuItemResponse updateMenuItem(@PathVariable UUID restaurantId,
                                           @PathVariable UUID menuItemId,
                                           @RequestBody MenuItemRequest request,
                                           @AuthenticationPrincipal Jwt jwt) {
        UUID ownerId = getOwnerIdFromJwt(jwt);
        return menuItemService.updateMenuItem(restaurantId, menuItemId, request, ownerId);
    }

    @DeleteMapping("/{menuItemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ROLE_RESTAURANT_OWNER')")
    public void deleteMenuItem(@PathVariable UUID restaurantId,
                               @PathVariable UUID menuItemId,
                               @AuthenticationPrincipal Jwt jwt) {
        UUID ownerId = getOwnerIdFromJwt(jwt);
        menuItemService.deleteMenuItem(restaurantId, menuItemId, ownerId);
    }
}