package com.kanapa4.shop.userinteraction.controller;

import com.kanapa4.shop.userinteraction.dto.AddressDto;
import com.kanapa4.shop.userinteraction.dto.CustomerDto;
import com.kanapa4.shop.userinteraction.dto.OrderPageResponse;
import com.kanapa4.shop.userinteraction.dto.OrderResponse;
import com.kanapa4.shop.userinteraction.dto.OrderStatus;
import com.kanapa4.shop.userinteraction.dto.OrderSummaryResponse;
import com.kanapa4.shop.userinteraction.exception.GlobalExceptionHandler;
import com.kanapa4.shop.userinteraction.model.DeliveryMethod;
import com.kanapa4.shop.userinteraction.model.PaymentMethod;
import com.kanapa4.shop.userinteraction.service.UserOrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class OrderControllerTest {

    private static final UUID USER_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID ORDER_ID = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
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
    @DisplayName("GET /api/orders returns 200 OK with paged order history")
    void getOrders_ReturnsOk() throws Exception {
        OrderSummaryResponse summary = new OrderSummaryResponse(
                ORDER_ID,
                "ORD-2026-0001",
                OrderStatus.NEW,
                new BigDecimal("115.00"),
                "PLN",
                Instant.now()
        );
        OrderPageResponse<OrderSummaryResponse> page = new OrderPageResponse<>(List.of(summary), 0, 20, 1, 1);
        when(userOrderService.getOrders(0, 20, "createdAt", "DESC")).thenReturn(page);

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(ORDER_ID.toString()))
                .andExpect(jsonPath("$.content[0].orderNumber").value("ORD-2026-0001"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(userOrderService).getOrders(0, 20, "createdAt", "DESC");
    }

    @Test
    @DisplayName("GET /api/orders/{orderId} returns 200 OK with order details")
    void getOrder_ReturnsOk() throws Exception {
        when(userOrderService.getOrder(ORDER_ID)).thenReturn(sampleOrderResponse(OrderStatus.NEW));

        mockMvc.perform(get("/api/orders/{orderId}", ORDER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ORDER_ID.toString()))
                .andExpect(jsonPath("$.orderNumber").value("ORD-2026-0001"))
                .andExpect(jsonPath("$.status").value("NEW"));

        verify(userOrderService).getOrder(ORDER_ID);
    }

    @Test
    @DisplayName("POST /api/orders/{orderId}/pay returns 200 OK with paid status")
    void pay_ReturnsOk() throws Exception {
        when(userOrderService.pay(ORDER_ID)).thenReturn(sampleOrderResponse(OrderStatus.PAID));

        mockMvc.perform(post("/api/orders/{orderId}/pay", ORDER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ORDER_ID.toString()))
                .andExpect(jsonPath("$.status").value("PAID"));

        verify(userOrderService).pay(ORDER_ID);
    }

    @Test
    @DisplayName("POST /api/orders/{orderId}/cancel returns 200 OK with cancelled status")
    void cancel_ReturnsOk() throws Exception {
        when(userOrderService.cancel(ORDER_ID)).thenReturn(sampleOrderResponse(OrderStatus.CANCELLED));

        mockMvc.perform(post("/api/orders/{orderId}/cancel", ORDER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ORDER_ID.toString()))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(userOrderService).cancel(ORDER_ID);
    }

    @Test
    @DisplayName("POST /api/orders/{orderId}/invoice returns 200 OK with order response")
    void retryInvoice_ReturnsOk() throws Exception {
        when(userOrderService.retryInvoice(ORDER_ID)).thenReturn(sampleOrderResponse(OrderStatus.PAID));

        mockMvc.perform(post("/api/orders/{orderId}/invoice", ORDER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ORDER_ID.toString()));

        verify(userOrderService).retryInvoice(ORDER_ID);
    }

    @Test
    @DisplayName("POST /api/orders/{orderId}/ship returns 200 OK with shipped status")
    void ship_ReturnsOk() throws Exception {
        when(userOrderService.ship(ORDER_ID)).thenReturn(sampleOrderResponse(OrderStatus.SHIPPED));

        mockMvc.perform(post("/api/orders/{orderId}/ship", ORDER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ORDER_ID.toString()))
                .andExpect(jsonPath("$.status").value("SHIPPED"));

        verify(userOrderService).ship(ORDER_ID);
    }
}

