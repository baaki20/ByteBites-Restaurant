package com.bytebites.restaurantservice.repository;

import com.bytebites.restaurantservice.model.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, UUID> {
    List<Restaurant> findByOwnerId(UUID ownerId);
    Optional<Restaurant> findByIdAndOwnerId(UUID restaurantId, UUID ownerId);
    boolean existsByIdAndOwnerId(UUID id, UUID ownerId);
}