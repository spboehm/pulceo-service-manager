package dev.pulceo.prm.api.exception;

public class PnaApiException extends Exception {

    public PnaApiException() {
    }

    public PnaApiException(String message) {
        super(message);
    }

    public PnaApiException(String message, Throwable cause) {
        super(message, cause);
    }

    public PnaApiException(Throwable cause) {
        super(cause);
    }

    public PnaApiException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
