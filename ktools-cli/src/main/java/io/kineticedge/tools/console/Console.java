package io.kineticedge.tools.console;

import java.io.PrintStream;

@SuppressWarnings("resource")
public interface Console {

  PrintStream out();

  PrintStream err();

  default void println(String string) {
    out().println(string);
  }

  default void printf(String string, Object... args) {
    out().printf(string, args);
  }

  default void errPrintln(String string) {
    err().println(string);
  }

  default void errPrintf(String string, Object... args) {
    err().printf(string, args);
  }

}
