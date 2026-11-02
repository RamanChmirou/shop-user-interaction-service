package com.kanapa4.shop.userinteraction.client.fallback;

import com.kanapa4.shop.userinteraction.client.ProductServiceClient;
import com.kanapa4.shop.userinteraction.dto.PageResponse;
import com.kanapa4.shop.userinteraction.dto.PriceQuoteDto;
import com.kanapa4.shop.userinteraction.dto.PriceQuoteRequest;
import com.kanapa4.shop.userinteraction.dto.ProductConfigurationDto;
import com.kanapa4.shop.userinteraction.dto.ProductDto;
import com.kanapa4.shop.userinteraction.dto.ProductRequest;
import com.kanapa4.shop.userinteraction.dto.ProductSummaryDto;
import com.kanapa4.shop.userinteraction.dto.ProductType;
import com.kanapa4.shop.userinteraction.exception.DownstreamServiceUnavailableException;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class ProductServiceClientFallbackFactory implements FallbackFactory<ProductServiceClient> {

    private static final String SERVICE_NAME = "product-service";

    @Override
    public ProductServiceClient create(Throwable cause) {
        return new ProductServiceClient() {
            @Override
            public PageResponse<ProductSummaryDto> getAll(int page, int size, String sort) {
                log.error("Fallback for {}.getAll: {}", SERVICE_NAME, cause.toString());
                throw failure(cause);
            }

            @Override
            public PageResponse<ProductSummaryDto> getByType(ProductType type, int page, int size, String sort) {
                log.error("Fallback for {}.getByType (type={}): {}", SERVICE_NAME, type, cause.toString());
                throw failure(cause);
            }

            @Override
            public ProductDto getById(UUID id) {
                log.error("Fallback for {}.getById (id={}): {}", SERVICE_NAME, id, cause.toString());
                throw failure(cause);
            }

            @Override
            public ProductConfigurationDto getConfiguration(UUID id) {
                log.error("Fallback for {}.getConfiguration (id={}): {}", SERVICE_NAME, id, cause.toString());
                throw failure(cause);
            }

            @Override
            public PriceQuoteDto quote(UUID id, PriceQuoteRequest request) {
                log.error("Fallback for {}.quote (id={}): {}", SERVICE_NAME, id, cause.toString());
                throw failure(cause);
            }

            @Override
            public ProductDto createProduct(ProductRequest request) {
                log.error("Fallback for {}.createProduct: {}", SERVICE_NAME, cause.toString());
                throw failure(cause);
            }

            @Override
            public ProductDto updateProduct(UUID id, ProductRequest request) {
                log.error("Fallback for {}.updateProduct (id={}): {}", SERVICE_NAME, id, cause.toString());
                throw failure(cause);
            }

            @Override
            public void deleteProduct(UUID id) {
                log.error("Fallback for {}.deleteProduct (id={}): {}", SERVICE_NAME, id, cause.toString());
                throw failure(cause);
            }
        };
    }

    private RuntimeException failure(Throwable cause) {
        Throwable actual = cause;
        while (actual.getCause() != null && !(actual instanceof FeignException)) {
            actual = actual.getCause();
        }
        if (actual instanceof FeignException feignEx && feignEx.status() >= 400 && feignEx.status() < 500) {
            return feignEx;
        }
        return new DownstreamServiceUnavailableException(SERVICE_NAME);
    }
}
