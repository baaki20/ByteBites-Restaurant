package com.bytebites.restaurantservice.service.impl;

import com.bytebites.restaurantservice.dto.MenuItemResponse;
import com.bytebites.restaurantservice.dto.RestaurantRequest;
import com.bytebites.restaurantservice.dto.RestaurantResponse;
import com.bytebites.restaurantservice.model.Restaurant;
import com.bytebites.restaurantservice.repository.MenuItemRepository;
import com.bytebites.restaurantservice.repository.RestaurantRepository;
import com.bytebites.restaurantservice.service.RestaurantService;
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
public class RestaurantServiceImpl implements RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;

    @Override
    @Transactional
    public RestaurantResponse createRestaurant(RestaurantRequest request, UUID ownerId) {
        log.info("Creating restaurant for ownerId: {}", ownerId);
        Restaurant restaurant = Restaurant.builder()
                .name(request.getName())
                .address(request.getAddress())
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail())
                .ownerId(ownerId)
                .build();
        Restaurant savedRestaurant = restaurantRepository.save(restaurant);
        log.info("Restaurant created with ID: {}", savedRestaurant.getId());
        return mapToRestaurantResponse(savedRestaurant);
    }

    @Override
    @Transactional(readOnly = true)
    public RestaurantResponse getRestaurantById(UUID id) {
        log.info("Fetching restaurant with ID: {}", id);
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Restaurant not found with ID: " + id));
        return mapToRestaurantResponse(restaurant);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RestaurantResponse> getAllRestaurants() {
        log.info("Fetching all restaurants");
        return restaurantRepository.findAll().stream()
                .map(this::mapToRestaurantResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RestaurantResponse updateRestaurant(UUID id, RestaurantRequest request, UUID ownerId) {
        log.info("Updating restaurant with ID: {} for ownerId: {}", id, ownerId);
        Restaurant existingRestaurant = restaurantRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new EntityNotFoundException("Restaurant not found or not owned by you with ID: " + id));

        existingRestaurant.setName(request.getName());
        existingRestaurant.setAddress(request.getAddress());
        existingRestaurant.setPhoneNumber(request.getPhoneNumber());
        existingRestaurant.setEmail(request.getEmail());

        Restaurant updatedRestaurant = restaurantRepository.save(existingRestaurant);
        log.info("Restaurant with ID: {} updated successfully.", updatedRestaurant.getId());
        return mapToRestaurantResponse(updatedRestaurant);
    }

    @Override
    @Transactional
    public void deleteRestaurant(UUID id, UUID ownerId) {
        log.info("Deleting restaurant with ID: {} for ownerId: {}", id, ownerId);
        if (!restaurantRepository.existsByIdAndOwnerId(id, ownerId)) {
            throw new EntityNotFoundException("Restaurant not found or not owned by you with ID: " + id);
        }
        restaurantRepository.deleteById(id);
        log.info("Restaurant with ID: {} deleted successfully.", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RestaurantResponse> getRestaurantsByOwner(UUID ownerId) {
        log.info("Fetching restaurants for ownerId: {}", ownerId);
        return restaurantRepository.findByOwnerId(ownerId).stream()
                .map(this::mapToRestaurantResponse)
                .collect(Collectors.toList());
    }

    private RestaurantResponse mapToRestaurantResponse(Restaurant restaurant) {
        List<MenuItemResponse> menuItemResponses = restaurant.getMenuItems() != null ?
                restaurant.getMenuItems().stream()
                        .map(menuItem -> MenuItemResponse.builder()
                                .id(menuItem.getId())
                                .name(menuItem.getName())
                                .description(menuItem.getDescription())
                                .price(menuItem.getPrice())
                                .available(menuItem.isAvailable())
                                .restaurantId(restaurant.getId())
                                .build())
                        .collect(Collectors.toList())
                : List.of();

        return RestaurantResponse.builder()
                .id(restaurant.getId())
                .name(restaurant.getName())
                .address(restaurant.getAddress())
                .phoneNumber(restaurant.getPhoneNumber())
                .email(restaurant.getEmail())
                .ownerId(restaurant.getOwnerId())
                .menuItems(menuItemResponses)
                .build();
    }
}