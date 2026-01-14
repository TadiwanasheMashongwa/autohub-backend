package com.autohub.api.controller;

import com.autohub.api.model.Cart;
import com.autohub.api.model.User;
import com.autohub.api.repository.UserRepository;
import com.autohub.api.service.CartService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
@PreAuthorize("hasRole('CUSTOMER')")
public class CartController {

    private final CartService cartService;
    private final UserRepository userRepository;

    public CartController(CartService cartService, UserRepository userRepository) {
        this.cartService = cartService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<Cart> getCart(Authentication authentication) {
        return ResponseEntity.ok(cartService.getCart(getUser(authentication)));
    }

    @PostMapping("/add")
    public ResponseEntity<Cart> addItem(
            @RequestParam Long partId,
            @RequestParam Integer quantity,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                cartService.addItemToCart(getUser(authentication), partId, quantity)
        );
    }

    @DeleteMapping("/item/{id}")
    public ResponseEntity<Cart> removeItem(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                cartService.removeItemFromCart(getUser(authentication), id)
        );
    }

    @DeleteMapping("/clear")
    public ResponseEntity<Void> clearCart(Authentication authentication) {
        cartService.clearCart(getUser(authentication));
        return ResponseEntity.noContent().build();
    }

    private User getUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new RuntimeException("User not found with email: " + authentication.getName())
                );
    }
}
