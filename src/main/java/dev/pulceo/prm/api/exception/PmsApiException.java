package dev.pulceo.prm.api.exception;

public class PmsApiException extends Exception {
    public PmsApiException() {
    }

    public PmsApiException(String message) {
        super(message);
    }

    public PmsApiException(String message, Throwable cause) {
        super(message, cause);
    }

    public PmsApiException(Throwable cause) {
        super(cause);
    }

    public PmsApiException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
