package com.kanapa4.shop.userinteraction.controller;

import com.kanapa4.shop.userinteraction.dto.CheckoutSummaryResponse;
import com.kanapa4.shop.userinteraction.dto.OrderResponse;
import com.kanapa4.shop.userinteraction.model.CheckoutData;
import com.kanapa4.shop.userinteraction.service.CheckoutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/checkout")
@RequiredArgsConstructor
@Tag(name = "Checkout", description = "Checkout draft and order placement flow (Screens 2 & 3)")
public class CheckoutController {

    private final CheckoutService checkoutService;

    @GetMapping("/draft")
    @Operation(summary = "Get checkout draft details (Screen 2)")
    public CheckoutData getDraft() {
        log.info("GET /api/checkout/draft");
        return checkoutService.getDraft().orElse(null);
    }

    @PutMapping("/draft")
    @Operation(summary = "Save or update checkout draft details (Screen 2)")
    public CheckoutData saveDraft(@Valid @RequestBody CheckoutData draft) {
        log.info("PUT /api/checkout/draft");
        return checkoutService.saveDraft(draft);
    }

    @GetMapping("/summary")
    @Operation(summary = "Get checkout summary aggregating cart and draft data (Screen 3)")
    public CheckoutSummaryResponse getSummary() {
        log.info("GET /api/checkout/summary");
        return checkoutService.getSummary();
    }

    @PostMapping("/submit")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Submit order from active cart and checkout draft (Screen 3)")
    public OrderResponse submitOrder() {
        log.info("POST /api/checkout/submit");
        return checkoutService.submitOrder();
    }
}
