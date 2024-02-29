package io.kineticedge.tools.console;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class StdConsoleTest {

  @Test
  void simpleTest() {
    Assertions.assertDoesNotThrow(() -> {
      StdConsole console = new StdConsole();
      console.errPrintln("error");
      console.println("out");
    });
  }
}