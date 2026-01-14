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

    public CartService(
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            PartRepository partRepository
    ) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.partRepository = partRepository;
    }

    /**
     * Retrieve the user's cart or create one if it doesn't exist.
     */
    public Cart getCart(User user) {
        return cartRepository.findByUser(user)
                .orElseGet(() -> cartRepository.save(new Cart(user)));
    }

    /**
     * Add or update an item in the cart.
     * IMPORTANT: Cart expresses intent ONLY.
     * No stock or availability enforcement happens here.
     */
    @Transactional
    public Cart addItemToCart(User user, Long partId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new RuntimeException("Quantity must be greater than zero.");
        }

        Cart cart = getCart(user);

        Part part = partRepository.findById(partId)
                .orElseThrow(() -> new RuntimeException("Part not found with ID: " + partId));

        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getPart().getId().equals(partId))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + quantity);
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
     * Remove a specific item from the cart.
     */
    @Transactional
    public Cart removeItemFromCart(User user, Long cartItemId) {
        Cart cart = getCart(user);
        cart.getItems().removeIf(item -> item.getId().equals(cartItemId));
        return cartRepository.save(cart);
    }

    /**
     * Clear the cart entirely.
     */
    @Transactional
    public void clearCart(User user) {
        Cart cart = getCart(user);
        cart.getItems().clear();
        cartRepository.save(cart);
    }
}
