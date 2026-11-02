package com.kanapa4.shop.userinteraction.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.List;

@Getter
public class CheckoutDraftIncompleteException extends UserInteractionServiceException {

    private final List<String> missingFields;

    public CheckoutDraftIncompleteException(List<String> missingFields) {
        super(HttpStatus.BAD_REQUEST,
                "Checkout draft data is incomplete, missing: " + String.join(", ", missingFields));
        this.missingFields = missingFields;
    }
}