package com.bytebites.restaurantservice.controller;

import com.bytebites.restaurantservice.dto.RestaurantRequest;
import com.bytebites.restaurantservice.dto.RestaurantResponse;
import com.bytebites.restaurantservice.security.GatewayAuthenticationToken;
import com.bytebites.restaurantservice.service.RestaurantService;
import lombok.RequiredArgsConstructor;
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
public class RestaurantController {

    private final RestaurantService restaurantService;

    private UUID getOwnerIdFromJwt(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
//    @PreAuthorize("hasRole('ROLE_RESTAURANT_OWNER')")
    public RestaurantResponse createRestaurant(@RequestBody RestaurantRequest request,
                                               @AuthenticationPrincipal GatewayAuthenticationToken authentication) {

        String userEmail = authentication.getEmail();
        UUID ownerId = UUID.nameUUIDFromBytes(userEmail.getBytes());
        return restaurantService.createRestaurant(request, ownerId);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
//    @PreAuthorize("isAuthenticated()")
    public List<RestaurantResponse> getAllRestaurants() {
        return restaurantService.getAllRestaurants();
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("isAuthenticated()")
    public RestaurantResponse getRestaurantById(@PathVariable UUID id) {
        return restaurantService.getRestaurantById(id);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ROLE_RESTAURANT_OWNER')")
    public RestaurantResponse updateRestaurant(@PathVariable UUID id,
                                               @RequestBody RestaurantRequest request,
                                               @AuthenticationPrincipal Jwt jwt) {
        UUID ownerId = getOwnerIdFromJwt(jwt);
        return restaurantService.updateRestaurant(id, request, ownerId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ROLE_RESTAURANT_OWNER')")
    public void deleteRestaurant(@PathVariable UUID id,
                                 @AuthenticationPrincipal Jwt jwt) {
        UUID ownerId = getOwnerIdFromJwt(jwt);
        restaurantService.deleteRestaurant(id, ownerId);
    }

    @GetMapping("/owner")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ROLE_RESTAURANT_OWNER')")
    public List<RestaurantResponse> getRestaurantsOwnedByUser(@AuthenticationPrincipal Jwt jwt) {
        UUID ownerId = getOwnerIdFromJwt(jwt);
        return restaurantService.getRestaurantsByOwner(ownerId);
    }
}