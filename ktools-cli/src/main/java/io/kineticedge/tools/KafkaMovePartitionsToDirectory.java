package io.kineticedge.tools;


import io.kineticedge.tools.cmd.kccf.config.KccfConfig;
import io.kineticedge.tools.cmd.leaders.MovePartitionsToDirectory;
import io.kineticedge.tools.config.MoveDirectories;
import io.kineticedge.tools.console.StdConsole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.util.Collections;
import java.util.List;


@SuppressWarnings("OptionalUsedAsFieldOrParameterType") // a picocli convention.
@CommandLine.Command(name = "kafka-move-partitions-to-directory", version = "0.0.3", description = "move partitions to first disk on given broker", usageHelpAutoWidth = true)
public class KafkaMovePartitionsToDirectory extends AdminCommand {

  private static final Logger log = LoggerFactory.getLogger(KafkaMovePartitionsToDirectory.class);

  @CommandLine.ArgGroup(exclusive = false, order = 10)
  private MoveDirectories moveDirectories = new MoveDirectories();

  public KafkaMovePartitionsToDirectory() {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      log.debug("shutting down.");
    }));
  }

  @Override
  public void run() {
    try (MovePartitionsToDirectory command = new MovePartitionsToDirectory(commandConnection.config(), new StdConsole())) {
      command.execute(moveDirectories);
    }
  }

}
