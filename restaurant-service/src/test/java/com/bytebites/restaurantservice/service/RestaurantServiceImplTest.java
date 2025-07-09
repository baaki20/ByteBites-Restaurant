package com.bytebites.restaurantservice.service;

import com.bytebites.restaurantservice.dto.RestaurantRequest;
import com.bytebites.restaurantservice.dto.RestaurantResponse;
import com.bytebites.restaurantservice.model.Restaurant;
import com.bytebites.restaurantservice.repository.MenuItemRepository;
import com.bytebites.restaurantservice.repository.RestaurantRepository;
import com.bytebites.restaurantservice.service.impl.RestaurantServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestaurantServiceImplTest {

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private MenuItemRepository menuItemRepository;

    @InjectMocks
    private RestaurantServiceImpl restaurantService;

    private UUID ownerId;
    private UUID restaurantId;
    private RestaurantRequest restaurantRequest;
    private Restaurant restaurant;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        restaurantId = UUID.randomUUID();
        restaurantRequest = RestaurantRequest.builder()
                .name("Test Restaurant")
                .address("123 Test St")
                .phoneNumber("123-456-7890")
                .email("test@example.com")
                .build();

        restaurant = Restaurant.builder()
                .id(restaurantId)
                .name("Test Restaurant")
                .address("123 Test St")
                .phoneNumber("123-456-7890")
                .email("test@example.com")
                .ownerId(ownerId)
                .menuItems(List.of())
                .build();
    }

    @Test
    @DisplayName("Should create a restaurant successfully")
    void createRestaurant_Success() {
        when(restaurantRepository.save(any(Restaurant.class))).thenReturn(restaurant);

        RestaurantResponse response = restaurantService.createRestaurant(restaurantRequest, ownerId);

        assertNotNull(response);
        assertEquals(restaurant.getId(), response.getId());
        assertEquals(restaurantRequest.getName(), response.getName());
        assertEquals(ownerId, response.getOwnerId());

        verify(restaurantRepository, times(1)).save(any(Restaurant.class));
    }

    @Test
    @DisplayName("Should get a restaurant by ID successfully")
    void getRestaurantById_Success() {
        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));

        RestaurantResponse response = restaurantService.getRestaurantById(restaurantId);

        assertNotNull(response);
        assertEquals(restaurant.getId(), response.getId());
        assertEquals(restaurant.getName(), response.getName());

        verify(restaurantRepository, times(1)).findById(restaurantId);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when getting a non-existent restaurant by ID")
    void getRestaurantById_NotFound() {
        when(restaurantRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        EntityNotFoundException thrown = assertThrows(EntityNotFoundException.class, () ->
                restaurantService.getRestaurantById(UUID.randomUUID())
        );

        assertTrue(thrown.getMessage().contains("Restaurant not found with ID:"));

        verify(restaurantRepository, times(1)).findById(any(UUID.class));
    }

    @Test
    @DisplayName("Should get all restaurants successfully")
    void getAllRestaurants_Success() {
        when(restaurantRepository.findAll()).thenReturn(Arrays.asList(restaurant));

        List<RestaurantResponse> responses = restaurantService.getAllRestaurants();

        assertNotNull(responses);
        assertFalse(responses.isEmpty());
        assertEquals(1, responses.size());
        assertEquals(restaurant.getId(), responses.get(0).getId());

        verify(restaurantRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should update a restaurant successfully")
    void updateRestaurant_Success() {
        RestaurantRequest updatedRequest = RestaurantRequest.builder()
                .name("Updated Restaurant Name")
                .address("456 New St")
                .phoneNumber("987-654-3210")
                .email("updated@example.com")
                .build();

        when(restaurantRepository.findByIdAndOwnerId(restaurantId, ownerId)).thenReturn(Optional.of(restaurant));
        when(restaurantRepository.save(any(Restaurant.class))).thenReturn(restaurant);

        RestaurantResponse response = restaurantService.updateRestaurant(restaurantId, updatedRequest, ownerId);

        assertNotNull(response);
        assertEquals(restaurantId, response.getId());
        assertEquals(updatedRequest.getName(), response.getName());
        assertEquals(updatedRequest.getAddress(), response.getAddress());
        assertEquals(updatedRequest.getPhoneNumber(), response.getPhoneNumber());
        assertEquals(updatedRequest.getEmail(), response.getEmail());

        verify(restaurantRepository, times(1)).findByIdAndOwnerId(restaurantId, ownerId);
        verify(restaurantRepository, times(1)).save(any(Restaurant.class));
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when updating a non-existent restaurant")
    void updateRestaurant_NotFound() {
        when(restaurantRepository.findByIdAndOwnerId(any(UUID.class), any(UUID.class))).thenReturn(Optional.empty());

        EntityNotFoundException thrown = assertThrows(EntityNotFoundException.class, () ->
                restaurantService.updateRestaurant(UUID.randomUUID(), restaurantRequest, ownerId)
        );

        assertTrue(thrown.getMessage().contains("Restaurant not found or not owned by you with ID:"));

        verify(restaurantRepository, times(1)).findByIdAndOwnerId(any(UUID.class), any(UUID.class));
        verify(restaurantRepository, never()).save(any(Restaurant.class));
    }

    @Test
    @DisplayName("Should delete a restaurant successfully")
    void deleteRestaurant_Success() {
        when(restaurantRepository.existsByIdAndOwnerId(restaurantId, ownerId)).thenReturn(true);
        doNothing().when(restaurantRepository).deleteById(restaurantId);

        assertDoesNotThrow(() -> restaurantService.deleteRestaurant(restaurantId, ownerId));

        verify(restaurantRepository, times(1)).existsByIdAndOwnerId(restaurantId, ownerId);
        verify(restaurantRepository, times(1)).deleteById(restaurantId);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when deleting a non-existent restaurant")
    void deleteRestaurant_NotFound() {
        when(restaurantRepository.existsByIdAndOwnerId(any(UUID.class), any(UUID.class))).thenReturn(false);

        EntityNotFoundException thrown = assertThrows(EntityNotFoundException.class, () ->
                restaurantService.deleteRestaurant(UUID.randomUUID(), ownerId)
        );

        assertTrue(thrown.getMessage().contains("Restaurant not found or not owned by you with ID:"));

        verify(restaurantRepository, times(1)).existsByIdAndOwnerId(any(UUID.class), any(UUID.class));
        verify(restaurantRepository, never()).deleteById(any(UUID.class));
    }

    @Test
    @DisplayName("Should get restaurants owned by user successfully")
    void getRestaurantsByOwner_Success() {
        when(restaurantRepository.findByOwnerId(ownerId)).thenReturn(Arrays.asList(restaurant));

        List<RestaurantResponse> responses = restaurantService.getRestaurantsByOwner(ownerId);

        assertNotNull(responses);
        assertFalse(responses.isEmpty());
        assertEquals(1, responses.size());
        assertEquals(restaurant.getId(), responses.get(0).getId());
        assertEquals(ownerId, responses.get(0).getOwnerId());

        verify(restaurantRepository, times(1)).findByOwnerId(ownerId);
    }

    @Test
    @DisplayName("Should return empty list when no restaurants are owned by user")
    void getRestaurantsByOwner_NoRestaurants() {
        when(restaurantRepository.findByOwnerId(ownerId)).thenReturn(List.of());

        List<RestaurantResponse> responses = restaurantService.getRestaurantsByOwner(ownerId);

        assertNotNull(responses);
        assertTrue(responses.isEmpty());

        verify(restaurantRepository, times(1)).findByOwnerId(ownerId);
    }
}
