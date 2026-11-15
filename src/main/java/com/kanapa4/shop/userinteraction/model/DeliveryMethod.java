package com.kanapa4.shop.userinteraction.model;

import java.math.BigDecimal;

public enum DeliveryMethod {
    COURIER(new BigDecimal("15.00")),
    PARCEL_LOCKER(new BigDecimal("9.99")),
    PICKUP(BigDecimal.ZERO);

    private final BigDecimal cost;

    DeliveryMethod(BigDecimal cost) {
        this.cost = cost;
    }

    public BigDecimal getCost() {
        return cost;
    }
}

