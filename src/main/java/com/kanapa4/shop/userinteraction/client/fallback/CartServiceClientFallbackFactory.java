package com.kanapa4.shop.userinteraction.client.fallback;

import com.kanapa4.shop.userinteraction.client.CartServiceClient;
import com.kanapa4.shop.userinteraction.dto.AddCartItemRequest;
import com.kanapa4.shop.userinteraction.dto.CartResponse;
import com.kanapa4.shop.userinteraction.dto.RepriceResponse;
import com.kanapa4.shop.userinteraction.dto.UpdateQuantityRequest;
import com.kanapa4.shop.userinteraction.exception.DownstreamServiceUnavailableException;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class CartServiceClientFallbackFactory implements FallbackFactory<CartServiceClient> {

    private static final String SERVICE_NAME = "cart-service";

    @Override
    public CartServiceClient create(Throwable cause) {
        return new CartServiceClient() {
            @Override
            public CartResponse getCart(UUID userId) {
                log.error("Fallback for {}.getCart (userId={}): {}", SERVICE_NAME, userId, cause.toString());
                throw failure(cause);
            }

            @Override
            public RepriceResponse reprice(UUID userId) {
                log.error("Fallback for {}.reprice (userId={}): {}", SERVICE_NAME, userId, cause.toString());
                throw failure(cause);
            }

            @Override
            public CartResponse addItem(UUID userId, AddCartItemRequest request) {
                log.error("Fallback for {}.addItem (userId={}, product={}): {}", SERVICE_NAME, userId, request.productId(), cause.toString());
                throw failure(cause);
            }

            @Override
            public CartResponse updateQuantity(UUID userId, UUID itemId, UpdateQuantityRequest request) {
                log.error("Fallback for {}.updateQuantity (userId={}, item={}): {}", SERVICE_NAME, userId, itemId, cause.toString());
                throw failure(cause);
            }

            @Override
            public void removeItem(UUID userId, UUID itemId) {
                log.error("Fallback for {}.removeItem (userId={}, item={}): {}", SERVICE_NAME, userId, itemId, cause.toString());
                throw failure(cause);
            }

            @Override
            public void clear(UUID userId) {
                log.error("Fallback for {}.clear (userId={}): {}", SERVICE_NAME, userId, cause.toString());
                throw failure(cause);
            }
        };
    }

    private RuntimeException failure(Throwable cause) {
        Throwable actual = cause;
        while (actual.getCause() != null && !(actual instanceof FeignException)) {
            actual = actual.getCause();
        }
        if (actual instanceof FeignException feignEx && feignEx.status() >= 400 && feignEx.status() < 500) {
            return feignEx;
        }
        return new DownstreamServiceUnavailableException(SERVICE_NAME);
    }
}
