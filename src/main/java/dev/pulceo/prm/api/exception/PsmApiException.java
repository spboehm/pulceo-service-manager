package dev.pulceo.prm.api.exception;

public class PsmApiException extends RuntimeException {
    public PsmApiException() {
    }

    public PsmApiException(String message) {
        super(message);
    }

    public PsmApiException(String message, Throwable cause) {
        super(message, cause);
    }

    public PsmApiException(Throwable cause) {
        super(cause);
    }

    public PsmApiException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
