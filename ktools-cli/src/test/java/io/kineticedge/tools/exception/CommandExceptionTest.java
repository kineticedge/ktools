package io.kineticedge.tools.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CommandExceptionTest {


  @Test
  void testCommandExceptionNoArg() {
    Exception exception = assertThrows(CommandException.class, () -> {
      throw new CommandException();
    });
    assertNull(exception.getMessage());
  }

  @Test
  void testCommandExceptionWithMessage() {
    String message = "Test exception message";
    Exception exception = assertThrows(CommandException.class, () -> {
      throw new CommandException(message);
    });
    assertEquals(message, exception.getMessage());
  }

  @Test
  void testCommandExceptionWithMessageAndCause() {
    String message = "Test exception message";
    Throwable cause = new Exception("Cause message");
    Exception exception = assertThrows(CommandException.class, () -> {
      throw new CommandException(message, cause);
    });
    assertEquals(message, exception.getMessage());
    assertEquals(cause, exception.getCause());
  }

  @Test
  void testCommandExceptionWithCause() {
    Throwable cause = new Exception("Cause message");
    Exception exception = assertThrows(CommandException.class, () -> {
      throw new CommandException(cause);
    });
    assertEquals(cause, exception.getCause());
  }

}