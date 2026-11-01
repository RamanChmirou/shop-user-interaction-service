package com.kanapa4.shop.userinteraction.dto;

import com.kanapa4.shop.userinteraction.model.DeliveryMethod;
import com.kanapa4.shop.userinteraction.model.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String orderNumber,
        UUID userId,
        OrderStatus status,
        DeliveryMethod deliveryMethod,
        PaymentMethod paymentMethod,
        CustomerDto customer,
        AddressDto shippingAddress,
        AddressDto billingAddress,
        List<OrderItemResponse> items,
        BigDecimal itemsTotal,
        BigDecimal deliveryCost,
        BigDecimal grandTotal,
        String currency,
        InvoiceResponse invoice,
        Instant createdAt) {

    public OrderResponse {
        if (items == null) {
            items = List.of();
        }
    }
}
