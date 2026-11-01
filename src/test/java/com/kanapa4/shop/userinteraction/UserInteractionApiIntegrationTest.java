package com.kanapa4.shop.userinteraction;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.kanapa4.shop.userinteraction.model.AddressData;
import com.kanapa4.shop.userinteraction.model.CheckoutData;
import com.kanapa4.shop.userinteraction.model.DeliveryMethod;
import com.kanapa4.shop.userinteraction.model.PaymentMethod;
import com.kanapa4.shop.userinteraction.repository.CheckoutDraftRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.wiremock.spring.EnableWireMock;
import org.wiremock.spring.InjectWireMock;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "PRODUCT_SERVICE_URL=http://localhost:${wiremock.server.port}",
        "CART_SERVICE_URL=http://localhost:${wiremock.server.port}",
        "ORDER_SERVICE_URL=http://localhost:${wiremock.server.port}",
        "resilience4j.circuitbreaker.configs.default.minimum-number-of-calls=100"
})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@EnableWireMock
class UserInteractionApiIntegrationTest {

    private static final UUID USER_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CheckoutDraftRepository checkoutDraftRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @InjectWireMock
    private WireMockServer wireMock;

    @BeforeEach
    void cleanUp() {
        checkoutDraftRepository.deleteAll();
        wireMock.resetAll();
    }

    @Test
    @DisplayName("Complete sales process across Screens 1, 2, 3")
    void completeSalesFlow_Success() throws Exception {
        // --- Screen 1: Browse catalog and view products ---
        String productsResponse = """
                {
                  "content": [
                    {
                      "id": "11111111-0000-0000-0000-000000000001",
                      "name": "Laptop Pro 15",
                      "type": "COMPUTER",
                      "price": { "amount": 3999.00, "currency": "PLN", "vatRate": "VAT_23" }
                    }
                  ],
                  "number": 0,
                  "size": 20,
                  "totalElements": 1,
                  "totalPages": 1
                }
                """;

        wireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("/products"))
                .willReturn(WireMock.okJson(productsResponse)));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Laptop Pro 15"))
                .andExpect(jsonPath("$.content[0].price.amount").value(3999.00));

        // --- Screen 1: Add product to cart ---
        String cartResponse = """
                {
                  "id": "22222222-0000-0000-0000-000000000001",
                  "userId": "aaaaaaaa-0000-0000-0000-000000000001",
                  "status": "ACTIVE",
                  "items": [
                    {
                      "id": "33333333-0000-0000-0000-000000000001",
                      "productId": "11111111-0000-0000-0000-000000000001",
                      "productName": "Laptop Pro 15",
                      "unitPrice": { "amount": 3999.00, "currency": "PLN", "vatRate": "VAT_23" },
                      "quantity": 1,
                      "lineTotal": { "amount": 3999.00, "currency": "PLN", "vatRate": "VAT_23" },
                      "selectedOptions": []
                    }
                  ],
                  "itemCount": 1,
                  "totalAmount": 3999.00,
                  "currency": "PLN"
                }
                """;

        wireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/carts/" + USER_ID + "/items"))
                .willReturn(WireMock.aResponse().withStatus(201).withHeader("Content-Type", "application/json").withBody(cartResponse)));

        wireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/carts/" + USER_ID))
                .willReturn(WireMock.okJson(cartResponse)));

        String addCartJson = """
                {
                  "productId": "11111111-0000-0000-0000-000000000001",
                  "quantity": 1,
                  "selectedOptions": []
                }
                """;

        mockMvc.perform(post("/api/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addCartJson)
                        .with(userAuth()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalAmount").value(3999.00));

        // --- Screen 2: Save checkout draft ---
        CheckoutData draft = new CheckoutData(
                "Jan",
                "Kowalski",
                "jan@example.com",
                "+48123456789",
                new AddressData("Marszalkowska 1", "Warszawa", "00-001", "PL"),
                null,
                DeliveryMethod.COURIER,
                PaymentMethod.CARD,
                "Door code: 1234"
        );

        mockMvc.perform(put("/api/checkout/draft")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(draft))
                        .with(userAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Jan"))
                .andExpect(jsonPath("$.deliveryMethod").value("COURIER"));

        // Verify draft was saved in DB
        assertThat(checkoutDraftRepository.findById(USER_ID)).isPresent();

        // --- Screen 3: Fetch Checkout Summary ---
        mockMvc.perform(get("/api/checkout/summary")
                        .with(userAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cart.totalAmount").value(3999.00))
                .andExpect(jsonPath("$.deliveryCost").value(15.00))
                .andExpect(jsonPath("$.grandTotal").value(4014.00))
                .andExpect(jsonPath("$.readyToSubmit").value(true));

        // --- Screen 3: Submit Order ---
        String orderCreatedResponse = """
                {
                  "id": "44444444-0000-0000-0000-000000000001",
                  "orderNumber": "ORD-2026-0001",
                  "userId": "aaaaaaaa-0000-0000-0000-000000000001",
                  "status": "NEW",
                  "deliveryMethod": "COURIER",
                  "paymentMethod": "CARD",
                  "itemsTotal": 3999.00,
                  "deliveryCost": 15.00,
                  "grandTotal": 4014.00,
                  "currency": "PLN",
                  "createdAt": "2026-11-01T12:00:00Z"
                }
                """;

        wireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/orders/" + USER_ID))
                .willReturn(WireMock.aResponse().withStatus(201).withHeader("Content-Type", "application/json").withBody(orderCreatedResponse)));

        mockMvc.perform(post("/api/checkout/submit")
                        .with(userAuth()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderNumber").value("ORD-2026-0001"))
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.grandTotal").value(4014.00));

        // Verify draft was cleared after successful order placement
        assertThat(checkoutDraftRepository.findById(USER_ID)).isEmpty();
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor userAuth() {
        return jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"))
                .jwt(builder -> builder.subject(USER_ID.toString()));
    }

}
