package com.kanapa4.shop.userinteraction.controller;

import com.kanapa4.shop.userinteraction.dto.OrderPageResponse;
import com.kanapa4.shop.userinteraction.dto.OrderResponse;
import com.kanapa4.shop.userinteraction.dto.OrderSummaryResponse;
import com.kanapa4.shop.userinteraction.service.UserOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order history, payment, cancellation, and shipping")
public class OrderController {

    private final UserOrderService userOrderService;

    @GetMapping
    @Operation(summary = "Get current user's orders (paged)")
    public OrderPageResponse<OrderSummaryResponse> getOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction) {
        log.info("GET /api/orders page={} size={} sortBy={} direction={}", page, size, sortBy, direction);
        return userOrderService.getOrders(page, size, sortBy, direction);
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get single order details by order ID")
    public OrderResponse getOrder(@PathVariable UUID orderId) {
        log.info("GET /api/orders/{}", orderId);
        return userOrderService.getOrder(orderId);
    }

    @PostMapping("/{orderId}/pay")
    @Operation(summary = "Pay for an unpaid order")
    public OrderResponse pay(@PathVariable UUID orderId) {
        log.info("POST /api/orders/{}/pay", orderId);
        return userOrderService.pay(orderId);
    }

    @PostMapping("/{orderId}/cancel")
    @Operation(summary = "Cancel an unpaid order")
    public OrderResponse cancel(@PathVariable UUID orderId) {
        log.info("POST /api/orders/{}/cancel", orderId);
        return userOrderService.cancel(orderId);
    }

    @PostMapping("/{orderId}/invoice")
    @Operation(summary = "Retry issuing invoice for a paid order")
    public OrderResponse retryInvoice(@PathVariable UUID orderId) {
        log.info("POST /api/orders/{}/invoice", orderId);
        return userOrderService.retryInvoice(orderId);
    }

    @PostMapping("/{orderId}/ship")
    @Operation(summary = "Mark order as shipped (Admin only)")
    public OrderResponse ship(@PathVariable UUID orderId) {
        log.info("POST /api/orders/{}/ship (Admin)", orderId);
        return userOrderService.ship(orderId);
    }
}
