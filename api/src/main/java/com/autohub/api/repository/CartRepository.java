package com.autohub.api.repository;

import com.autohub.api.model.Cart;
import com.autohub.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    /**
     * OPTIMIZATION: Uses JOIN FETCH to load the cart and its items in one query.
     * This prevents the N+1 performance issue when displaying the cart.
     */
    @Query("SELECT c FROM Cart c LEFT JOIN FETCH c.items WHERE c.user = :user")
    Optional<Cart> findByUser(@Param("user") User user);
}