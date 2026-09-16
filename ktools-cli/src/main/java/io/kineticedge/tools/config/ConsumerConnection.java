package io.kineticedge.tools.config;

import picocli.CommandLine;

import java.io.File;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType") // a picocli convention.
public class ConsumerConnection extends AbstractConnection {

  @CommandLine.Option(names = {"--consumer.config", "--consumer-config"}, description = "property file containing configs for Kafka consumer.", order = 2)
  protected Optional<File> commandConfig;

  public Map<String, Object> config() {
    return config(commandConfig);
  }

}
