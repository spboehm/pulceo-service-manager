package dev.pulceo.prm.util;

public class FileManagerServiceException extends RuntimeException {

    public FileManagerServiceException() {
    }

    public FileManagerServiceException(String message) {
        super(message);
    }

    public FileManagerServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public FileManagerServiceException(Throwable cause) {
        super(cause);
    }

    public FileManagerServiceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
