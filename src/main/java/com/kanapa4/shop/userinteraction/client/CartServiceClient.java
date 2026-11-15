package com.kanapa4.shop.userinteraction.client;

import com.kanapa4.shop.userinteraction.dto.AddCartItemRequest;
import com.kanapa4.shop.userinteraction.dto.CartResponse;
import com.kanapa4.shop.userinteraction.dto.RepriceResponse;
import com.kanapa4.shop.userinteraction.dto.UpdateQuantityRequest;
import com.kanapa4.shop.userinteraction.client.config.FeignClientConfig;
import com.kanapa4.shop.userinteraction.client.fallback.CartServiceClientFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(
        name = "cart-service",
        url = "${CART_SERVICE_URL:http://localhost:8082}",
        configuration = FeignClientConfig.class,
        fallbackFactory = CartServiceClientFallbackFactory.class
)
public interface CartServiceClient {


    @GetMapping("/carts/{userId}")
    CartResponse getCart(@PathVariable("userId") UUID userId);

    @PostMapping("/carts/{userId}/reprice")
    RepriceResponse reprice(@PathVariable("userId") UUID userId);

    @PostMapping("/carts/{userId}/items")
    CartResponse addItem(@PathVariable("userId") UUID userId, @RequestBody AddCartItemRequest request);

    @PatchMapping("/carts/{userId}/items/{itemId}")
    CartResponse updateQuantity(
            @PathVariable("userId") UUID userId,
            @PathVariable("itemId") UUID itemId,
            @RequestBody UpdateQuantityRequest request);

    @DeleteMapping("/carts/{userId}/items/{itemId}")
    void removeItem(@PathVariable("userId") UUID userId, @PathVariable("itemId") UUID itemId);

    @DeleteMapping("/carts/{userId}")
    void clear(@PathVariable("userId") UUID userId);
}
