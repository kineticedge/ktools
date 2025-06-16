package io.kineticedge.tools.console;

import java.io.PrintStream;

/**
 * CLI tools need to write to system out and system error,
 * even if tools like SonarLint tell you otherwise! This abstraction
 * is also leveraged for unit testing.
 */
@SuppressWarnings("java:S106")
public class StdConsole implements  Console {

  // If a print stream is provided to Jackson's ObjectMapper, it will close it.
  // However, we do not want Jackson to close the System.out and System.err streams.
  private final PrintStream out = new NonClosingPrintStream(System.out);
  private final PrintStream err = new NonClosingPrintStream(System.err);

  public PrintStream out() {
    return out;
  }

  public PrintStream err() {
    return err;
  }

}
