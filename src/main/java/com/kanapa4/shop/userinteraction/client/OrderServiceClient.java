package com.kanapa4.shop.userinteraction.client;

import com.kanapa4.shop.userinteraction.dto.OrderCheckoutRequest;
import com.kanapa4.shop.userinteraction.dto.OrderPageResponse;
import com.kanapa4.shop.userinteraction.dto.OrderResponse;
import com.kanapa4.shop.userinteraction.dto.OrderSummaryResponse;
import com.kanapa4.shop.userinteraction.client.config.FeignClientConfig;
import com.kanapa4.shop.userinteraction.client.fallback.OrderServiceClientFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(
        name = "order-service",
        url = "${ORDER_SERVICE_URL:http://localhost:8083}",
        configuration = FeignClientConfig.class,
        fallbackFactory = OrderServiceClientFallbackFactory.class
)
public interface OrderServiceClient {


    @PostMapping("/orders/{userId}")
    OrderResponse checkout(@PathVariable("userId") UUID userId, @RequestBody OrderCheckoutRequest request);

    @GetMapping("/orders/{userId}")
    OrderPageResponse<OrderSummaryResponse> getOrders(
            @PathVariable("userId") UUID userId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "direction", defaultValue = "DESC") String direction);

    @GetMapping("/orders/{userId}/{orderId}")
    OrderResponse getOrder(@PathVariable("userId") UUID userId, @PathVariable("orderId") UUID orderId);

    @PostMapping("/orders/{userId}/{orderId}/pay")
    OrderResponse pay(@PathVariable("userId") UUID userId, @PathVariable("orderId") UUID orderId);

    @PostMapping("/orders/{userId}/{orderId}/cancel")
    OrderResponse cancel(@PathVariable("userId") UUID userId, @PathVariable("orderId") UUID orderId);

    @PostMapping("/orders/{userId}/{orderId}/invoice")
    OrderResponse retryInvoice(@PathVariable("userId") UUID userId, @PathVariable("orderId") UUID orderId);

    @PostMapping("/orders/{orderId}/ship")
    OrderResponse ship(@PathVariable("orderId") UUID orderId);
}
