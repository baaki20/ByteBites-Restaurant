package com.bytebites.orderservice.repository;

import com.bytebites.orderservice.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {

    List<Order> findByUserEmail(String userEmail);

    List<Order> findByRestaurantId(String restaurantId);

    Optional<Order> findByIdAndUserEmail(String id, String userEmail);
}