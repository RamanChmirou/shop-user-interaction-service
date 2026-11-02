package com.kanapa4.shop.userinteraction.service;

import com.kanapa4.shop.userinteraction.client.OrderServiceClient;
import com.kanapa4.shop.userinteraction.dto.AddressDto;
import com.kanapa4.shop.userinteraction.dto.CustomerDto;
import com.kanapa4.shop.userinteraction.dto.OrderPageResponse;
import com.kanapa4.shop.userinteraction.dto.OrderResponse;
import com.kanapa4.shop.userinteraction.dto.OrderStatus;
import com.kanapa4.shop.userinteraction.dto.OrderSummaryResponse;
import com.kanapa4.shop.userinteraction.model.DeliveryMethod;
import com.kanapa4.shop.userinteraction.model.PaymentMethod;
import com.kanapa4.shop.userinteraction.security.CurrentUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserOrderServiceTest {

    private static final UUID USER_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID ORDER_ID = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001");

    @Mock
    private OrderServiceClient orderServiceClient;

    @Mock
    private CurrentUser currentUser;

    @InjectMocks
    private UserOrderService userOrderService;

    private OrderResponse sampleOrderResponse(OrderStatus status) {
        CustomerDto customer = new CustomerDto("Jan", "Kowalski", "jan@example.com", "+48123456789");
        AddressDto address = new AddressDto("Marszalkowska 1", "Warszawa", "00-001", "PL");
        return new OrderResponse(
                ORDER_ID,
                "ORD-2026-0001",
                USER_ID,
                status,
                DeliveryMethod.COURIER,
                PaymentMethod.CARD,
                customer,
                address,
                null,
                List.of(),
                new BigDecimal("100.00"),
                new BigDecimal("15.00"),
                new BigDecimal("115.00"),
                "PLN",
                null,
                Instant.now()
        );
    }

    @Test
    @DisplayName("getOrders retrieves paged order history for authenticated user")
    void getOrders_Success() {
        when(currentUser.id()).thenReturn(USER_ID);
        OrderSummaryResponse summary = new OrderSummaryResponse(
                ORDER_ID,
                "ORD-2026-0001",
                OrderStatus.NEW,
                new BigDecimal("115.00"),
                "PLN",
                Instant.now()
        );
        OrderPageResponse<OrderSummaryResponse> expectedPage = new OrderPageResponse<>(List.of(summary), 0, 20, 1, 1);
        when(orderServiceClient.getOrders(USER_ID, 0, 20, "createdAt", "DESC")).thenReturn(expectedPage);

        OrderPageResponse<OrderSummaryResponse> result = userOrderService.getOrders(0, 20, "createdAt", "DESC");

        assertThat(result).isEqualTo(expectedPage);
        verify(orderServiceClient).getOrders(USER_ID, 0, 20, "createdAt", "DESC");
    }

    @Test
    @DisplayName("getOrder retrieves specific order details for authenticated user")
    void getOrder_Success() {
        when(currentUser.id()).thenReturn(USER_ID);
        OrderResponse expected = sampleOrderResponse(OrderStatus.NEW);
        when(orderServiceClient.getOrder(USER_ID, ORDER_ID)).thenReturn(expected);

        OrderResponse result = userOrderService.getOrder(ORDER_ID);

        assertThat(result).isEqualTo(expected);
        verify(orderServiceClient).getOrder(USER_ID, ORDER_ID);
    }

    @Test
    @DisplayName("pay triggers payment for user's order")
    void pay_Success() {
        when(currentUser.id()).thenReturn(USER_ID);
        OrderResponse expected = sampleOrderResponse(OrderStatus.PAID);
        when(orderServiceClient.pay(USER_ID, ORDER_ID)).thenReturn(expected);

        OrderResponse result = userOrderService.pay(ORDER_ID);

        assertThat(result).isEqualTo(expected);
        verify(orderServiceClient).pay(USER_ID, ORDER_ID);
    }

    @Test
    @DisplayName("cancel cancels an unpaid order for user")
    void cancel_Success() {
        when(currentUser.id()).thenReturn(USER_ID);
        OrderResponse expected = sampleOrderResponse(OrderStatus.CANCELLED);
        when(orderServiceClient.cancel(USER_ID, ORDER_ID)).thenReturn(expected);

        OrderResponse result = userOrderService.cancel(ORDER_ID);

        assertThat(result).isEqualTo(expected);
        verify(orderServiceClient).cancel(USER_ID, ORDER_ID);
    }

    @Test
    @DisplayName("retryInvoice retries generating invoice for user's paid order")
    void retryInvoice_Success() {
        when(currentUser.id()).thenReturn(USER_ID);
        OrderResponse expected = sampleOrderResponse(OrderStatus.PAID);
        when(orderServiceClient.retryInvoice(USER_ID, ORDER_ID)).thenReturn(expected);

        OrderResponse result = userOrderService.retryInvoice(ORDER_ID);

        assertThat(result).isEqualTo(expected);
        verify(orderServiceClient).retryInvoice(USER_ID, ORDER_ID);
    }

    @Test
    @DisplayName("ship marks order as shipped without requiring currentUser")
    void ship_Success() {
        OrderResponse expected = sampleOrderResponse(OrderStatus.SHIPPED);
        when(orderServiceClient.ship(ORDER_ID)).thenReturn(expected);

        OrderResponse result = userOrderService.ship(ORDER_ID);

        assertThat(result).isEqualTo(expected);
        verify(orderServiceClient).ship(ORDER_ID);
        verifyNoInteractions(currentUser);
    }
}

