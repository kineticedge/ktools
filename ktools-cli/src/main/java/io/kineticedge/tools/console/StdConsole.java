package io.kineticedge.tools.console;

import java.io.PrintStream;

import static java.lang.System.err;
import static java.lang.System.out;

/**
 * CLI tools need to write to system out and system error,
 * even if tools like SonarLint tell you otherwise! This abstraction
 * is also leveraged for unit testing.
 */
public class StdConsole implements  Console {

  public PrintStream out() {
    return out;
  }

  public PrintStream err() {
    return err;
  }

}
