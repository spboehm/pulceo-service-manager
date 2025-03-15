package dev.pulceo.prm.exception;

public class TaskServiceException extends Exception {
    public TaskServiceException() {
        super();
    }

    public TaskServiceException(String message) {
        super(message);
    }

    public TaskServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public TaskServiceException(Throwable cause) {
        super(cause);
    }

    protected TaskServiceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
