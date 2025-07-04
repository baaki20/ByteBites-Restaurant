package com.bytebites.restaurantservice.service;

import com.bytebites.restaurantservice.dto.RestaurantRequest;
import com.bytebites.restaurantservice.dto.RestaurantResponse;

import java.util.List;
import java.util.UUID;

public interface RestaurantService {
    RestaurantResponse createRestaurant(RestaurantRequest request, UUID ownerId);
    RestaurantResponse getRestaurantById(UUID id);
    List<RestaurantResponse> getAllRestaurants();
    RestaurantResponse updateRestaurant(UUID id, RestaurantRequest request, UUID ownerId);
    void deleteRestaurant(UUID id, UUID ownerId);
    List<RestaurantResponse> getRestaurantsByOwner(UUID ownerId);
}