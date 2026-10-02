package com.dismal.distribuciones.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a customer's credit limit is exceeded.
 * Results in a 402 Payment Required HTTP status, indicating the action could not be
 * completed until payment or sufficient credit is established.
 */
@ResponseStatus(HttpStatus.PAYMENT_REQUIRED)
public class InsufficientCreditException extends RuntimeException {
    public InsufficientCreditException(String message) {
        super(message);
    }
}
