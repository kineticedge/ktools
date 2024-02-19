package io.kineticedge.tools.cmd.truncate;

import io.kineticedge.tools.console.Console;

import java.util.ArrayList;
import java.util.List;

public class TestConsole implements Console {

  private List<String> out = new ArrayList<>();
  private List<String> err = new ArrayList<>();

  @Override
  public void out(String string) {
    out.add(string);
  }

  @Override
  public void err(String string) {
    err.add(string);
  }

  public List<String> getOut() {
    return out;
  }

  public List<String> getErr() {
    return err;
  }

  public void clear() {
    out.clear();
    err.clear();
  }

}
