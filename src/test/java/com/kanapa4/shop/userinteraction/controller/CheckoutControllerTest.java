package com.kanapa4.shop.userinteraction.controller;

import com.kanapa4.shop.userinteraction.dto.AddressDto;
import com.kanapa4.shop.userinteraction.dto.CartItemResponse;
import com.kanapa4.shop.userinteraction.dto.CartResponse;
import com.kanapa4.shop.userinteraction.dto.CheckoutSummaryResponse;
import com.kanapa4.shop.userinteraction.dto.CustomerDto;
import com.kanapa4.shop.userinteraction.dto.OrderResponse;
import com.kanapa4.shop.userinteraction.dto.OrderStatus;
import com.kanapa4.shop.userinteraction.dto.PriceDto;
import com.kanapa4.shop.userinteraction.dto.VatRate;
import com.kanapa4.shop.userinteraction.exception.CheckoutDraftIncompleteException;
import com.kanapa4.shop.userinteraction.exception.CheckoutDraftMissingException;
import com.kanapa4.shop.userinteraction.exception.EmptyCartException;
import com.kanapa4.shop.userinteraction.exception.GlobalExceptionHandler;
import com.kanapa4.shop.userinteraction.model.AddressData;
import com.kanapa4.shop.userinteraction.model.CheckoutData;
import com.kanapa4.shop.userinteraction.model.DeliveryMethod;
import com.kanapa4.shop.userinteraction.model.PaymentMethod;
import com.kanapa4.shop.userinteraction.service.CheckoutService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CheckoutController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CheckoutControllerTest {

    private static final UUID USER_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID ORDER_ID = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CheckoutService checkoutService;

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

    private CartResponse sampleCartResponse() {
        return new CartResponse(
                UUID.randomUUID(),
                USER_ID,
                "ACTIVE",
                List.of(new CartItemResponse(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "Laptop Pro 15",
                        new PriceDto(new BigDecimal("3999.00"), "PLN", VatRate.VAT_23),
                        1,
                        new PriceDto(new BigDecimal("3999.00"), "PLN", VatRate.VAT_23),
                        List.of()
                )),
                1,
                new BigDecimal("3999.00"),
                "PLN"
        );
    }

    @Test
    @DisplayName("GET /api/checkout/draft returns 200 OK with draft when present")
    void getDraft_Present_ReturnsOk() throws Exception {
        CheckoutData data = sampleCheckoutData();
        when(checkoutService.getDraft()).thenReturn(Optional.of(data));

        mockMvc.perform(get("/api/checkout/draft"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Jan"))
                .andExpect(jsonPath("$.email").value("jan@example.com"))
                .andExpect(jsonPath("$.deliveryMethod").value("COURIER"));

        verify(checkoutService).getDraft();
    }

    @Test
    @DisplayName("GET /api/checkout/draft returns 200 OK with empty body when draft is missing")
    void getDraft_Empty_ReturnsOk() throws Exception {
        when(checkoutService.getDraft()).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/checkout/draft"))
                .andExpect(status().isOk());

        verify(checkoutService).getDraft();
    }

    @Test
    @DisplayName("PUT /api/checkout/draft returns 200 OK with saved draft")
    void saveDraft_ValidRequest_ReturnsOk() throws Exception {
        CheckoutData data = sampleCheckoutData();
        when(checkoutService.saveDraft(any(CheckoutData.class))).thenReturn(data);

        mockMvc.perform(put("/api/checkout/draft")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Jan"))
                .andExpect(jsonPath("$.paymentMethod").value("CARD"));

        verify(checkoutService).saveDraft(any(CheckoutData.class));
    }

    @Test
    @DisplayName("GET /api/checkout/summary returns 200 OK with aggregated summary")
    void getSummary_ReturnsOk() throws Exception {
        CheckoutSummaryResponse summary = new CheckoutSummaryResponse(
                sampleCartResponse(),
                sampleCheckoutData(),
                new BigDecimal("15.00"),
                new BigDecimal("4014.00"),
                "PLN",
                true,
                List.of()
        );
        when(checkoutService.getSummary()).thenReturn(summary);

        mockMvc.perform(get("/api/checkout/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryCost").value(15.00))
                .andExpect(jsonPath("$.grandTotal").value(4014.00))
                .andExpect(jsonPath("$.readyToSubmit").value(true));

        verify(checkoutService).getSummary();
    }

    @Test
    @DisplayName("POST /api/checkout/submit returns 201 CREATED with order details")
    void submitOrder_Success_ReturnsCreated() throws Exception {
        OrderResponse orderResponse = new OrderResponse(
                ORDER_ID,
                "ORD-2026-0001",
                USER_ID,
                OrderStatus.NEW,
                DeliveryMethod.COURIER,
                PaymentMethod.CARD,
                new CustomerDto("Jan", "Kowalski", "jan@example.com", "+48123456789"),
                new AddressDto("Marszalkowska 1", "Warszawa", "00-001", "PL"),
                null,
                List.of(),
                new BigDecimal("3999.00"),
                new BigDecimal("15.00"),
                new BigDecimal("4014.00"),
                "PLN",
                null,
                Instant.now()
        );
        when(checkoutService.submitOrder()).thenReturn(orderResponse);

        mockMvc.perform(post("/api/checkout/submit"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ORDER_ID.toString()))
                .andExpect(jsonPath("$.orderNumber").value("ORD-2026-0001"))
                .andExpect(jsonPath("$.status").value("NEW"));

        verify(checkoutService).submitOrder();
    }

    @Test
    @DisplayName("POST /api/checkout/submit returns 400 Bad Request when cart is empty")
    void submitOrder_EmptyCart_ReturnsBadRequest() throws Exception {
        when(checkoutService.submitOrder()).thenThrow(new EmptyCartException());

        mockMvc.perform(post("/api/checkout/submit"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Cart is empty"));
    }

    @Test
    @DisplayName("POST /api/checkout/submit returns 400 Bad Request when draft is missing")
    void submitOrder_MissingDraft_ReturnsBadRequest() throws Exception {
        when(checkoutService.submitOrder()).thenThrow(new CheckoutDraftMissingException());

        mockMvc.perform(post("/api/checkout/submit"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Checkout draft is missing"));
    }

    @Test
    @DisplayName("POST /api/checkout/submit returns 400 Bad Request when draft is incomplete")
    void submitOrder_IncompleteDraft_ReturnsBadRequest() throws Exception {
        when(checkoutService.submitOrder()).thenThrow(new CheckoutDraftIncompleteException(List.of("shippingAddress")));

        mockMvc.perform(post("/api/checkout/submit"))
                .andExpect(status().isBadRequest());
    }
}

