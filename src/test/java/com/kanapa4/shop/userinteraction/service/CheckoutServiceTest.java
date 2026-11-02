package com.kanapa4.shop.userinteraction.service;

import com.kanapa4.shop.userinteraction.client.CartServiceClient;
import com.kanapa4.shop.userinteraction.client.OrderServiceClient;
import com.kanapa4.shop.userinteraction.dto.AddressDto;
import com.kanapa4.shop.userinteraction.dto.CartItemResponse;
import com.kanapa4.shop.userinteraction.dto.CartResponse;
import com.kanapa4.shop.userinteraction.dto.CheckoutSummaryResponse;
import com.kanapa4.shop.userinteraction.dto.CustomerDto;
import com.kanapa4.shop.userinteraction.dto.OrderCheckoutRequest;
import com.kanapa4.shop.userinteraction.dto.OrderResponse;
import com.kanapa4.shop.userinteraction.dto.OrderStatus;
import com.kanapa4.shop.userinteraction.dto.PriceDto;
import com.kanapa4.shop.userinteraction.dto.VatRate;
import com.kanapa4.shop.userinteraction.model.AddressData;
import com.kanapa4.shop.userinteraction.model.CheckoutData;
import com.kanapa4.shop.userinteraction.model.CheckoutDraft;
import com.kanapa4.shop.userinteraction.model.DeliveryMethod;
import com.kanapa4.shop.userinteraction.model.PaymentMethod;
import com.kanapa4.shop.userinteraction.repository.CheckoutDraftRepository;
import com.kanapa4.shop.userinteraction.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.kanapa4.shop.userinteraction.exception.CheckoutDraftMissingException;
import com.kanapa4.shop.userinteraction.exception.EmptyCartException;


import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckoutServiceTest {

    private static final UUID USER_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");

    @Mock
    private CheckoutDraftRepository checkoutDraftRepository;

    @Mock
    private CartServiceClient cartServiceClient;

    @Mock
    private OrderServiceClient orderServiceClient;

    @Mock
    private CurrentUser currentUser;

    @InjectMocks
    private CheckoutService checkoutService;

    @BeforeEach
    void setUp() {
        when(currentUser.id()).thenReturn(USER_ID);
    }

    @Test
    @DisplayName("saveDraft persists or updates user's draft in database")
    void saveDraft_Success() {
        CheckoutData data = sampleCheckoutData();
        when(checkoutDraftRepository.findById(USER_ID)).thenReturn(Optional.empty());

        CheckoutData result = checkoutService.saveDraft(data);

        assertThat(result).isEqualTo(data);
        verify(checkoutDraftRepository).save(any(CheckoutDraft.class));
    }

    @Test
    @DisplayName("getSummary aggregates cart and draft data with courier delivery cost")
    void getSummary_Success() {
        CartResponse cart = sampleCartResponse(new BigDecimal("100.00"));
        CheckoutData data = sampleCheckoutData();

        when(cartServiceClient.getCart(USER_ID)).thenReturn(cart);
        when(checkoutDraftRepository.findById(USER_ID)).thenReturn(Optional.of(new CheckoutDraft(USER_ID, data)));

        CheckoutSummaryResponse summary = checkoutService.getSummary();

        assertThat(summary.cart()).isEqualTo(cart);
        assertThat(summary.checkoutData()).isEqualTo(data);
        assertThat(summary.deliveryCost()).isEqualByComparingTo(new BigDecimal("15.00")); // COURIER
        assertThat(summary.grandTotal()).isEqualByComparingTo(new BigDecimal("115.00"));
        assertThat(summary.readyToSubmit()).isTrue();
    }

    @Test
    @DisplayName("submitOrder throws BAD_REQUEST when draft is missing")
    void submitOrder_DraftMissing() {
        when(checkoutDraftRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> checkoutService.submitOrder())
                .isInstanceOf(CheckoutDraftMissingException.class)
                .hasMessageContaining("Checkout draft is missing");
    }

    @Test
    @DisplayName("submitOrder throws BAD_REQUEST when cart is empty")
    void submitOrder_CartEmpty() {
        CheckoutData data = sampleCheckoutData();
        when(checkoutDraftRepository.findById(USER_ID)).thenReturn(Optional.of(new CheckoutDraft(USER_ID, data)));
        when(cartServiceClient.getCart(USER_ID)).thenReturn(sampleEmptyCartResponse());

        assertThatThrownBy(() -> checkoutService.submitOrder())
                .isInstanceOf(EmptyCartException.class)
                .hasMessageContaining("Cart is empty");
    }


    @Test
    @DisplayName("submitOrder submits to orderServiceClient and deletes draft")
    void submitOrder_Success() {
        CheckoutData data = sampleCheckoutData();
        CartResponse cart = sampleCartResponse(new BigDecimal("2999.00"));
        OrderResponse expectedOrder = sampleOrderResponse();

        when(checkoutDraftRepository.findById(USER_ID)).thenReturn(Optional.of(new CheckoutDraft(USER_ID, data)));
        when(cartServiceClient.getCart(USER_ID)).thenReturn(cart);
        when(orderServiceClient.checkout(eq(USER_ID), any(OrderCheckoutRequest.class))).thenReturn(expectedOrder);

        OrderResponse result = checkoutService.submitOrder();

        assertThat(result).isEqualTo(expectedOrder);
        verify(checkoutDraftRepository).deleteById(USER_ID);

        ArgumentCaptor<OrderCheckoutRequest> captor = ArgumentCaptor.forClass(OrderCheckoutRequest.class);
        verify(orderServiceClient).checkout(eq(USER_ID), captor.capture());
        OrderCheckoutRequest captured = captor.getValue();
        assertThat(captured.expectedTotal()).isEqualByComparingTo(new BigDecimal("2999.00"));
        assertThat(captured.deliveryMethod()).isEqualTo(DeliveryMethod.COURIER);
        assertThat(captured.paymentMethod()).isEqualTo(PaymentMethod.CARD);
    }

    private CheckoutData sampleCheckoutData() {
        AddressData address = new AddressData("Marszalkowska 1", "Warszawa", "00-001", "PL");
        return new CheckoutData(
                "Jan",
                "Kowalski",
                "jan@example.com",
                "+48123456789",
                address,
                null,
                DeliveryMethod.COURIER,
                PaymentMethod.CARD,
                "Please call before delivery"
        );
    }

    private CartResponse sampleCartResponse(BigDecimal total) {
        PriceDto price = new PriceDto(total, "PLN", VatRate.VAT_23);
        CartItemResponse item = new CartItemResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Smartphone X",
                price,
                1,
                price,
                List.of()
        );
        return new CartResponse(
                UUID.randomUUID(),
                USER_ID,
                "ACTIVE",
                List.of(item),
                1,
                total,
                "PLN"
        );
    }

    private CartResponse sampleEmptyCartResponse() {
        return new CartResponse(
                UUID.randomUUID(),
                USER_ID,
                "ACTIVE",
                List.of(),
                0,
                BigDecimal.ZERO,
                "PLN"
        );
    }

    private OrderResponse sampleOrderResponse() {
        CustomerDto customer = new CustomerDto("Jan", "Kowalski", "jan@example.com", "+48123456789");
        AddressDto address = new AddressDto("Marszalkowska 1", "Warszawa", "00-001", "PL");
        return new OrderResponse(
                UUID.randomUUID(),
                "ORD-2026-0001",
                USER_ID,
                OrderStatus.NEW,
                DeliveryMethod.COURIER,
                PaymentMethod.CARD,
                customer,
                address,
                address,
                List.of(),
                new BigDecimal("2999.00"),
                new BigDecimal("15.00"),
                new BigDecimal("3014.00"),
                "PLN",
                null,
                Instant.now()
        );
    }
}
