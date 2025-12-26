package dev.pulceo.prm.api.exception;

public class PrsApiException extends Exception {
    public PrsApiException() {
    }

    public PrsApiException(String message) {
        super(message);
    }

    public PrsApiException(String message, Throwable cause) {
        super(message, cause);
    }

    public PrsApiException(Throwable cause) {
        super(cause);
    }

    public PrsApiException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
