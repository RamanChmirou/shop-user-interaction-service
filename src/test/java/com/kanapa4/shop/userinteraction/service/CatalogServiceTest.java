package com.kanapa4.shop.userinteraction.service;

import com.kanapa4.shop.userinteraction.client.ProductServiceClient;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    private static final UUID PRODUCT_ID = UUID.fromString("11111111-0000-0000-0000-000000000001");

    @Mock
    private ProductServiceClient productServiceClient;

    @InjectMocks
    private CatalogService catalogService;

    private ProductSummaryDto sampleSummary() {
        return new ProductSummaryDto(
                PRODUCT_ID,
                "Laptop Pro 15",
                ProductType.COMPUTER,
                new PriceDto(new BigDecimal("3999.00"), "PLN", VatRate.VAT_23)
        );
    }

    private ProductDto sampleProduct() {
        return new ProductDto(
                PRODUCT_ID,
                "Laptop Pro 15",
                "Powerful laptop",
                ProductType.COMPUTER,
                new PriceDto(new BigDecimal("3999.00"), "PLN", VatRate.VAT_23),
                Map.of("RAM", "16GB"),
                true
        );
    }

    @Test
    @DisplayName("getAll delegates to ProductServiceClient and returns paged products")
    void getAll_Success() {
        PageResponse<ProductSummaryDto> expectedPage = new PageResponse<>(List.of(sampleSummary()), 0, 20, 1, 1);
        when(productServiceClient.getAll(0, 20, "name")).thenReturn(expectedPage);

        PageResponse<ProductSummaryDto> result = catalogService.getAll(0, 20, "name");

        assertThat(result).isEqualTo(expectedPage);
        verify(productServiceClient).getAll(0, 20, "name");
    }

    @Test
    @DisplayName("getByType delegates to ProductServiceClient with type filter")
    void getByType_Success() {
        PageResponse<ProductSummaryDto> expectedPage = new PageResponse<>(List.of(sampleSummary()), 0, 20, 1, 1);
        when(productServiceClient.getByType(ProductType.COMPUTER, 0, 20, "name")).thenReturn(expectedPage);

        PageResponse<ProductSummaryDto> result = catalogService.getByType(ProductType.COMPUTER, 0, 20, "name");

        assertThat(result).isEqualTo(expectedPage);
        verify(productServiceClient).getByType(ProductType.COMPUTER, 0, 20, "name");
    }

    @Test
    @DisplayName("getById delegates to ProductServiceClient and returns product details")
    void getById_Success() {
        ProductDto expected = sampleProduct();
        when(productServiceClient.getById(PRODUCT_ID)).thenReturn(expected);

        ProductDto result = catalogService.getById(PRODUCT_ID);

        assertThat(result).isEqualTo(expected);
        verify(productServiceClient).getById(PRODUCT_ID);
    }

    @Test
    @DisplayName("getConfiguration delegates to ProductServiceClient and returns configuration options")
    void getConfiguration_Success() {
        ProductConfigurationDto expected = new ProductConfigurationDto(
                PRODUCT_ID,
                "Laptop Pro 15",
                new PriceDto(new BigDecimal("3999.00"), "PLN", VatRate.VAT_23),
                List.of()
        );
        when(productServiceClient.getConfiguration(PRODUCT_ID)).thenReturn(expected);

        ProductConfigurationDto result = catalogService.getConfiguration(PRODUCT_ID);

        assertThat(result).isEqualTo(expected);
        verify(productServiceClient).getConfiguration(PRODUCT_ID);
    }

    @Test
    @DisplayName("quote delegates to ProductServiceClient and returns price quote")
    void quote_Success() {
        PriceQuoteRequest request = new PriceQuoteRequest(List.of(new SelectedOptionRequest("RAM", "RAM_32")));
        PriceQuoteDto expected = new PriceQuoteDto(
                PRODUCT_ID,
                "Laptop Pro 15",
                new PriceDto(new BigDecimal("4399.00"), "PLN", VatRate.VAT_23),
                List.of()
        );
        when(productServiceClient.quote(PRODUCT_ID, request)).thenReturn(expected);

        PriceQuoteDto result = catalogService.quote(PRODUCT_ID, request);

        assertThat(result).isEqualTo(expected);
        verify(productServiceClient).quote(PRODUCT_ID, request);
    }

    @Test
    @DisplayName("createProduct delegates to ProductServiceClient and returns created product")
    void createProduct_Success() {
        ProductRequest request = new ProductRequest(
                "New Laptop",
                "Description",
                ProductType.COMPUTER,
                new PriceRequest(new BigDecimal("2999.00"), "PLN", VatRate.VAT_23),
                Map.of(),
                List.of()
        );
        ProductDto expected = sampleProduct();
        when(productServiceClient.createProduct(request)).thenReturn(expected);

        ProductDto result = catalogService.createProduct(request);

        assertThat(result).isEqualTo(expected);
        verify(productServiceClient).createProduct(request);
    }

    @Test
    @DisplayName("updateProduct delegates to ProductServiceClient and returns updated product")
    void updateProduct_Success() {
        ProductRequest request = new ProductRequest(
                "Updated Laptop",
                "Updated Description",
                ProductType.COMPUTER,
                new PriceRequest(new BigDecimal("3499.00"), "PLN", VatRate.VAT_23),
                Map.of(),
                List.of()
        );
        ProductDto expected = sampleProduct();
        when(productServiceClient.updateProduct(PRODUCT_ID, request)).thenReturn(expected);

        ProductDto result = catalogService.updateProduct(PRODUCT_ID, request);

        assertThat(result).isEqualTo(expected);
        verify(productServiceClient).updateProduct(PRODUCT_ID, request);
    }

    @Test
    @DisplayName("deleteProduct delegates to ProductServiceClient")
    void deleteProduct_Success() {
        catalogService.deleteProduct(PRODUCT_ID);

        verify(productServiceClient).deleteProduct(PRODUCT_ID);
    }
}
