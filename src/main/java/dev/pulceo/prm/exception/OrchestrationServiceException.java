package dev.pulceo.prm.exception;

public class OrchestrationServiceException extends Exception {

    public OrchestrationServiceException() {
    }

    public OrchestrationServiceException(String message) {
        super(message);
    }

    public OrchestrationServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public OrchestrationServiceException(Throwable cause) {
        super(cause);
    }

    public OrchestrationServiceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
    
}
