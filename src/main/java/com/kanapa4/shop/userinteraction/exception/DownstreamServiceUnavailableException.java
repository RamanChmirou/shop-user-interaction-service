package com.kanapa4.shop.userinteraction.exception;

import org.springframework.http.HttpStatus;

public class DownstreamServiceUnavailableException extends UserInteractionServiceException {

    public DownstreamServiceUnavailableException(String serviceName) {
        super(HttpStatus.SERVICE_UNAVAILABLE, serviceName + " is currently unavailable, please try again later");
    }
}
