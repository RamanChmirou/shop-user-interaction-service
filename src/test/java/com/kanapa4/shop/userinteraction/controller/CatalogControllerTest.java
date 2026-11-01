package com.kanapa4.shop.userinteraction.controller;

import com.kanapa4.shop.userinteraction.dto.OptionGroupDto;
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
import com.kanapa4.shop.userinteraction.exception.GlobalExceptionHandler;
import com.kanapa4.shop.userinteraction.service.CatalogService;
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
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CatalogController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CatalogControllerTest {

    private static final UUID PRODUCT_ID = UUID.fromString("11111111-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
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
    @DisplayName("GET /api/products returns 200 OK with page of products")
    void getAll_ReturnsOk() throws Exception {
        PageResponse<ProductSummaryDto> page = new PageResponse<>(List.of(sampleSummary()), 0, 20, 1, 1);
        when(catalogService.getAll(0, 20, "name")).thenReturn(page);

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(PRODUCT_ID.toString()))
                .andExpect(jsonPath("$.content[0].name").value("Laptop Pro 15"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(catalogService).getAll(0, 20, "name");
    }

    @Test
    @DisplayName("GET /api/products?type=COMPUTER returns 200 OK filtered by type")
    void getByType_ReturnsOk() throws Exception {
        PageResponse<ProductSummaryDto> page = new PageResponse<>(List.of(sampleSummary()), 0, 20, 1, 1);
        when(catalogService.getByType(ProductType.COMPUTER, 0, 20, "name")).thenReturn(page);

        mockMvc.perform(get("/api/products").param("type", "COMPUTER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("COMPUTER"));

        verify(catalogService).getByType(ProductType.COMPUTER, 0, 20, "name");
    }

    @Test
    @DisplayName("GET /api/products/{id} returns 200 OK with product details")
    void getById_ReturnsOk() throws Exception {
        when(catalogService.getById(PRODUCT_ID)).thenReturn(sampleProduct());

        mockMvc.perform(get("/api/products/{id}", PRODUCT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PRODUCT_ID.toString()))
                .andExpect(jsonPath("$.name").value("Laptop Pro 15"));

        verify(catalogService).getById(PRODUCT_ID);
    }

    @Test
    @DisplayName("GET /api/products/{id}/configuration returns 200 OK with option groups")
    void getConfiguration_ReturnsOk() throws Exception {
        ProductConfigurationDto config = new ProductConfigurationDto(
                PRODUCT_ID,
                "Laptop Pro 15",
                new PriceDto(new BigDecimal("3999.00"), "PLN", VatRate.VAT_23),
                List.of()
        );
        when(catalogService.getConfiguration(PRODUCT_ID)).thenReturn(config);

        mockMvc.perform(get("/api/products/{id}/configuration", PRODUCT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(PRODUCT_ID.toString()));

        verify(catalogService).getConfiguration(PRODUCT_ID);
    }

    @Test
    @DisplayName("POST /api/products/{id}/price-quote returns 200 OK with calculated quote")
    void quote_ValidRequest_ReturnsOk() throws Exception {
        PriceQuoteRequest request = new PriceQuoteRequest(List.of(new SelectedOptionRequest("RAM", "RAM_32")));
        PriceQuoteDto quote = new PriceQuoteDto(
                PRODUCT_ID,
                "Laptop Pro 15",
                new PriceDto(new BigDecimal("4399.00"), "PLN", VatRate.VAT_23),
                List.of()
        );
        when(catalogService.quote(eq(PRODUCT_ID), any(PriceQuoteRequest.class))).thenReturn(quote);

        mockMvc.perform(post("/api/products/{id}/price-quote", PRODUCT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unitPrice.amount").value(4399.00));

        verify(catalogService).quote(eq(PRODUCT_ID), any(PriceQuoteRequest.class));
    }

    @Test
    @DisplayName("POST /api/products returns 201 CREATED for valid product")
    void createProduct_ValidRequest_ReturnsCreated() throws Exception {
        ProductRequest request = new ProductRequest(
                "Monitor 27",
                "4K IPS Monitor",
                ProductType.ELECTRONICS,
                new PriceRequest(new BigDecimal("1499.00"), "PLN", VatRate.VAT_23),
                Map.of(),
                List.of()
        );
        when(catalogService.createProduct(any(ProductRequest.class))).thenReturn(sampleProduct());

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(PRODUCT_ID.toString()));

        verify(catalogService).createProduct(any(ProductRequest.class));
    }

    @Test
    @DisplayName("POST /api/products with blank name returns 400 Bad Request")
    void createProduct_BlankName_ReturnsBadRequest() throws Exception {
        String invalidJson = """
                {
                    "name": "",
                    "type": "ELECTRONICS",
                    "price": { "amount": 100.0, "currency": "PLN", "vatRate": "VAT_23" }
                }
                """;

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/products/{id} returns 200 OK with updated product")
    void updateProduct_ValidRequest_ReturnsOk() throws Exception {
        ProductRequest request = new ProductRequest(
                "Updated Laptop",
                "Updated description",
                ProductType.COMPUTER,
                new PriceRequest(new BigDecimal("3899.00"), "PLN", VatRate.VAT_23),
                Map.of(),
                List.of()
        );
        when(catalogService.updateProduct(eq(PRODUCT_ID), any(ProductRequest.class))).thenReturn(sampleProduct());

        mockMvc.perform(put("/api/products/{id}", PRODUCT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(catalogService).updateProduct(eq(PRODUCT_ID), any(ProductRequest.class));
    }

    @Test
    @DisplayName("DELETE /api/products/{id} returns 204 No Content")
    void deleteProduct_ReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/products/{id}", PRODUCT_ID))
                .andExpect(status().isNoContent());

        verify(catalogService).deleteProduct(PRODUCT_ID);
    }
}

