package com.kanapa4.shop.userinteraction.exception;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.circuitbreaker.NoFallbackAvailableException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserInteractionServiceException.class)
    public ResponseEntity<ProblemDetail> handleUserInteractionServiceException(UserInteractionServiceException ex) {
        log.warn("Request failed with {}: {}", ex.getStatus(), ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
        return ResponseEntity.status(ex.getStatus()).body(problemDetail);
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ProblemDetail> handleFeignException(FeignException ex) {
        int status = ex.status();
        HttpStatusCode httpStatus = status > 0 ? HttpStatusCode.valueOf(status) : HttpStatus.BAD_GATEWAY;
        String content = ex.contentUTF8();
        String detail = (content != null && !content.isBlank()) ? content : ex.getMessage();
        log.warn("Downstream service error [status={}]: {}", status, detail);
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(httpStatus, detail);
        return ResponseEntity.status(httpStatus).body(problemDetail);
    }

    @ExceptionHandler(NoFallbackAvailableException.class)
    public ResponseEntity<ProblemDetail> handleNoFallback(NoFallbackAvailableException ex) {
        if (ex.getCause() instanceof FeignException feignEx) {
            return handleFeignException(feignEx);
        }
        if (ex.getCause() instanceof UserInteractionServiceException serviceEx) {
            return handleUserInteractionServiceException(serviceEx);
        }
        log.error("Circuit breaker exception without fallback: {}", ex.getMessage(), ex);
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "Downstream service unavailable");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problemDetail);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("Validation failed: {}", details);
        return ResponseEntity.badRequest()
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, details));
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ProblemDetail> handleMalformedRequest(Exception ex) {
        log.warn("Malformed request: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Malformed request"));
    }

    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, DataIntegrityViolationException.class})
    public ResponseEntity<ProblemDetail> handleConcurrentModification(Exception ex) {
        log.warn("Concurrent modification: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                        "Resource was modified concurrently, please retry"));
    }
}
