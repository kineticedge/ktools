package io.kineticedge.tools.util;

import picocli.CommandLine;

import java.util.Arrays;
import java.util.List;

public class IntegerListConverter implements CommandLine.ITypeConverter<List<Integer>> {

  @Override
  public List<Integer> convert(String value) throws Exception {
    return Arrays.stream(value.split(","))
            .map(Integer::parseInt)
            .toList();
  }

}