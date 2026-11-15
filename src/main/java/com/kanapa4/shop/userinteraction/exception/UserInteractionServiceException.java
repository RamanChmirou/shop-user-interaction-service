package com.kanapa4.shop.userinteraction.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class UserInteractionServiceException extends RuntimeException {

    private final HttpStatus status;

    public UserInteractionServiceException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
