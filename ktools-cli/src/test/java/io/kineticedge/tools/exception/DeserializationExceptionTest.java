package io.kineticedge.tools.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DeserializationExceptionTest {


  @Test
  void testDeserializationExceptionNoArg() {
    Exception exception = assertThrows(DeserializationException.class, () -> {
      throw new DeserializationException();
    });
    assertNull(exception.getMessage());
  }

  @Test
  void testDeserializationExceptionWithMessage() {
    String message = "Test exception message";
    Exception exception = assertThrows(DeserializationException.class, () -> {
      throw new DeserializationException(message);
    });
    assertEquals(message, exception.getMessage());
  }

  @Test
  void testDeserializationExceptionWithMessageAndCause() {
    String message = "Test exception message";
    Throwable cause = new Exception("Cause message");
    Exception exception = assertThrows(DeserializationException.class, () -> {
      throw new DeserializationException(message, cause);
    });
    assertEquals(message, exception.getMessage());
    assertEquals(cause, exception.getCause());
  }

  @Test
  void testDeserializationExceptionWithCause() {
    Throwable cause = new Exception("Cause message");
    Exception exception = assertThrows(DeserializationException.class, () -> {
      throw new DeserializationException(cause);
    });
    assertEquals(cause, exception.getCause());
  }

}