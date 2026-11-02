package com.kanapa4.shop.userinteraction.exception;

import org.springframework.http.HttpStatus;

public class EmptyCartException extends UserInteractionServiceException {

    public EmptyCartException() {
        super(HttpStatus.BAD_REQUEST, "Cart is empty");
    }
}
