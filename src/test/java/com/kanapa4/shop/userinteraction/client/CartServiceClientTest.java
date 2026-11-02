package com.kanapa4.shop.userinteraction.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.kanapa4.shop.userinteraction.client.config.FeignClientConfig;
import com.kanapa4.shop.userinteraction.client.fallback.CartServiceClientFallbackFactory;
import com.kanapa4.shop.userinteraction.dto.AddCartItemRequest;
import com.kanapa4.shop.userinteraction.dto.CartResponse;
import com.kanapa4.shop.userinteraction.dto.RepriceResponse;
import com.kanapa4.shop.userinteraction.dto.SelectedOptionRequest;
import com.kanapa4.shop.userinteraction.dto.UpdateQuantityRequest;
import com.kanapa4.shop.userinteraction.exception.DownstreamServiceUnavailableException;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.wiremock.spring.EnableWireMock;
import org.wiremock.spring.InjectWireMock;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(
        classes = CartServiceClientTest.TestConfig.class,
        properties = {
                "CART_SERVICE_URL=http://localhost:${wiremock.server.port}",
                "spring.cloud.openfeign.httpclient.hc5.enabled=true",
                "resilience4j.circuitbreaker.configs.default.minimum-number-of-calls=100"
        }
)
@EnableWireMock
class CartServiceClientTest {

    @Configuration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            LiquibaseAutoConfiguration.class
    })
    @EnableFeignClients(clients = CartServiceClient.class)
    @Import({CartServiceClientFallbackFactory.class, FeignClientConfig.class})
    static class TestConfig {
    }

    private static final UUID USER_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID ITEM_ID = UUID.fromString("cccccccc-0000-0000-0000-000000000001");
    private static final UUID PRODUCT_ID = UUID.fromString("11111111-0000-0000-0000-000000000001");

    private static final String CART_JSON = """
            {
              "id": "bbbbbbbb-0000-0000-0000-000000000001",
              "userId": "aaaaaaaa-0000-0000-0000-000000000001",
              "status": "ACTIVE",
              "items": [
                {
                  "id": "cccccccc-0000-0000-0000-000000000001",
                  "productId": "11111111-0000-0000-0000-000000000001",
                  "productName": "Laptop Pro 15",
                  "unitPrice": { "amount": 3999.00, "currency": "PLN", "vatRate": "VAT_23" },
                  "quantity": 1,
                  "totalPrice": { "amount": 3999.00, "currency": "PLN", "vatRate": "VAT_23" },
                  "selectedOptions": []
                }
              ],
              "itemCount": 1,
              "totalAmount": 3999.00,
              "currency": "PLN"
            }
            """;

    @InjectWireMock
    private WireMockServer wireMock;

    @Autowired
    private CartServiceClient cartServiceClient;

    @BeforeEach
    void resetWireMock() {
        wireMock.resetAll();
    }

    @Test
    @DisplayName("getCart: successful 200 response maps into CartResponse")
    void getCart_Success() {
        wireMock.stubFor(get(urlPathEqualTo("/carts/" + USER_ID))
                .willReturn(okJson(CART_JSON)));

        CartResponse response = cartServiceClient.getCart(USER_ID);

        assertThat(response.userId()).isEqualTo(USER_ID);
        assertThat(response.items()).hasSize(1);
        assertThat(response.totalAmount()).isEqualByComparingTo(new BigDecimal("3999.00"));
    }

    @Test
    @DisplayName("addItem: 200 response adds item to user cart")
    void addItem_Success() {
        wireMock.stubFor(post(urlPathEqualTo("/carts/" + USER_ID + "/items"))
                .willReturn(okJson(CART_JSON)));

        AddCartItemRequest request = new AddCartItemRequest(PRODUCT_ID, 1, List.of(new SelectedOptionRequest("RAM", "RAM_32")));
        CartResponse response = cartServiceClient.addItem(USER_ID, request);

        assertThat(response).isNotNull();
        wireMock.verify(1, postRequestedFor(urlPathEqualTo("/carts/" + USER_ID + "/items")));
    }

    @Test
    @DisplayName("updateQuantity: 200 response updates cart item quantity")
    void updateQuantity_Success() {
        wireMock.stubFor(patch(urlPathEqualTo("/carts/" + USER_ID + "/items/" + ITEM_ID))
                .willReturn(okJson(CART_JSON)));

        CartResponse response = cartServiceClient.updateQuantity(USER_ID, ITEM_ID, new UpdateQuantityRequest(2));

        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("removeItem: 204 No Content removes item")
    void removeItem_Success() {
        wireMock.stubFor(delete(urlPathEqualTo("/carts/" + USER_ID + "/items/" + ITEM_ID))
                .willReturn(aResponse().withStatus(204)));

        cartServiceClient.removeItem(USER_ID, ITEM_ID);

        wireMock.verify(1, deleteRequestedFor(urlPathEqualTo("/carts/" + USER_ID + "/items/" + ITEM_ID)));
    }

    @Test
    @DisplayName("clear: 204 No Content empties cart")
    void clear_Success() {
        wireMock.stubFor(delete(urlPathEqualTo("/carts/" + USER_ID))
                .willReturn(aResponse().withStatus(204)));

        cartServiceClient.clear(USER_ID);

        wireMock.verify(1, deleteRequestedFor(urlPathEqualTo("/carts/" + USER_ID)));
    }

    @Test
    @DisplayName("reprice: 200 response returns reprice details")
    void reprice_Success() {
        String repriceJson = """
                {
                  "cart": %s,
                  "priceChanges": []
                }
                """.formatted(CART_JSON);

        wireMock.stubFor(post(urlPathEqualTo("/carts/" + USER_ID + "/reprice"))
                .willReturn(okJson(repriceJson)));

        RepriceResponse response = cartServiceClient.reprice(USER_ID);

        assertThat(response.cart()).isNotNull();
        assertThat(response.priceChanges()).isEmpty();
    }

    @Test
    @DisplayName("404 response is rethrown as FeignException.NotFound without hiding client errors")
    void clientError_Propagated() {
        wireMock.stubFor(get(urlPathEqualTo("/carts/" + USER_ID))
                .willReturn(notFound()));

        assertThatThrownBy(() -> cartServiceClient.getCart(USER_ID))
                .isInstanceOf(FeignException.NotFound.class);
    }

    @Test
    @DisplayName("500 response triggers fallback factory and throws DownstreamServiceUnavailableException")
    void serverError_TriggersFallback() {
        wireMock.stubFor(get(urlPathEqualTo("/carts/" + USER_ID))
                .willReturn(serverError()));

        assertThatThrownBy(() -> cartServiceClient.getCart(USER_ID))
                .isInstanceOf(DownstreamServiceUnavailableException.class)
                .hasMessageContaining("cart-service");
    }

    @Test
    @DisplayName("503 response triggers retryer and succeeds when recovering")
    void retry_On503_Succeeds() {
        wireMock.stubFor(get(urlPathEqualTo("/carts/" + USER_ID)).inScenario("retry-test")
                .whenScenarioStateIs(STARTED)
                .willReturn(aResponse().withStatus(503))
                .willSetStateTo("recovered"));

        wireMock.stubFor(get(urlPathEqualTo("/carts/" + USER_ID)).inScenario("retry-test")
                .whenScenarioStateIs("recovered")
                .willReturn(okJson(CART_JSON)));

        CartResponse response = cartServiceClient.getCart(USER_ID);

        assertThat(response).isNotNull();
        wireMock.verify(2, getRequestedFor(urlPathEqualTo("/carts/" + USER_ID)));
    }
}

