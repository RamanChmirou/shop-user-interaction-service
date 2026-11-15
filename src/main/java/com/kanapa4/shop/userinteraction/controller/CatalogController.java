package com.kanapa4.shop.userinteraction.controller;

import com.kanapa4.shop.userinteraction.dto.PageResponse;
import com.kanapa4.shop.userinteraction.dto.PriceQuoteDto;
import com.kanapa4.shop.userinteraction.dto.PriceQuoteRequest;
import com.kanapa4.shop.userinteraction.dto.ProductConfigurationDto;
import com.kanapa4.shop.userinteraction.dto.ProductDto;
import com.kanapa4.shop.userinteraction.dto.ProductRequest;
import com.kanapa4.shop.userinteraction.dto.ProductSummaryDto;
import com.kanapa4.shop.userinteraction.dto.ProductType;
import com.kanapa4.shop.userinteraction.service.CatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Catalog", description = "Product catalog and configuration browsing")
public class CatalogController {

    private final CatalogService catalogService;

    @GetMapping
    @Operation(summary = "Get list of products", description = "Retrieves active products with pagination and sorting.")
    public PageResponse<ProductSummaryDto> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sort) {
        log.info("GET /api/products page={} size={} sort={}", page, size, sort);
        return catalogService.getAll(page, size, sort);
    }

    @GetMapping(params = "type")
    @Operation(summary = "Get products by type", description = "Retrieves products filtered by type: COMPUTER, SMARTPHONE, ELECTRONICS.")
    public PageResponse<ProductSummaryDto> getByType(
            @RequestParam ProductType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sort) {
        log.info("GET /api/products type={} page={} size={} sort={}", type, page, size, sort);
        return catalogService.getByType(type, page, size, sort);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product details by ID")
    public ProductDto getById(@PathVariable UUID id) {
        log.info("GET /api/products/{}", id);
        return catalogService.getById(id);
    }

    @GetMapping("/{id}/configuration")
    @Operation(summary = "Get product configuration options")
    public ProductConfigurationDto getConfiguration(@PathVariable UUID id) {
        log.info("GET /api/products/{}/configuration", id);
        return catalogService.getConfiguration(id);
    }

    @PostMapping("/{id}/price-quote")
    @Operation(summary = "Calculate price quote for configured product options")
    public PriceQuoteDto quote(@PathVariable UUID id, @Valid @RequestBody PriceQuoteRequest request) {
        log.info("POST /api/products/{}/price-quote", id);
        return catalogService.quote(id, request);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create product (Admin only)")
    public ProductDto createProduct(@Valid @RequestBody ProductRequest request) {
        log.info("POST /api/products (Admin) name={}", request.name());
        return catalogService.createProduct(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update product (Admin only)")
    public ProductDto updateProduct(@PathVariable UUID id, @Valid @RequestBody ProductRequest request) {
        log.info("PUT /api/products/{} (Admin)", id);
        return catalogService.updateProduct(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete product (Admin only)")
    public void deleteProduct(@PathVariable UUID id) {
        log.info("DELETE /api/products/{} (Admin)", id);
        catalogService.deleteProduct(id);
    }
}
