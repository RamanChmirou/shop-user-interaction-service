package com.kanapa4.shop.userinteraction.security;

import com.kanapa4.shop.userinteraction.controller.CartController;
import com.kanapa4.shop.userinteraction.controller.CatalogController;
import com.kanapa4.shop.userinteraction.controller.CheckoutController;
import com.kanapa4.shop.userinteraction.controller.OrderController;
import com.kanapa4.shop.userinteraction.dto.CartResponse;
import com.kanapa4.shop.userinteraction.dto.PageResponse;
import com.kanapa4.shop.userinteraction.dto.PriceDto;
import com.kanapa4.shop.userinteraction.dto.ProductDto;
import com.kanapa4.shop.userinteraction.dto.ProductRequest;
import com.kanapa4.shop.userinteraction.dto.ProductType;
import com.kanapa4.shop.userinteraction.dto.VatRate;
import com.kanapa4.shop.userinteraction.service.CatalogService;
import com.kanapa4.shop.userinteraction.service.CheckoutService;
import com.kanapa4.shop.userinteraction.service.UserCartService;
import com.kanapa4.shop.userinteraction.service.UserOrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({
        CatalogController.class,
        CartController.class,
        CheckoutController.class,
        OrderController.class
})
@Import({SecurityConfig.class, KeycloakRealmRoleConverter.class, CurrentUser.class})
class UserInteractionSecurityTest {

    private static final UUID USER_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID ADMIN_ID = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CatalogService catalogService;

    @MockitoBean
    private UserCartService userCartService;

    @MockitoBean
    private CheckoutService checkoutService;

    @MockitoBean
    private UserOrderService userOrderService;

    @Test
    @DisplayName("GET /api/products is publicly accessible without token")
    void getProducts_PublicAccess_ReturnsOk() throws Exception {
        when(catalogService.getAll(anyInt(), anyInt(), anyString()))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/cart without token returns 401 Unauthorized")
    void getCart_Anonymous_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/cart with USER role returns 200 OK")
    void getCart_UserRole_ReturnsOk() throws Exception {
        when(userCartService.getCart()).thenReturn(new CartResponse(UUID.randomUUID(), USER_ID, "ACTIVE", List.of(), 0, BigDecimal.ZERO, "PLN"));

        mockMvc.perform(get("/api/cart")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))
                                .jwt(builder -> builder.subject(USER_ID.toString()))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/products (Admin action) with USER role returns 403 Forbidden")
    void createProduct_UserRole_ReturnsForbidden() throws Exception {
        String json = """
                {
                  "name": "New Laptop",
                  "type": "COMPUTER",
                  "price": { "amount": 2999.00, "currency": "PLN", "vatRate": "VAT_23" }
                }
                """;

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))
                                .jwt(builder -> builder.subject(USER_ID.toString()))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/products (Admin action) with ADMIN role returns 201 Created")
    void createProduct_AdminRole_ReturnsCreated() throws Exception {
        String json = """
                {
                  "name": "New Laptop",
                  "type": "COMPUTER",
                  "price": { "amount": 2999.00, "currency": "PLN", "vatRate": "VAT_23" }
                }
                """;

        when(catalogService.createProduct(any(ProductRequest.class)))
                .thenReturn(new ProductDto(UUID.randomUUID(), "New Laptop", null, ProductType.COMPUTER,
                        new PriceDto(new BigDecimal("2999.00"), "PLN", VatRate.VAT_23), Map.of(), true));

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_ADMIN"))
                                .jwt(builder -> builder.subject(ADMIN_ID.toString()))))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /api/orders/{id}/ship with USER role returns 403 Forbidden")
    void shipOrder_UserRole_ReturnsForbidden() throws Exception {
        UUID orderId = UUID.randomUUID();

        mockMvc.perform(post("/api/orders/{id}/ship", orderId)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))
                                .jwt(builder -> builder.subject(USER_ID.toString()))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/orders/{id}/ship with ADMIN role is allowed")
    void shipOrder_AdminRole_ReturnsOk() throws Exception {
        UUID orderId = UUID.randomUUID();

        mockMvc.perform(post("/api/orders/{id}/ship", orderId)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_ADMIN"))
                                .jwt(builder -> builder.subject(ADMIN_ID.toString()))))
                .andExpect(status().isOk());
    }
}
