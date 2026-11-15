package com.kanapa4.shop.userinteraction.exception;

import org.springframework.http.HttpStatus;

public class CheckoutDraftMissingException extends UserInteractionServiceException {

    public CheckoutDraftMissingException() {
        super(HttpStatus.BAD_REQUEST, "Checkout draft is missing");
    }
}
