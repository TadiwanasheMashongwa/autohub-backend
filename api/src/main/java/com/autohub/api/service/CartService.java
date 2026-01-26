package com.autohub.api.service;

import com.autohub.api.model.Cart;
import com.autohub.api.model.CartItem;
import com.autohub.api.model.Part;
import com.autohub.api.model.User;
import com.autohub.api.repository.CartRepository;
import com.autohub.api.repository.PartRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final PartRepository partRepository;

    public CartService(CartRepository cartRepository, PartRepository partRepository) {
        this.cartRepository = cartRepository;
        this.partRepository = partRepository;
    }

    public Cart getCart(User user) {
        return cartRepository.findByUser(user)
                .orElseGet(() -> cartRepository.save(new Cart(user)));
    }

    @Transactional
    public Cart addItemToCart(User user, Long partId, Integer quantity) {
        Cart cart = getCart(user);
        Part part = partRepository.findById(partId)
                .orElseThrow(() -> new RuntimeException("Part not found with ID: " + partId));

        // Initial stock check
        if (part.getStockQuantity() < quantity) {
            throw new RuntimeException("Not enough stock for " + part.getName());
        }

        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getPart().getId().equals(partId))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            int finalQuantity = item.getQuantity() + quantity;

            if (part.getStockQuantity() < finalQuantity) {
                throw new RuntimeException("Adding " + quantity + " more would exceed available stock.");
            }
            item.setQuantity(finalQuantity);
        } else {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setPart(part);
            newItem.setQuantity(quantity);
            cart.getItems().add(newItem);
        }

        return cartRepository.save(cart);
    }

    @Transactional
    public Cart updateItemQuantity(User user, Long cartItemId, Integer newQuantity) {
        Cart cart = getCart(user);
        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Item not in cart"));

        if (item.getPart().getStockQuantity() < newQuantity) {
            throw new RuntimeException("Requested quantity exceeds stock.");
        }

        if (newQuantity <= 0) {
            cart.getItems().remove(item);
        } else {
            item.setQuantity(newQuantity);
        }

        return cartRepository.save(cart);
    }

    @Transactional
    public Cart removeItemFromCart(User user, Long cartItemId) {
        Cart cart = getCart(user);
        cart.getItems().removeIf(item -> item.getId().equals(cartItemId));
        return cartRepository.save(cart);
    }

    @Transactional
    public void clearCart(User user) {
        Cart cart = getCart(user);
        cart.getItems().clear();
        cartRepository.save(cart);
    }
}