package com.kanapa4.shop.userinteraction.service;

import com.kanapa4.shop.userinteraction.client.ProductServiceClient;
import com.kanapa4.shop.userinteraction.dto.PageResponse;
import com.kanapa4.shop.userinteraction.dto.PriceQuoteDto;
import com.kanapa4.shop.userinteraction.dto.PriceQuoteRequest;
import com.kanapa4.shop.userinteraction.dto.ProductConfigurationDto;
import com.kanapa4.shop.userinteraction.dto.ProductDto;
import com.kanapa4.shop.userinteraction.dto.ProductRequest;
import com.kanapa4.shop.userinteraction.dto.ProductSummaryDto;
import com.kanapa4.shop.userinteraction.dto.ProductType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogService {

    private final ProductServiceClient productServiceClient;

    public PageResponse<ProductSummaryDto> getAll(int page, int size, String sort) {
        log.info("Fetching products page={} size={} sort={}", page, size, sort);
        return productServiceClient.getAll(page, size, sort);
    }

    public PageResponse<ProductSummaryDto> getByType(ProductType type, int page, int size, String sort) {
        log.info("Fetching products by type={} page={} size={} sort={}", type, page, size, sort);
        return productServiceClient.getByType(type, page, size, sort);
    }

    public ProductDto getById(UUID id) {
        log.info("Fetching product id={}", id);
        return productServiceClient.getById(id);
    }

    public ProductConfigurationDto getConfiguration(UUID id) {
        log.info("Fetching configuration for product id={}", id);
        return productServiceClient.getConfiguration(id);
    }

    public PriceQuoteDto quote(UUID id, PriceQuoteRequest request) {
        log.info("Calculating price quote for product id={}", id);
        return productServiceClient.quote(id, request);
    }

    public ProductDto createProduct(ProductRequest request) {
        log.info("Creating product name={}", request.name());
        return productServiceClient.createProduct(request);
    }

    public ProductDto updateProduct(UUID id, ProductRequest request) {
        log.info("Updating product id={}", id);
        return productServiceClient.updateProduct(id, request);
    }

    public void deleteProduct(UUID id) {
        log.info("Deleting product id={}", id);
        productServiceClient.deleteProduct(id);
    }
}
