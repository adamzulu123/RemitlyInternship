package com.remitly.main.RemitlyInternship.Exception;


public class InvalidSwiftCodeException extends RuntimeException {
    public InvalidSwiftCodeException(String message) {
        super(message);
    }

    public InvalidSwiftCodeException(String message, Throwable cause) {
        super(message, cause);
    }
}

