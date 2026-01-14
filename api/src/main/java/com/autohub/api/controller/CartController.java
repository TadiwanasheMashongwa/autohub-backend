package com.autohub.api.controller;

import com.autohub.api.model.Cart;
import com.autohub.api.model.Order;
import com.autohub.api.model.User;
import com.autohub.api.repository.UserRepository;
import com.autohub.api.service.CartService;
import com.autohub.api.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
@PreAuthorize("hasRole('CUSTOMER')")
public class CartController {

    private final CartService cartService;
    private final OrderService orderService;
    private final UserRepository userRepository;

    public CartController(CartService cartService, OrderService orderService, UserRepository userRepository) {
        this.cartService = cartService;
        this.orderService = orderService;
        this.userRepository = userRepository;
    }

    /**
     * AUDIT #2.1: View active cart.
     */
    @GetMapping
    public ResponseEntity<Cart> getCart(Authentication authentication) {
        User user = getUserFromAuth(authentication);
        return ResponseEntity.ok(cartService.getCart(user));
    }

    /**
     * AUDIT #2.2: Add part to cart or update quantity.
     */
    @PostMapping("/add")
    public ResponseEntity<Cart> addItem(@RequestParam Long partId, @RequestParam Integer quantity, Authentication authentication) {
        User user = getUserFromAuth(authentication);
        return ResponseEntity.ok(cartService.addItemToCart(user, partId, quantity));
    }

    /**
     * AUDIT #2.3: Remove specific item from cart.
     */
    @DeleteMapping("/item/{id}")
    public ResponseEntity<Cart> removeItem(@PathVariable Long id, Authentication authentication) {
        User user = getUserFromAuth(authentication);
        return ResponseEntity.ok(cartService.removeItemFromCart(user, id));
    }

    /**
     * AUDIT #2.4: Clear entire cart.
     * New: Synchronized with Audit Item #2.4 in your Postman collection.
     */
    @DeleteMapping("/clear")
    public ResponseEntity<Void> clearCart(Authentication authentication) {
        User user = getUserFromAuth(authentication);
        cartService.clearCart(user);
        return ResponseEntity.noContent().build();
    }

    /**
     * AUDIT #1.6: Checkout cart and create order.
     * Uses Idempotency-Key to prevent duplicate charges.
     */
    @PostMapping("/checkout")
    public ResponseEntity<Order> checkout(@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey, Authentication authentication) {
        User user = getUserFromAuth(authentication);
        return ResponseEntity.ok(orderService.checkoutCart(user, idempotencyKey));
    }

    private User getUserFromAuth(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found with email: " + authentication.getName()));
    }
}