package io.kineticedge.tools.console;

import java.io.PrintStream;

/**
 * To be used on a System.out or System.err PrintStream to prevent it fron being closed.
 */
public class NonClosingPrintStream extends PrintStream {

  public NonClosingPrintStream(final PrintStream out) {
    super(out);
  }

  @Override
  public void close() {
    // do not close the stream.
  }

}