package dev.pulceo.prm.api.exception;

public class ResourceNotReadyException extends Exception {

    public ResourceNotReadyException() {
    }

    public ResourceNotReadyException(String message) {
        super(message);
    }

    public ResourceNotReadyException(String message, Throwable cause) {
        super(message, cause);
    }

    public ResourceNotReadyException(Throwable cause) {
        super(cause);
    }

    public ResourceNotReadyException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
