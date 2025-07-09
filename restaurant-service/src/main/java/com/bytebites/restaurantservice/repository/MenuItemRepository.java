package com.bytebites.restaurantservice.repository;

import com.bytebites.restaurantservice.model.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, UUID> {
    List<MenuItem> findByRestaurantId(UUID restaurantId);
    Optional<MenuItem> findByIdAndRestaurantId(UUID menuItemId, UUID restaurantId);
    boolean existsByIdAndRestaurantId(UUID menuItemId, UUID restaurantId);
}