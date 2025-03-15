package dev.pulceo.prm.api.exception;

public class PrmApiException extends Exception {
    public PrmApiException() {
    }

    public PrmApiException(String message) {
        super(message);
    }

    public PrmApiException(String message, Throwable cause) {
        super(message, cause);
    }

    public PrmApiException(Throwable cause) {
        super(cause);
    }

    public PrmApiException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
