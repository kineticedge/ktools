package io.kineticedge.tools.cmd.truncate;

import io.kineticedge.tools.console.Console;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class TestConsole implements Console {

  private ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
  private PrintStream printStream = new PrintStream(outputStream);

  private ByteArrayOutputStream errOutputStream = new ByteArrayOutputStream();
  private PrintStream errPrintStream = new PrintStream(errOutputStream);

  public String asString() {
    return outputStream.toString(StandardCharsets.UTF_8);
  }

  public String errAsString() {
    return errOutputStream.toString(StandardCharsets.UTF_8);
  }

  @Override
  public PrintStream out() {
    return printStream;
  }

  @Override
  public PrintStream err() {
    return errPrintStream;
  }

  public void flush() {
    out().flush();
    err().flush();
  }

  public void reset() {
    outputStream.reset();
    errOutputStream.reset();
  }
}
