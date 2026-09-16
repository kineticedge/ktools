package io.kineticedge.tools;


import io.kineticedge.tools.cmd.kccf.config.KccfConfig;
import io.kineticedge.tools.cmd.kccf.JsonConsole;
import io.kineticedge.tools.consumer.ConsoleConsumer;
import io.kineticedge.tools.consumer.ConsoleConsumerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.util.List;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType") // a picocli convention.
@CommandLine.Command(
        name = "kafka-console-consumer-filter",
        version = "0.0.2",
        usageHelpAutoWidth = true,
        sortOptions = false,
        description = "\nconsume json based message with filtering and highlights\n",
        footer = "\nexample valid timestamp: '2024-01-31Z', '2024-02-01T13Z', '2024-02-02 15', '2024-02-03 02:00:15Z', 'now', 'latest'.\n\nJSON Path filter and highlights on Avro, are based on the JSON representation of Avro.\n\n"
)
public class KafkaConsoleConsumerFilter extends ConsoleCommand {

  private static final Logger log = LoggerFactory.getLogger(KafkaConsoleConsumerFilter.class);

  @CommandLine.ArgGroup(exclusive = false, order = 10)
  private KccfConfig kccfConfig = new KccfConfig();

  public KafkaConsoleConsumerFilter() {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      log.debug("shutting down.");
    }));
  }


  @Override
  public void run() {

    final JsonConsole command = new JsonConsole(kccfConfig);

    final ConsoleConsumerConfig config = new ConsoleConsumerConfig(consumerConnection.config())
            .withConsumerTopic(consumerTopic)
            .withConsoleControls(consoleControls);

    try (ConsoleConsumer consumer = new ConsoleConsumer(config)) {
      consumer.start(command::display);
    }
  }



}
