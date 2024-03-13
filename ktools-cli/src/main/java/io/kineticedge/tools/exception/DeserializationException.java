package io.kineticedge.tools.exception;

public class DeserializationException extends RuntimeException {

  public DeserializationException() {
  }

  public DeserializationException(String message) {
    super(message);
  }

  public DeserializationException(String message, Throwable cause) {
    super(message, cause);
  }

  public DeserializationException(Throwable cause) {
    super(cause);
  }
}
