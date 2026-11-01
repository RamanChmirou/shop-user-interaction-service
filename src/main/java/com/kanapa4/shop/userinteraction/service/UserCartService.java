package com.kanapa4.shop.userinteraction.service;

import com.kanapa4.shop.userinteraction.client.CartServiceClient;
import com.kanapa4.shop.userinteraction.dto.AddCartItemRequest;
import com.kanapa4.shop.userinteraction.dto.CartResponse;
import com.kanapa4.shop.userinteraction.dto.RepriceResponse;
import com.kanapa4.shop.userinteraction.dto.UpdateQuantityRequest;
import com.kanapa4.shop.userinteraction.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserCartService {

    private final CartServiceClient cartServiceClient;
    private final CurrentUser currentUser;

    public CartResponse getCart() {
        UUID userId = currentUser.id();
        log.info("Fetching cart for user={}", userId);
        return cartServiceClient.getCart(userId);
    }

    public CartResponse addItem(AddCartItemRequest request) {
        UUID userId = currentUser.id();
        log.info("Adding item to cart for user={}, productId={}", userId, request.productId());
        return cartServiceClient.addItem(userId, request);
    }

    public CartResponse updateQuantity(UUID itemId, UpdateQuantityRequest request) {
        UUID userId = currentUser.id();
        log.info("Updating quantity for user={}, itemId={}, quantity={}", userId, itemId, request.quantity());
        return cartServiceClient.updateQuantity(userId, itemId, request);
    }

    public void removeItem(UUID itemId) {
        UUID userId = currentUser.id();
        log.info("Removing item from cart for user={}, itemId={}", userId, itemId);
        cartServiceClient.removeItem(userId, itemId);
    }

    public void clear() {
        UUID userId = currentUser.id();
        log.info("Clearing cart for user={}", userId);
        cartServiceClient.clear(userId);
    }

    public RepriceResponse reprice() {
        UUID userId = currentUser.id();
        log.info("Repricing cart for user={}", userId);
        return cartServiceClient.reprice(userId);
    }
}
