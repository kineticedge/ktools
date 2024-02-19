package io.kineticedge.tools.console;


import static java.lang.System.err;
import static java.lang.System.out;

/**
 * CLI tools need to write to system out and system error,
 * even if tools like SonarLint tell you otherwise!
 */
public class StdConsole implements  Console {

  public void out(final String string) {
    out.println(string);
  }

  public void err(final String string) {
    err.println(string);
  }

}
