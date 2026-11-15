package com.kanapa4.shop.userinteraction.controller;

import com.kanapa4.shop.userinteraction.dto.AddCartItemRequest;
import com.kanapa4.shop.userinteraction.dto.CartResponse;
import com.kanapa4.shop.userinteraction.dto.RepriceResponse;
import com.kanapa4.shop.userinteraction.dto.UpdateQuantityRequest;
import com.kanapa4.shop.userinteraction.service.UserCartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Shopping cart operations for the authenticated customer")
public class CartController {

    private final UserCartService userCartService;

    @GetMapping
    @Operation(summary = "Get user's shopping cart")
    public CartResponse getCart() {
        log.info("GET /api/cart");
        return userCartService.getCart();
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add item to user's shopping cart")
    public CartResponse addItem(@Valid @RequestBody AddCartItemRequest request) {
        log.info("POST /api/cart/items productId={} quantity={}", request.productId(), request.quantity());
        return userCartService.addItem(request);
    }

    @PatchMapping("/items/{itemId}")
    @Operation(summary = "Update quantity of a cart item")
    public CartResponse updateQuantity(
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateQuantityRequest request) {
        log.info("PATCH /api/cart/items/{} quantity={}", itemId, request.quantity());
        return userCartService.updateQuantity(itemId, request);
    }

    @DeleteMapping("/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove item from cart")
    public void removeItem(@PathVariable UUID itemId) {
        log.info("DELETE /api/cart/items/{}", itemId);
        userCartService.removeItem(itemId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Clear shopping cart")
    public void clear() {
        log.info("DELETE /api/cart");
        userCartService.clear();
    }

    @PostMapping("/reprice")
    @Operation(summary = "Reprice shopping cart against latest catalog prices")
    public RepriceResponse reprice() {
        log.info("POST /api/cart/reprice");
        return userCartService.reprice();
    }
}
