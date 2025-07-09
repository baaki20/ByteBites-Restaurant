package com.bytebites.restaurantservice.service;

import com.bytebites.restaurantservice.dto.MenuItemRequest;
import com.bytebites.restaurantservice.dto.MenuItemResponse;
import com.bytebites.restaurantservice.model.MenuItem;
import com.bytebites.restaurantservice.model.Restaurant;
import com.bytebites.restaurantservice.repository.MenuItemRepository;
import com.bytebites.restaurantservice.repository.RestaurantRepository;
import com.bytebites.restaurantservice.service.impl.MenuItemServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MenuItemServiceImplTest {

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    @InjectMocks
    private MenuItemServiceImpl menuItemService;

    private UUID ownerId;
    private UUID restaurantId;
    private UUID menuItemId;
    private Restaurant restaurant;
    private MenuItemRequest menuItemRequest;
    private MenuItem menuItem;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        restaurantId = UUID.randomUUID();
        menuItemId = UUID.randomUUID();

        restaurant = Restaurant.builder()
                .id(restaurantId)
                .name("Test Restaurant")
                .ownerId(ownerId)
                .build();

        menuItemRequest = MenuItemRequest.builder()
                .name("Burger")
                .description("Delicious beef burger")
                .price(new BigDecimal("12.99"))
                .available(true)
                .build();

        menuItem = MenuItem.builder()
                .id(menuItemId)
                .name("Burger")
                .description("Delicious beef burger")
                .price(new BigDecimal("12.99"))
                .available(true)
                .restaurant(restaurant)
                .build();
    }

    @Test
    @DisplayName("Should create a menu item successfully")
    void createMenuItem_Success() {
        when(restaurantRepository.findByIdAndOwnerId(restaurantId, ownerId)).thenReturn(Optional.of(restaurant));
        when(menuItemRepository.save(any(MenuItem.class))).thenReturn(menuItem);

        MenuItemResponse response = menuItemService.createMenuItem(restaurantId, menuItemRequest, ownerId);

        assertNotNull(response);
        assertEquals(menuItemId, response.getId());
        assertEquals(menuItemRequest.getName(), response.getName());
        assertEquals(restaurantId, response.getRestaurantId());

        verify(restaurantRepository, times(1)).findByIdAndOwnerId(restaurantId, ownerId);
        verify(menuItemRepository, times(1)).save(any(MenuItem.class));
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when creating menu item for non-existent restaurant")
    void createMenuItem_RestaurantNotFound() {
        when(restaurantRepository.findByIdAndOwnerId(any(UUID.class), any(UUID.class))).thenReturn(Optional.empty());

        EntityNotFoundException thrown = assertThrows(EntityNotFoundException.class, () ->
                menuItemService.createMenuItem(UUID.randomUUID(), menuItemRequest, ownerId)
        );

        assertTrue(thrown.getMessage().contains("Restaurant not found or not owned by you with ID:"));

        verify(restaurantRepository, times(1)).findByIdAndOwnerId(any(UUID.class), any(UUID.class));
        verify(menuItemRepository, never()).save(any(MenuItem.class));
    }

    @Test
    @DisplayName("Should get a menu item by ID successfully")
    void getMenuItemById_Success() {
        when(menuItemRepository.findByIdAndRestaurantId(menuItemId, restaurantId)).thenReturn(Optional.of(menuItem));

        MenuItemResponse response = menuItemService.getMenuItemById(restaurantId, menuItemId);

        assertNotNull(response);
        assertEquals(menuItemId, response.getId());
        assertEquals(menuItem.getName(), response.getName());

        verify(menuItemRepository, times(1)).findByIdAndRestaurantId(menuItemId, restaurantId);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when getting a non-existent menu item by ID")
    void getMenuItemById_NotFound() {
        when(menuItemRepository.findByIdAndRestaurantId(any(UUID.class), any(UUID.class))).thenReturn(Optional.empty());

        EntityNotFoundException thrown = assertThrows(EntityNotFoundException.class, () ->
                menuItemService.getMenuItemById(restaurantId, UUID.randomUUID())
        );

        assertTrue(thrown.getMessage().contains("Menu item not found with ID:"));

        verify(menuItemRepository, times(1)).findByIdAndRestaurantId(any(UUID.class), any(UUID.class));
    }

    @Test
    @DisplayName("Should get all menu items for a restaurant successfully")
    void getMenuItemsByRestaurant_Success() {
        when(restaurantRepository.existsById(restaurantId)).thenReturn(true);
        when(menuItemRepository.findByRestaurantId(restaurantId)).thenReturn(Arrays.asList(menuItem));

        List<MenuItemResponse> responses = menuItemService.getMenuItemsByRestaurant(restaurantId);

        assertNotNull(responses);
        assertFalse(responses.isEmpty());
        assertEquals(1, responses.size());
        assertEquals(menuItemId, responses.get(0).getId());

        verify(restaurantRepository, times(1)).existsById(restaurantId);
        verify(menuItemRepository, times(1)).findByRestaurantId(restaurantId);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when getting menu items for a non-existent restaurant")
    void getMenuItemsByRestaurant_RestaurantNotFound() {
        when(restaurantRepository.existsById(any(UUID.class))).thenReturn(false);

        EntityNotFoundException thrown = assertThrows(EntityNotFoundException.class, () ->
                menuItemService.getMenuItemsByRestaurant(UUID.randomUUID())
        );

        assertTrue(thrown.getMessage().contains("Restaurant not found with ID:"));

        verify(restaurantRepository, times(1)).existsById(any(UUID.class));
        verify(menuItemRepository, never()).findByRestaurantId(any(UUID.class));
    }

    @Test
    @DisplayName("Should update a menu item successfully")
    void updateMenuItem_Success() {
        MenuItemRequest updatedRequest = MenuItemRequest.builder()
                .name("Updated Burger")
                .description("New and improved burger")
                .price(new BigDecimal("15.50"))
                .available(false)
                .build();

        when(restaurantRepository.findByIdAndOwnerId(restaurantId, ownerId)).thenReturn(Optional.of(restaurant));
        when(menuItemRepository.findByIdAndRestaurantId(menuItemId, restaurantId)).thenReturn(Optional.of(menuItem));
        when(menuItemRepository.save(any(MenuItem.class))).thenReturn(menuItem);

        MenuItemResponse response = menuItemService.updateMenuItem(restaurantId, menuItemId, updatedRequest, ownerId);

        assertNotNull(response);
        assertEquals(menuItemId, response.getId());
        assertEquals(updatedRequest.getName(), response.getName());
        assertEquals(updatedRequest.getDescription(), response.getDescription());
        assertEquals(updatedRequest.getPrice(), response.getPrice());
        assertEquals(updatedRequest.isAvailable(), response.isAvailable());

        verify(restaurantRepository, times(1)).findByIdAndOwnerId(restaurantId, ownerId);
        verify(menuItemRepository, times(1)).findByIdAndRestaurantId(menuItemId, restaurantId);
        verify(menuItemRepository, times(1)).save(any(MenuItem.class));
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when updating menu item for non-existent restaurant")
    void updateMenuItem_RestaurantNotFound() {
        when(restaurantRepository.findByIdAndOwnerId(any(UUID.class), any(UUID.class))).thenReturn(Optional.empty());

        EntityNotFoundException thrown = assertThrows(EntityNotFoundException.class, () ->
                menuItemService.updateMenuItem(UUID.randomUUID(), menuItemId, menuItemRequest, ownerId)
        );

        assertTrue(thrown.getMessage().contains("Restaurant not found or not owned by you with ID:"));

        verify(restaurantRepository, times(1)).findByIdAndOwnerId(any(UUID.class), any(UUID.class));
        verify(menuItemRepository, never()).findByIdAndRestaurantId(any(UUID.class), any(UUID.class));
        verify(menuItemRepository, never()).save(any(MenuItem.class));
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when updating a non-existent menu item")
    void updateMenuItem_MenuItemNotFound() {
        when(restaurantRepository.findByIdAndOwnerId(restaurantId, ownerId)).thenReturn(Optional.of(restaurant));
        when(menuItemRepository.findByIdAndRestaurantId(any(UUID.class), any(UUID.class))).thenReturn(Optional.empty());

        EntityNotFoundException thrown = assertThrows(EntityNotFoundException.class, () ->
                menuItemService.updateMenuItem(restaurantId, UUID.randomUUID(), menuItemRequest, ownerId)
        );

        assertTrue(thrown.getMessage().contains("Menu item not found with ID:"));

        verify(restaurantRepository, times(1)).findByIdAndOwnerId(restaurantId, ownerId);
        verify(menuItemRepository, times(1)).findByIdAndRestaurantId(any(UUID.class), any(UUID.class));
        verify(menuItemRepository, never()).save(any(MenuItem.class));
    }

    @Test
    @DisplayName("Should delete a menu item successfully")
    void deleteMenuItem_Success() {
        when(restaurantRepository.existsByIdAndOwnerId(restaurantId, ownerId)).thenReturn(true);
        when(menuItemRepository.existsByIdAndRestaurantId(menuItemId, restaurantId)).thenReturn(true);
        doNothing().when(menuItemRepository).deleteById(menuItemId);

        assertDoesNotThrow(() -> menuItemService.deleteMenuItem(restaurantId, menuItemId, ownerId));

        verify(restaurantRepository, times(1)).existsByIdAndOwnerId(restaurantId, ownerId);
        verify(menuItemRepository, times(1)).existsByIdAndRestaurantId(menuItemId, restaurantId);
        verify(menuItemRepository, times(1)).deleteById(menuItemId);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when deleting menu item for non-existent restaurant")
    void deleteMenuItem_RestaurantNotFound() {
        when(restaurantRepository.existsByIdAndOwnerId(any(UUID.class), any(UUID.class))).thenReturn(false);

        EntityNotFoundException thrown = assertThrows(EntityNotFoundException.class, () ->
                menuItemService.deleteMenuItem(UUID.randomUUID(), menuItemId, ownerId)
        );

        assertTrue(thrown.getMessage().contains("Restaurant not found or not owned by you with ID:"));

        verify(restaurantRepository, times(1)).existsByIdAndOwnerId(any(UUID.class), any(UUID.class));
        verify(menuItemRepository, never()).existsByIdAndRestaurantId(any(UUID.class), any(UUID.class));
        verify(menuItemRepository, never()).deleteById(any(UUID.class));
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when deleting a non-existent menu item")
    void deleteMenuItem_MenuItemNotFound() {
        when(restaurantRepository.existsByIdAndOwnerId(restaurantId, ownerId)).thenReturn(true);
        when(menuItemRepository.existsByIdAndRestaurantId(any(UUID.class), any(UUID.class))).thenReturn(false);

        EntityNotFoundException thrown = assertThrows(EntityNotFoundException.class, () ->
                menuItemService.deleteMenuItem(restaurantId, UUID.randomUUID(), ownerId)
        );

        assertTrue(thrown.getMessage().contains("Menu item not found with ID:"));

        verify(restaurantRepository, times(1)).existsByIdAndOwnerId(restaurantId, ownerId);
        verify(menuItemRepository, times(1)).existsByIdAndRestaurantId(any(UUID.class), any(UUID.class));
        verify(menuItemRepository, never()).deleteById(any(UUID.class));
    }
}
