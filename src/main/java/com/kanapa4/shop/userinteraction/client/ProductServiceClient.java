package com.kanapa4.shop.userinteraction.client;

import com.kanapa4.shop.userinteraction.dto.PageResponse;
import com.kanapa4.shop.userinteraction.dto.PriceQuoteDto;
import com.kanapa4.shop.userinteraction.dto.PriceQuoteRequest;
import com.kanapa4.shop.userinteraction.dto.ProductConfigurationDto;
import com.kanapa4.shop.userinteraction.dto.ProductDto;
import com.kanapa4.shop.userinteraction.dto.ProductRequest;
import com.kanapa4.shop.userinteraction.dto.ProductSummaryDto;
import com.kanapa4.shop.userinteraction.dto.ProductType;
import com.kanapa4.shop.userinteraction.client.config.FeignClientConfig;
import com.kanapa4.shop.userinteraction.client.fallback.ProductServiceClientFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;


@FeignClient(
        name = "product-service",
        url = "${PRODUCT_SERVICE_URL:http://localhost:8081}",
        configuration = FeignClientConfig.class,
        fallbackFactory = ProductServiceClientFallbackFactory.class
)
public interface ProductServiceClient {


    @GetMapping("/products")
    PageResponse<ProductSummaryDto> getAll(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sort", defaultValue = "name") String sort);

    @GetMapping(value = "/products", params = "type")
    PageResponse<ProductSummaryDto> getByType(
            @RequestParam("type") ProductType type,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sort", defaultValue = "name") String sort);

    @GetMapping("/products/{id}")
    ProductDto getById(@PathVariable("id") UUID id);

    @GetMapping("/products/{id}/configuration")
    ProductConfigurationDto getConfiguration(@PathVariable("id") UUID id);

    @PostMapping("/products/{id}/price-quote")
    PriceQuoteDto quote(@PathVariable("id") UUID id, @RequestBody PriceQuoteRequest request);

    @PostMapping("/products")
    ProductDto createProduct(@RequestBody ProductRequest request);

    @PutMapping("/products/{id}")
    ProductDto updateProduct(@PathVariable("id") UUID id, @RequestBody ProductRequest request);

    @DeleteMapping("/products/{id}")
    void deleteProduct(@PathVariable("id") UUID id);
}
