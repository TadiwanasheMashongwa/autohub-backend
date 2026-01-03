package com.autohub.api.controller;

import com.autohub.api.model.Cart;
import com.autohub.api.model.Order;
import com.autohub.api.model.User;
import com.autohub.api.repository.UserRepository;
import com.autohub.api.service.CartService;
import com.autohub.api.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;
    private final OrderService orderService;
    private final UserRepository userRepository;

    public CartController(CartService cartService, OrderService orderService, UserRepository userRepository) {
        this.cartService = cartService;
        this.orderService = orderService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<Cart> getCart(Authentication authentication) {
        User user = getUserFromAuth(authentication);
        return ResponseEntity.ok(cartService.getCart(user));
    }

    @PostMapping("/add")
    public ResponseEntity<Cart> addItem(@RequestParam Long partId, @RequestParam Integer quantity, Authentication authentication) {
        User user = getUserFromAuth(authentication);
        return ResponseEntity.ok(cartService.addItemToCart(user, partId, quantity));
    }

    @DeleteMapping("/item/{id}")
    public ResponseEntity<Cart> removeItem(@PathVariable Long id, Authentication authentication) {
        User user = getUserFromAuth(authentication);
        return ResponseEntity.ok(cartService.removeItemFromCart(user, id));
    }

    /**
     * Checkout the cart and create a PENDING order.
     * @param idempotencyKey Required to prevent duplicate orders during network retries.
     */
    @PostMapping("/checkout")
    public ResponseEntity<Order> checkout(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            Authentication authentication) {
        User user = getUserFromAuth(authentication);
        // FIXED: Now passing the required String argument to OrderService
        return ResponseEntity.ok(orderService.checkoutCart(user, idempotencyKey));
    }

    private User getUserFromAuth(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}