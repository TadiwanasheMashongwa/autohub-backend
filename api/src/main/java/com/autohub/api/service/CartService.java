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
                .orElseThrow(() -> new RuntimeException("Part not found: " + partId));

        if (part.getStockQuantity() < quantity) {
            throw new RuntimeException("Insufficient stock");
        }

        Optional<CartItem> existing = cart.getItems()
                .stream()
                .filter(i -> i.getPart().getId().equals(partId))
                .findFirst();

        if (existing.isPresent()) {
            int newQty = existing.get().getQuantity() + quantity;
            if (part.getStockQuantity() < newQty) {
                throw new RuntimeException("Stock exceeded");
            }
            existing.get().setQuantity(newQty);
        } else {
            CartItem item = new CartItem();
            item.setCart(cart);
            item.setPart(part);
            item.setQuantity(quantity);
            cart.getItems().add(item);
        }

        return cartRepository.save(cart);
    }

    @Transactional
    public Cart removeItemFromCart(User user, Long cartItemId) {
        Cart cart = getCart(user);
        cart.getItems().removeIf(i -> i.getId().equals(cartItemId));
        return cartRepository.save(cart);
    }

    @Transactional
    public void clearCart(User user) {
        Cart cart = getCart(user);
        cart.getItems().clear();
        cartRepository.save(cart);
    }
}
