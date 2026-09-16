package io.kineticedge.tools.config;

import io.kineticedge.tools.exception.CommandException;
import io.kineticedge.tools.util.PropertiesUtil;
import org.apache.kafka.clients.CommonClientConfigs;
import picocli.CommandLine;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType") // a picocli convention.
public class CommandConnection extends AbstractConnection {

  @CommandLine.Option(names = {"--command-config", "--command.config"}, description = "property file containing configs for Kafka Admin client.", order = 2)
  protected Optional<File> commandConfig;

  public Map<String, Object> config() {
    return config(commandConfig);
  }

}
