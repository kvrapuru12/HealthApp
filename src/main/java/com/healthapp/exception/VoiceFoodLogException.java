package com.healthapp.exception;

import org.springframework.http.HttpStatus;

/**
 * Failure while processing voice-to-food logging; carries a stable {@link #getErrorCode()} for clients
 * and a {@link #getUserMessage()} safe to show in the UI.
 */
public class VoiceFoodLogException extends RuntimeException {

    private final String errorCode;
    private final String userMessage;
    private final HttpStatus httpStatus;

    public VoiceFoodLogException(String errorCode, String userMessage, HttpStatus httpStatus) {
        super(userMessage);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
        this.httpStatus = httpStatus;
    }

    public VoiceFoodLogException(String errorCode, String userMessage, HttpStatus httpStatus, Throwable cause) {
        super(userMessage, cause);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
        this.httpStatus = httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
