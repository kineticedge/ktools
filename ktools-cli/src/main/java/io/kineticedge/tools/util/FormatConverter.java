package io.kineticedge.tools.util;

import io.kineticedge.tools.cmd.kccf.config.Format;
import picocli.CommandLine;

public class FormatConverter implements CommandLine.ITypeConverter<Format> {

  @Override
  public Format convert(String value) {
    try {
      return Format.valueOf(value.toUpperCase().replace("-", "_"));
    } catch (final Exception e) {
      throw new CommandLine.TypeConversionException(String.format("%s is not a valid format.", value));
    }
  }
}
