package com.kanapa4.shop.userinteraction.client.fallback;

import com.kanapa4.shop.userinteraction.client.OrderServiceClient;
import com.kanapa4.shop.userinteraction.dto.OrderCheckoutRequest;
import com.kanapa4.shop.userinteraction.dto.OrderPageResponse;
import com.kanapa4.shop.userinteraction.dto.OrderResponse;
import com.kanapa4.shop.userinteraction.dto.OrderSummaryResponse;
import com.kanapa4.shop.userinteraction.exception.DownstreamServiceUnavailableException;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class OrderServiceClientFallbackFactory implements FallbackFactory<OrderServiceClient> {

    private static final String SERVICE_NAME = "order-service";

    @Override
    public OrderServiceClient create(Throwable cause) {
        return new OrderServiceClient() {
            @Override
            public OrderResponse checkout(UUID userId, OrderCheckoutRequest request) {
                log.error("Fallback for {}.checkout (userId={}): {}", SERVICE_NAME, userId, cause.toString());
                throw failure(cause);
            }

            @Override
            public OrderPageResponse<OrderSummaryResponse> getOrders(UUID userId, int page, int size, String sortBy, String direction) {
                log.error("Fallback for {}.getOrders (userId={}): {}", SERVICE_NAME, userId, cause.toString());
                throw failure(cause);
            }

            @Override
            public OrderResponse getOrder(UUID userId, UUID orderId) {
                log.error("Fallback for {}.getOrder (userId={}, orderId={}): {}", SERVICE_NAME, userId, orderId, cause.toString());
                throw failure(cause);
            }

            @Override
            public OrderResponse pay(UUID userId, UUID orderId) {
                log.error("Fallback for {}.pay (userId={}, orderId={}): {}", SERVICE_NAME, userId, orderId, cause.toString());
                throw failure(cause);
            }

            @Override
            public OrderResponse cancel(UUID userId, UUID orderId) {
                log.error("Fallback for {}.cancel (userId={}, orderId={}): {}", SERVICE_NAME, userId, orderId, cause.toString());
                throw failure(cause);
            }

            @Override
            public OrderResponse retryInvoice(UUID userId, UUID orderId) {
                log.error("Fallback for {}.retryInvoice (userId={}, orderId={}): {}", SERVICE_NAME, userId, orderId, cause.toString());
                throw failure(cause);
            }

            @Override
            public OrderResponse ship(UUID orderId) {
                log.error("Fallback for {}.ship (orderId={}): {}", SERVICE_NAME, orderId, cause.toString());
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
