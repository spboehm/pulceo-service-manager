package dev.pulceo.prm.exception;

public class TaskException extends Exception {
  public TaskException() {
    super();
  }

  public TaskException(String message) {
    super(message);
  }

  public TaskException(String message, Throwable cause) {
    super(message, cause);
  }

  public TaskException(Throwable cause) {
    super(cause);
  }

  protected TaskException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
