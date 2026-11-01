package com.kanapa4.shop.userinteraction.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.kanapa4.shop.userinteraction.client.config.FeignClientConfig;
import com.kanapa4.shop.userinteraction.client.fallback.ProductServiceClientFallbackFactory;
import com.kanapa4.shop.userinteraction.dto.PageResponse;
import com.kanapa4.shop.userinteraction.dto.PriceDto;
import com.kanapa4.shop.userinteraction.dto.PriceQuoteDto;
import com.kanapa4.shop.userinteraction.dto.PriceQuoteRequest;
import com.kanapa4.shop.userinteraction.dto.PriceRequest;
import com.kanapa4.shop.userinteraction.dto.ProductConfigurationDto;
import com.kanapa4.shop.userinteraction.dto.ProductDto;
import com.kanapa4.shop.userinteraction.dto.ProductRequest;
import com.kanapa4.shop.userinteraction.dto.ProductSummaryDto;
import com.kanapa4.shop.userinteraction.dto.ProductType;
import com.kanapa4.shop.userinteraction.dto.SelectedOptionRequest;
import com.kanapa4.shop.userinteraction.dto.VatRate;
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
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(
        classes = ProductServiceClientTest.TestConfig.class,
        properties = {
                "PRODUCT_SERVICE_URL=http://localhost:${wiremock.server.port}",
                "spring.cloud.openfeign.httpclient.hc5.enabled=true",
                "resilience4j.circuitbreaker.configs.default.minimum-number-of-calls=100"
        }
)
@EnableWireMock
class ProductServiceClientTest {

    @Configuration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            LiquibaseAutoConfiguration.class
    })
    @EnableFeignClients(clients = ProductServiceClient.class)
    @Import({ProductServiceClientFallbackFactory.class, FeignClientConfig.class})
    static class TestConfig {
    }

    private static final UUID PRODUCT_ID = UUID.fromString("11111111-0000-0000-0000-000000000001");

    private static final String PRODUCT_JSON = """
            {
              "id": "11111111-0000-0000-0000-000000000001",
              "name": "Laptop Pro 15",
              "description": "High performance laptop",
              "type": "COMPUTER",
              "price": { "amount": 3999.00, "currency": "PLN", "vatRate": "VAT_23" },
              "attributes": { "screen": "15.6 inch" },
              "active": true
            }
            """;

    @InjectWireMock
    private WireMockServer wireMock;

    @Autowired
    private ProductServiceClient productServiceClient;

    @BeforeEach
    void resetWireMock() {
        wireMock.resetAll();
    }

    @Test
    @DisplayName("getAll: 200 response returns paged products")
    void getAll_Success() {
        String pageJson = """
                {
                  "content": [ %s ],
                  "number": 0,
                  "size": 20,
                  "totalElements": 1,
                  "totalPages": 1
                }
                """.formatted(PRODUCT_JSON);

        wireMock.stubFor(get(urlPathEqualTo("/products"))
                .willReturn(okJson(pageJson)));

        PageResponse<ProductSummaryDto> response = productServiceClient.getAll(0, 20, "name");

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().name()).isEqualTo("Laptop Pro 15");
    }

    @Test
    @DisplayName("getByType: 200 response returns products filtered by type")
    void getByType_Success() {
        String pageJson = """
                {
                  "content": [ %s ],
                  "number": 0,
                  "size": 20,
                  "totalElements": 1,
                  "totalPages": 1
                }
                """.formatted(PRODUCT_JSON);

        wireMock.stubFor(get(urlPathEqualTo("/products"))
                .willReturn(okJson(pageJson)));

        PageResponse<ProductSummaryDto> response = productServiceClient.getByType(ProductType.COMPUTER, 0, 20, "name");

        assertThat(response.content()).hasSize(1);
    }

    @Test
    @DisplayName("getById: 200 response returns product details")
    void getById_Success() {
        wireMock.stubFor(get(urlPathEqualTo("/products/" + PRODUCT_ID))
                .willReturn(okJson(PRODUCT_JSON)));

        ProductDto response = productServiceClient.getById(PRODUCT_ID);

        assertThat(response.id()).isEqualTo(PRODUCT_ID);
        assertThat(response.name()).isEqualTo("Laptop Pro 15");
    }

    @Test
    @DisplayName("getConfiguration: 200 response returns option groups")
    void getConfiguration_Success() {
        String configJson = """
                {
                  "productId": "11111111-0000-0000-0000-000000000001",
                  "productName": "Laptop Pro 15",
                  "basePrice": { "amount": 3999.00, "currency": "PLN", "vatRate": "VAT_23" },
                  "optionGroups": []
                }
                """;

        wireMock.stubFor(get(urlPathEqualTo("/products/" + PRODUCT_ID + "/configuration"))
                .willReturn(okJson(configJson)));

        ProductConfigurationDto response = productServiceClient.getConfiguration(PRODUCT_ID);

        assertThat(response.productId()).isEqualTo(PRODUCT_ID);
        assertThat(response.optionGroups()).isEmpty();
    }

    @Test
    @DisplayName("quote: 200 response returns calculated price quote")
    void quote_Success() {
        String quoteJson = """
                {
                  "productId": "11111111-0000-0000-0000-000000000001",
                  "productName": "Laptop Pro 15",
                  "unitPrice": { "amount": 4399.00, "currency": "PLN", "vatRate": "VAT_23" },
                  "selectedOptions": []
                }
                """;

        wireMock.stubFor(post(urlPathEqualTo("/products/" + PRODUCT_ID + "/price-quote"))
                .willReturn(okJson(quoteJson)));

        PriceQuoteRequest request = new PriceQuoteRequest(List.of(new SelectedOptionRequest("RAM", "RAM_32")));
        PriceQuoteDto response = productServiceClient.quote(PRODUCT_ID, request);

        assertThat(response.unitPrice().amount()).isEqualByComparingTo(new BigDecimal("4399.00"));
    }

    @Test
    @DisplayName("createProduct: 201 response creates product")
    void createProduct_Success() {
        wireMock.stubFor(post(urlPathEqualTo("/products"))
                .willReturn(aResponse().withStatus(201).withBody(PRODUCT_JSON).withHeader("Content-Type", "application/json")));

        ProductRequest request = new ProductRequest(
                "Laptop Pro 15",
                "High performance laptop",
                ProductType.COMPUTER,
                new PriceRequest(new BigDecimal("3999.00"), "PLN", VatRate.VAT_23),
                Map.of("screen", "15.6 inch"),
                List.of()
        );

        ProductDto response = productServiceClient.createProduct(request);

        assertThat(response.id()).isEqualTo(PRODUCT_ID);
        wireMock.verify(1, postRequestedFor(urlPathEqualTo("/products")));
    }

    @Test
    @DisplayName("updateProduct: 200 response updates product")
    void updateProduct_Success() {
        wireMock.stubFor(put(urlPathEqualTo("/products/" + PRODUCT_ID))
                .willReturn(okJson(PRODUCT_JSON)));

        ProductRequest request = new ProductRequest(
                "Laptop Pro 15",
                "Updated description",
                ProductType.COMPUTER,
                new PriceRequest(new BigDecimal("3999.00"), "PLN", VatRate.VAT_23),
                Map.of(),
                List.of()
        );

        ProductDto response = productServiceClient.updateProduct(PRODUCT_ID, request);

        assertThat(response.id()).isEqualTo(PRODUCT_ID);
    }

    @Test
    @DisplayName("deleteProduct: 204 No Content deletes product")
    void deleteProduct_Success() {
        wireMock.stubFor(delete(urlPathEqualTo("/products/" + PRODUCT_ID))
                .willReturn(aResponse().withStatus(204)));

        productServiceClient.deleteProduct(PRODUCT_ID);

        wireMock.verify(1, deleteRequestedFor(urlPathEqualTo("/products/" + PRODUCT_ID)));
    }

    @Test
    @DisplayName("404 response is rethrown as FeignException.NotFound")
    void clientError_Propagated() {
        wireMock.stubFor(get(urlPathEqualTo("/products/" + PRODUCT_ID))
                .willReturn(notFound()));

        assertThatThrownBy(() -> productServiceClient.getById(PRODUCT_ID))
                .isInstanceOf(FeignException.NotFound.class);
    }

    @Test
    @DisplayName("500 response triggers fallback factory and throws DownstreamServiceUnavailableException")
    void serverError_TriggersFallback() {
        wireMock.stubFor(get(urlPathEqualTo("/products/" + PRODUCT_ID))
                .willReturn(serverError()));

        assertThatThrownBy(() -> productServiceClient.getById(PRODUCT_ID))
                .isInstanceOf(DownstreamServiceUnavailableException.class)
                .hasMessageContaining("product-service");
    }
}

