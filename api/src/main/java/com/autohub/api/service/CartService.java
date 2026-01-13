package com.autohub.api.service;

import com.autohub.api.model.Cart;
import com.autohub.api.model.CartItem;
import com.autohub.api.model.Part;
import com.autohub.api.model.User;
import com.autohub.api.repository.CartItemRepository;
import com.autohub.api.repository.CartRepository;
import com.autohub.api.repository.PartRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final PartRepository partRepository;

    public CartService(CartRepository cartRepository, CartItemRepository cartItemRepository, PartRepository partRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.partRepository = partRepository;
    }

    /**
     * AUDIT #2.1: Retrieve the user's cart or create a new one.
     */
    public Cart getCart(User user) {
        return cartRepository.findByUser(user)
                .orElseGet(() -> cartRepository.save(new Cart(user)));
    }

    /**
     * AUDIT #2.2: Add item to cart with stock validation.
     */
    @Transactional
    public Cart addItemToCart(User user, Long partId, Integer quantity) {
        Cart cart = getCart(user);
        Part part = partRepository.findById(partId)
                .orElseThrow(() -> new RuntimeException("Part not found with ID: " + partId));

        // NEW: Phase 3 Stock Validation
        // Ensures Mike doesn't oversell parts that aren't in the warehouse.
        if (part.getStockQuantity() < quantity) {
            throw new RuntimeException("Insufficient stock. Only " + part.getStockQuantity() + " units available.");
        }

        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getPart().getId().equals(partId))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            int newQuantity = item.getQuantity() + quantity;

            // Re-validate total quantity against stock
            if (part.getStockQuantity() < newQuantity) {
                throw new RuntimeException("Cannot add more. Total in cart would exceed stock levels.");
            }
            item.setQuantity(newQuantity);
        } else {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setPart(part);
            newItem.setQuantity(quantity);
            cart.getItems().add(newItem);
        }

        return cartRepository.save(cart);
    }

    /**
     * AUDIT #2.3: Remove specific item.
     */
    @Transactional
    public Cart removeItemFromCart(User user, Long cartItemId) {
        Cart cart = getCart(user);
        cart.getItems().removeIf(item -> item.getId().equals(cartItemId));
        return cartRepository.save(cart);
    }

    /**
     * AUDIT #2.4: Clear cart entirely.
     */
    @Transactional
    public void clearCart(User user) {
        Cart cart = getCart(user);
        cart.getItems().clear();
        cartRepository.save(cart);
    }
}