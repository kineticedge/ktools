package io.kineticedge.tools.console;

public enum Color {

  NONE(""),
  BLACK("\033[30m"),
  RED("\033[31m"),
  GREEN("\033[32m"),
  YELLOW("\033[33m"),
  BLUE("\033[34m"),
  PURPLE("\033[35m"),
  CYAN("\033[36m"),
  WHITE("\033[37m");

  private final String ascii;

  Color(final String ascii) {
    this.ascii = ascii;
  }

  public String getAscii() {
    return ascii;
  }

}
