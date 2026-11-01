package com.kanapa4.shop.userinteraction.service;

import com.kanapa4.shop.userinteraction.client.OrderServiceClient;
import com.kanapa4.shop.userinteraction.dto.OrderPageResponse;
import com.kanapa4.shop.userinteraction.dto.OrderResponse;
import com.kanapa4.shop.userinteraction.dto.OrderSummaryResponse;
import com.kanapa4.shop.userinteraction.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserOrderService {

    private final OrderServiceClient orderServiceClient;
    private final CurrentUser currentUser;

    public OrderPageResponse<OrderSummaryResponse> getOrders(int page, int size, String sortBy, String direction) {
        UUID userId = currentUser.id();
        log.info("Fetching orders for user={}, page={}, size={}, sortBy={}, direction={}", userId, page, size, sortBy, direction);
        return orderServiceClient.getOrders(userId, page, size, sortBy, direction);
    }

    public OrderResponse getOrder(UUID orderId) {
        UUID userId = currentUser.id();
        log.info("Fetching order id={} for user={}", orderId, userId);
        return orderServiceClient.getOrder(userId, orderId);
    }

    public OrderResponse pay(UUID orderId) {
        UUID userId = currentUser.id();
        log.info("Paying order id={} for user={}", orderId, userId);
        return orderServiceClient.pay(userId, orderId);
    }

    public OrderResponse cancel(UUID orderId) {
        UUID userId = currentUser.id();
        log.info("Cancelling order id={} for user={}", orderId, userId);
        return orderServiceClient.cancel(userId, orderId);
    }

    public OrderResponse retryInvoice(UUID orderId) {
        UUID userId = currentUser.id();
        log.info("Retrying invoice for order id={}, user={}", orderId, userId);
        return orderServiceClient.retryInvoice(userId, orderId);
    }

    public OrderResponse ship(UUID orderId) {
        log.info("Shipping order id={}", orderId);
        return orderServiceClient.ship(orderId);
    }
}
