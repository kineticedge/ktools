package io.kineticedge.tools.util;

import io.kineticedge.tools.console.Color;
import picocli.CommandLine;

public class ColorConverter implements CommandLine.ITypeConverter<Color> {

  @Override
  public Color convert(String value) {
    try {
      return Color.valueOf(value.toUpperCase());
    } catch (Exception e) {
      throw new CommandLine.TypeConversionException(String.format("%s is not a valid ansi color.", value));
    }
  }
}
