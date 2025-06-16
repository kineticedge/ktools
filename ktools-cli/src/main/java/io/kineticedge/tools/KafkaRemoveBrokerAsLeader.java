package io.kineticedge.tools;


import io.kineticedge.tools.cmd.leaders.MoveLeadersRunner;
import io.kineticedge.tools.console.StdConsole;
import io.kineticedge.tools.exception.CommandException;
import io.kineticedge.tools.util.PropertiesUtil;
import org.apache.kafka.clients.CommonClientConfigs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType") // a picocli convention.
@CommandLine.Command(name = "kafka-remove-broker-as-leader", version = "0.0.3", description = "move leaders off a broker", usageHelpAutoWidth = true)
public class KafkaRemoveBrokerAsLeader extends Subcommand {

  private static final Logger log = LoggerFactory.getLogger(KafkaRemoveBrokerAsLeader.class);

  @CommandLine.Option(names = {"--bootstrap-server"}, description = "The Kafka server to connect to (can be defined in command-config).")
  private Optional<String> bootstrapServer;

  @CommandLine.Option(names = {"--command-config", "--command.config"}, description = "Property file containing configs to be passed to Admin Client.")
  private Optional<File> commandConfig;

  @CommandLine.Option(names = {"--topics"}, description = "the topic to be truncated.")
  private List<String> topics = Collections.emptyList();

  @CommandLine.Option(names = {"--broker"}, description = "the broker to have leaders evicted.", required = true)
  private int broker;

  @CommandLine.Option(names = {"--restore-json-file"}, description = "use to move back to prior leader election, if possible.")
  private Optional<File> restoreJsonFile;

  @CommandLine.Option(names = {"--execute"}, description = "execute operation, only a dry-run w/out this.")
  private boolean execute;


  public KafkaRemoveBrokerAsLeader() {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      log.debug("shutting down.");
    }));
  }

  @Override
  public void run() {
    try (MoveLeadersRunner command = new MoveLeadersRunner(config(), new StdConsole())) {
      restoreJsonFile
              .ifPresentOrElse(
                      file -> command.reverse(broker, file, execute),
                      () -> command.execute(broker, topics, execute)
              );
    }
  }


  private Map<String, Object> config() {

    final Map<String, Object> map = new HashMap<>();

    commandConfig.ifPresent(file -> map.putAll(PropertiesUtil.loadProperties(file)));
    bootstrapServer.ifPresent(server -> map.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, server));

    if (!map.containsKey(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG)) {
      throw new CommandException(String.format("%s must be defined with command line option or within configuration file.", CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG));
    }

    return map;
  }
}
