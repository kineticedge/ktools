package io.kineticedge.tools;


import io.kineticedge.tools.cmd.leaders.MoveLeadersOffBroker;
import io.kineticedge.tools.console.StdConsole;
import io.kineticedge.tools.exception.CommandException;
import io.kineticedge.tools.util.PropertiesUtil;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClient;
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
@CommandLine.Command(name = "kafka-move-leaders-off-broker", version = "0.0.3", description = "move leaders off a broker", usageHelpAutoWidth = true)
public class KafkaMoveLeadersOffBroker extends AdminCommand {

  private static final Logger log = LoggerFactory.getLogger(KafkaMoveLeadersOffBroker.class);

  @CommandLine.Option(names = {"--broker"}, description = "the broker to have leaders evicted.", required = true)
  private int broker;

  @CommandLine.Option(names = {"--topics"}, description = "only move for the list of topics provided")
  private List<String> topics = Collections.emptyList();

  @CommandLine.Option(names = {"--restore-json-file"}, description = "use to move back to prior leader election, if possible.")
  private Optional<File> restoreJsonFile;

  @CommandLine.Option(names = {"--execute"}, description = "execute operation, only a dry-run w/out this.")
  private boolean execute;

  public KafkaMoveLeadersOffBroker() {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      log.debug("shutting down.");
    }));
  }

  @Override
  public void run() {
    try (MoveLeadersOffBroker command = new MoveLeadersOffBroker(commandConnection.config(), new StdConsole())) {
      restoreJsonFile
              .ifPresentOrElse(
                      file -> command.reverse(broker, file, execute),
                      () -> command.execute(broker, topics, execute)
              );
    }
  }

}
