package io.kineticedge.tools.util;

import com.jayway.jsonpath.JsonPath;
import io.kineticedge.tools.console.Color;
import org.apache.commons.lang3.tuple.Pair;
import picocli.CommandLine;

public class HighlightConverter implements CommandLine.ITypeConverter<Pair<Color, JsonPath>> {

  @Override
  public Pair<Color, JsonPath>convert(String value) {

    if (value.startsWith("$")) {
      return Pair.of(null, compile(value));
    }

    //System.out.println("H : " + value + " : " + System.identityHashCode(value));
    final String[] split = value.split("=", 2);
    if (split.length == 1) {
      return Pair.of(null, compile(value));
    } else {
      return Pair.of(Color.valueOf(split[0].toUpperCase()), compile(split[1]));
    }
  }


  private static JsonPath compile(final String value) {
    try {
      return JsonPath.compile(value);
    } catch (final Exception e) {
      throw new CommandLine.TypeConversionException(String.format("%s is not a valid JSONPath.", value));
    }
  }

}
