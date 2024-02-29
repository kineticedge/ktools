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
public class AbstractConnection {

  @CommandLine.Option(names = {"--bootstrap-server"}, description = "The Kafka server to connect to (can be defined in config).", order = 1)
  protected Optional<String> bootstrapServer;

  protected Map<String, Object> config(final Optional<File> config) {

    final Map<String, Object> map = new HashMap<>();

    config.ifPresent(file -> map.putAll(PropertiesUtil.loadProperties(file)));
    bootstrapServer.ifPresent(server -> map.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, server));

    if (!map.containsKey(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG)) {
      throw new CommandException(String.format("%s must be defined with command line option or within configuration file.", CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG));
    }

    return map;
  }


}
