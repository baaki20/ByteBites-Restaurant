package com.bytebites.restaurantservice.controller;

import com.bytebites.restaurantservice.dto.RestaurantRequest;
import com.bytebites.restaurantservice.dto.RestaurantResponse;
import com.bytebites.restaurantservice.service.RestaurantService;
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
@RequestMapping("/api/restaurants")
@RequiredArgsConstructor
@Slf4j
public class RestaurantController {

    private final RestaurantService restaurantService;

    private UUID getOwnerIdFromJwt(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ROLE_RESTAURANT_OWNER')")
    public RestaurantResponse createRestaurant(@Valid @RequestBody RestaurantRequest request,
                                               @AuthenticationPrincipal Jwt jwt) {
        UUID ownerId = getOwnerIdFromJwt(jwt);
        MDC.put("ownerId", ownerId.toString());

        log.info("Received request to create restaurant by owner ID: {}", ownerId);
        try {
            RestaurantResponse response = restaurantService.createRestaurant(request, ownerId);
            MDC.put("restaurantId", response.getId().toString());
            log.info("Successfully created restaurant with ID: {} by owner ID: {}", response.getId(), ownerId);
            return response;
        } catch (Exception e) {
            log.error("Failed to create restaurant by owner ID: {}. Error: {}", ownerId, e.getMessage(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("isAuthenticated()")
    public List<RestaurantResponse> getAllRestaurants() {
        log.info("Received request to get all restaurants.");
        try {
            List<RestaurantResponse> restaurants = restaurantService.getAllRestaurants();
            log.info("Successfully retrieved {} restaurants.", restaurants.size());
            return restaurants;
        } catch (Exception e) {
            log.error("Failed to retrieve all restaurants. Error: {}", e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("isAuthenticated()")
    public RestaurantResponse getRestaurantById(@PathVariable UUID id) {
        MDC.put("restaurantId", id.toString());

        log.info("Received request to get restaurant by ID: {}", id);
        try {
            RestaurantResponse restaurant = restaurantService.getRestaurantById(id);
            log.info("Successfully retrieved restaurant by ID: {}", id);
            return restaurant;
        } catch (Exception e) {
            log.error("Failed to retrieve restaurant by ID: {}. Error: {}", id, e.getMessage(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ROLE_RESTAURANT_OWNER')")
    public RestaurantResponse updateRestaurant(@PathVariable UUID id,
                                               @Valid @RequestBody RestaurantRequest request,
                                               @AuthenticationPrincipal Jwt jwt) {
        UUID ownerId = getOwnerIdFromJwt(jwt);
        MDC.put("ownerId", ownerId.toString());
        MDC.put("restaurantId", id.toString());

        log.info("Received request to update restaurant ID: {} by owner ID: {}", id, ownerId);
        try {
            RestaurantResponse response = restaurantService.updateRestaurant(id, request, ownerId);
            log.info("Successfully updated restaurant with ID: {} by owner ID: {}", response.getId(), ownerId);
            return response;
        } catch (Exception e) {
            log.error("Failed to update restaurant ID: {} by owner ID: {}. Error: {}", id, ownerId, e.getMessage(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ROLE_RESTAURANT_OWNER')")
    public void deleteRestaurant(@PathVariable UUID id,
                                 @AuthenticationPrincipal Jwt jwt) {
        UUID ownerId = getOwnerIdFromJwt(jwt);
        MDC.put("ownerId", ownerId.toString());
        MDC.put("restaurantId", id.toString());

        log.info("Received request to delete restaurant ID: {} by owner ID: {}", id, ownerId);
        try {
            restaurantService.deleteRestaurant(id, ownerId);
            log.info("Successfully deleted restaurant ID: {} by owner ID: {}", id, ownerId);
        } catch (Exception e) {
            log.error("Failed to delete restaurant ID: {} by owner ID: {}. Error: {}", id, ownerId, e.getMessage(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/owner")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ROLE_RESTAURANT_OWNER')")
    public List<RestaurantResponse> getRestaurantsOwnedByUser(@AuthenticationPrincipal Jwt jwt) {
        UUID ownerId = getOwnerIdFromJwt(jwt);
        MDC.put("ownerId", ownerId.toString());

        log.info("Received request to get restaurants owned by owner ID: {}", ownerId);
        try {
            List<RestaurantResponse> restaurants = restaurantService.getRestaurantsByOwner(ownerId);
            log.info("Successfully retrieved {} restaurants owned by owner ID: {}", restaurants.size(), ownerId);
            return restaurants;
        } catch (Exception e) {
            log.error("Failed to retrieve restaurants owned by owner ID: {}. Error: {}", ownerId, e.getMessage(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }
}