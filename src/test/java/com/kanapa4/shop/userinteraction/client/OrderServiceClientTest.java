package com.kanapa4.shop.userinteraction.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.kanapa4.shop.userinteraction.client.config.FeignClientConfig;
import com.kanapa4.shop.userinteraction.client.fallback.OrderServiceClientFallbackFactory;
import com.kanapa4.shop.userinteraction.dto.AddressDto;
import com.kanapa4.shop.userinteraction.dto.CustomerDto;
import com.kanapa4.shop.userinteraction.dto.OrderCheckoutRequest;
import com.kanapa4.shop.userinteraction.dto.OrderPageResponse;
import com.kanapa4.shop.userinteraction.dto.OrderResponse;
import com.kanapa4.shop.userinteraction.dto.OrderStatus;
import com.kanapa4.shop.userinteraction.dto.OrderSummaryResponse;
import com.kanapa4.shop.userinteraction.exception.DownstreamServiceUnavailableException;
import com.kanapa4.shop.userinteraction.model.DeliveryMethod;
import com.kanapa4.shop.userinteraction.model.PaymentMethod;
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
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(
        classes = OrderServiceClientTest.TestConfig.class,
        properties = {
                "ORDER_SERVICE_URL=http://localhost:${wiremock.server.port}",
                "spring.cloud.openfeign.httpclient.hc5.enabled=true",
                "resilience4j.circuitbreaker.configs.default.minimum-number-of-calls=100"
        }
)
@EnableWireMock
class OrderServiceClientTest {

    @Configuration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            LiquibaseAutoConfiguration.class
    })
    @EnableFeignClients(clients = OrderServiceClient.class)
    @Import({OrderServiceClientFallbackFactory.class, FeignClientConfig.class})
    static class TestConfig {
    }

    private static final UUID USER_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID ORDER_ID = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001");

    private static final String ORDER_JSON = """
            {
              "id": "bbbbbbbb-0000-0000-0000-000000000001",
              "orderNumber": "ORD-2026-0001",
              "userId": "aaaaaaaa-0000-0000-0000-000000000001",
              "status": "NEW",
              "deliveryMethod": "COURIER",
              "paymentMethod": "CARD",
              "customer": {
                "firstName": "Jan",
                "lastName": "Kowalski",
                "email": "jan@example.com",
                "phone": "+48123456789"
              },
              "shippingAddress": {
                "street": "Marszalkowska 1",
                "city": "Warszawa",
                "postalCode": "00-001",
                "country": "PL"
              },
              "billingAddress": null,
              "items": [],
              "itemsTotal": 3999.00,
              "deliveryCost": 15.00,
              "grandTotal": 4014.00,
              "currency": "PLN",
              "invoice": null,
              "createdAt": "2026-11-01T20:00:00Z"
            }
            """;

    @InjectWireMock
    private WireMockServer wireMock;

    @Autowired
    private OrderServiceClient orderServiceClient;

    @BeforeEach
    void resetWireMock() {
        wireMock.resetAll();
    }

    @Test
    @DisplayName("checkout: 200 response places order")
    void checkout_Success() {
        wireMock.stubFor(post(urlPathEqualTo("/orders/" + USER_ID))
                .willReturn(okJson(ORDER_JSON)));

        OrderCheckoutRequest request = new OrderCheckoutRequest(
                new CustomerDto("Jan", "Kowalski", "jan@example.com", "+48123456789"),
                DeliveryMethod.COURIER,
                PaymentMethod.CARD,
                new AddressDto("Marszalkowska 1", "Warszawa", "00-001", "PL"),
                null,
                new BigDecimal("4014.00")
        );

        OrderResponse response = orderServiceClient.checkout(USER_ID, request);

        assertThat(response.id()).isEqualTo(ORDER_ID);
        assertThat(response.orderNumber()).isEqualTo("ORD-2026-0001");
        wireMock.verify(1, postRequestedFor(urlPathEqualTo("/orders/" + USER_ID)));
    }

    @Test
    @DisplayName("getOrders: 200 response returns paged orders")
    void getOrders_Success() {
        String pageJson = """
                {
                  "content": [
                    {
                      "id": "bbbbbbbb-0000-0000-0000-000000000001",
                      "orderNumber": "ORD-2026-0001",
                      "status": "NEW",
                      "grandTotal": 4014.00,
                      "currency": "PLN",
                      "createdAt": "2026-11-01T20:00:00Z"
                    }
                  ],
                  "page": 0,
                  "size": 20,
                  "totalElements": 1,
                  "totalPages": 1
                }
                """;

        wireMock.stubFor(get(urlPathEqualTo("/orders/" + USER_ID))
                .willReturn(okJson(pageJson)));

        OrderPageResponse<OrderSummaryResponse> response = orderServiceClient.getOrders(USER_ID, 0, 20, "createdAt", "DESC");

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().orderNumber()).isEqualTo("ORD-2026-0001");
    }

    @Test
    @DisplayName("getOrder: 200 response returns single order details")
    void getOrder_Success() {
        wireMock.stubFor(get(urlPathEqualTo("/orders/" + USER_ID + "/" + ORDER_ID))
                .willReturn(okJson(ORDER_JSON)));

        OrderResponse response = orderServiceClient.getOrder(USER_ID, ORDER_ID);

        assertThat(response.id()).isEqualTo(ORDER_ID);
        assertThat(response.userId()).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("pay: 200 response pays order")
    void pay_Success() {
        String paidJson = ORDER_JSON.replace("\"NEW\"", "\"PAID\"");
        wireMock.stubFor(post(urlPathEqualTo("/orders/" + USER_ID + "/" + ORDER_ID + "/pay"))
                .willReturn(okJson(paidJson)));

        OrderResponse response = orderServiceClient.pay(USER_ID, ORDER_ID);

        assertThat(response.status()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    @DisplayName("cancel: 200 response cancels order")
    void cancel_Success() {
        String cancelledJson = ORDER_JSON.replace("\"NEW\"", "\"CANCELLED\"");
        wireMock.stubFor(post(urlPathEqualTo("/orders/" + USER_ID + "/" + ORDER_ID + "/cancel"))
                .willReturn(okJson(cancelledJson)));

        OrderResponse response = orderServiceClient.cancel(USER_ID, ORDER_ID);

        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("retryInvoice: 200 response retries invoice creation")
    void retryInvoice_Success() {
        wireMock.stubFor(post(urlPathEqualTo("/orders/" + USER_ID + "/" + ORDER_ID + "/invoice"))
                .willReturn(okJson(ORDER_JSON)));

        OrderResponse response = orderServiceClient.retryInvoice(USER_ID, ORDER_ID);

        assertThat(response.id()).isEqualTo(ORDER_ID);
    }

    @Test
    @DisplayName("ship: 200 response ships order")
    void ship_Success() {
        String shippedJson = ORDER_JSON.replace("\"NEW\"", "\"SHIPPED\"");
        wireMock.stubFor(post(urlPathEqualTo("/orders/" + ORDER_ID + "/ship"))
                .willReturn(okJson(shippedJson)));

        OrderResponse response = orderServiceClient.ship(ORDER_ID);

        assertThat(response.status()).isEqualTo(OrderStatus.SHIPPED);
    }

    @Test
    @DisplayName("404 response is rethrown as FeignException.NotFound")
    void clientError_Propagated() {
        wireMock.stubFor(get(urlPathEqualTo("/orders/" + USER_ID + "/" + ORDER_ID))
                .willReturn(notFound()));

        assertThatThrownBy(() -> orderServiceClient.getOrder(USER_ID, ORDER_ID))
                .isInstanceOf(FeignException.NotFound.class);
    }

    @Test
    @DisplayName("500 response triggers fallback factory and throws DownstreamServiceUnavailableException")
    void serverError_TriggersFallback() {
        wireMock.stubFor(get(urlPathEqualTo("/orders/" + USER_ID + "/" + ORDER_ID))
                .willReturn(serverError()));

        assertThatThrownBy(() -> orderServiceClient.getOrder(USER_ID, ORDER_ID))
                .isInstanceOf(DownstreamServiceUnavailableException.class)
                .hasMessageContaining("order-service");
    }
}

