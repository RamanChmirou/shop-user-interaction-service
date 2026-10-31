package com.kanapa4.shop.userinteraction.model;

public record CheckoutData(
        String firstName,
        String lastName,
        String email,
        String phone,
        AddressData shippingAddress,
        AddressData billingAddress,
        DeliveryMethod deliveryMethod,
        PaymentMethod paymentMethod,
        String note) {
}
