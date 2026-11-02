package com.kanapa4.shop.userinteraction.dto;

import com.kanapa4.shop.userinteraction.model.DeliveryMethod;
import com.kanapa4.shop.userinteraction.model.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record OrderCheckoutRequest(
        @NotNull @Valid CustomerDto customer,
        @NotNull DeliveryMethod deliveryMethod,
        @NotNull PaymentMethod paymentMethod,
        @Valid AddressDto shippingAddress,
        @Valid AddressDto billingAddress,
        @NotNull @Positive BigDecimal expectedTotal) {
}
