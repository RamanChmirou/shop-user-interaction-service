package com.kanapa4.shop.userinteraction.controller;

import com.kanapa4.shop.userinteraction.dto.AddCartItemRequest;
import com.kanapa4.shop.userinteraction.dto.CartItemResponse;
import com.kanapa4.shop.userinteraction.dto.CartResponse;
import com.kanapa4.shop.userinteraction.dto.PriceDto;
import com.kanapa4.shop.userinteraction.dto.RepriceResponse;
import com.kanapa4.shop.userinteraction.dto.SelectedOptionRequest;
import com.kanapa4.shop.userinteraction.dto.UpdateQuantityRequest;
import com.kanapa4.shop.userinteraction.dto.VatRate;
import com.kanapa4.shop.userinteraction.exception.DownstreamServiceUnavailableException;
import com.kanapa4.shop.userinteraction.exception.GlobalExceptionHandler;
import com.kanapa4.shop.userinteraction.service.UserCartService;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CartController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CartControllerTest {

    private static final UUID USER_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID CART_ID = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001");
    private static final UUID ITEM_ID = UUID.fromString("cccccccc-0000-0000-0000-000000000001");
    private static final UUID PRODUCT_ID = UUID.fromString("11111111-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserCartService userCartService;

    private CartResponse sampleCartResponse() {
        CartItemResponse item = new CartItemResponse(
                ITEM_ID,
                PRODUCT_ID,
                "Laptop Pro 15",
                new PriceDto(new BigDecimal("3999.00"), "PLN", VatRate.VAT_23),
                1,
                new PriceDto(new BigDecimal("3999.00"), "PLN", VatRate.VAT_23),
                List.of()
        );
        return new CartResponse(
                CART_ID,
                USER_ID,
                "ACTIVE",
                List.of(item),
                1,
                new BigDecimal("3999.00"),
                "PLN"
        );
    }

    @Test
    @DisplayName("GET /api/cart returns 200 OK with cart response")
    void getCart_ReturnsOk() throws Exception {
        when(userCartService.getCart()).thenReturn(sampleCartResponse());

        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CART_ID.toString()))
                .andExpect(jsonPath("$.userId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.items[0].productName").value("Laptop Pro 15"))
                .andExpect(jsonPath("$.totalAmount").value(3999.00));

        verify(userCartService).getCart();
    }

    @Test
    @DisplayName("GET /api/cart returns 503 when downstream service is unavailable")
    void getCart_DownstreamUnavailable_Returns503() throws Exception {
        when(userCartService.getCart()).thenThrow(new DownstreamServiceUnavailableException("cart-service"));

        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value("cart-service is currently unavailable, please try again later"));
    }

    @Test
    @DisplayName("POST /api/cart/items returns 201 CREATED with updated cart")
    void addItem_ValidRequest_ReturnsCreated() throws Exception {
        AddCartItemRequest request = new AddCartItemRequest(PRODUCT_ID, 2, List.of(new SelectedOptionRequest("RAM", "RAM_32")));
        when(userCartService.addItem(any(AddCartItemRequest.class))).thenReturn(sampleCartResponse());

        mockMvc.perform(post("/api/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(CART_ID.toString()));

        verify(userCartService).addItem(any(AddCartItemRequest.class));
    }

    @Test
    @DisplayName("POST /api/cart/items with invalid quantity returns 400 Bad Request")
    void addItem_InvalidQuantity_ReturnsBadRequest() throws Exception {
        String invalidJson = """
                {
                    "productId": "11111111-0000-0000-0000-000000000001",
                    "quantity": 0
                }
                """;

        mockMvc.perform(post("/api/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /api/cart/items/{itemId} returns 200 OK with updated cart")
    void updateQuantity_ValidRequest_ReturnsOk() throws Exception {
        UpdateQuantityRequest request = new UpdateQuantityRequest(3);
        when(userCartService.updateQuantity(eq(ITEM_ID), any(UpdateQuantityRequest.class))).thenReturn(sampleCartResponse());

        mockMvc.perform(patch("/api/cart/items/{itemId}", ITEM_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CART_ID.toString()));

        verify(userCartService).updateQuantity(eq(ITEM_ID), any(UpdateQuantityRequest.class));
    }

    @Test
    @DisplayName("DELETE /api/cart/items/{itemId} returns 204 No Content")
    void removeItem_ReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/cart/items/{itemId}", ITEM_ID))
                .andExpect(status().isNoContent());

        verify(userCartService).removeItem(ITEM_ID);
    }

    @Test
    @DisplayName("DELETE /api/cart returns 204 No Content")
    void clear_ReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/cart"))
                .andExpect(status().isNoContent());

        verify(userCartService).clear();
    }

    @Test
    @DisplayName("POST /api/cart/reprice returns 200 OK with reprice response")
    void reprice_ReturnsOk() throws Exception {
        RepriceResponse repriceResponse = new RepriceResponse(sampleCartResponse(), List.of());
        when(userCartService.reprice()).thenReturn(repriceResponse);

        mockMvc.perform(post("/api/cart/reprice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cart.id").value(CART_ID.toString()));

        verify(userCartService).reprice();
    }
}

