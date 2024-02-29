package io.kineticedge.tools.util;

import com.jayway.jsonpath.InvalidPathException;
import com.jayway.jsonpath.JsonPath;
import picocli.CommandLine;

import java.time.Instant;

public class JsonPathConverter implements CommandLine.ITypeConverter<JsonPath> {

  @Override
  public JsonPath convert(String value) {
    try {
      return JsonPath.compile(value);
    } catch (final Exception e) {
      throw new CommandLine.TypeConversionException(String.format("%s is not a valid JSONPath.", value));
    }
  }
}
