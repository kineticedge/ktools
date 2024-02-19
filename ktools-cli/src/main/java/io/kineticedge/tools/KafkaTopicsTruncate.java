package io.kineticedge.tools;


import io.kineticedge.tools.cmd.truncate.TruncateTopic;
import io.kineticedge.tools.console.StdConsole;
import io.kineticedge.tools.exception.CommandException;
import org.apache.kafka.clients.CommonClientConfigs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType") // a picocli convention.
@CommandLine.Command(name = "kafka-topic-truncate", version = "0.0.1", description = "truncate a topic", usageHelpAutoWidth = true)
public class KafkaTopicsTruncate extends Subcommand {

    private static final Logger log = LoggerFactory.getLogger(KafkaTopicsTruncate.class);

    @CommandLine.Option(names = { "--help" }, usageHelp = true, hidden = true, description = "this help message.")
    private boolean helpRequested = false;

    @CommandLine.Option(names = {"--bootstrap-server"}, description = "The Kafka server to connect to (can be defined in command-config).")
    private Optional<String> bootstrapServer;

    @CommandLine.Option(names = {"--command-config", "--command.config"}, description = "Property file containing configs to be passed to Admin Client.")
    private Optional<File> commandConfig;

    @CommandLine.Option(names = {"--topic"}, description = "the topic to be truncated.", required = true)
    private String topic;

    @CommandLine.Option(names = {"--execute"}, description = "execute operation, only a dry-run w/out this.")
    private boolean execute;

    @CommandLine.Option(names = {"--force"}, description = "if topic only has a delete.policy of 'compacted', use to temporarily add a delete cleanup policy.")
    private boolean force;

    @CommandLine.Option(names = {"--quiet"}, description = "do not give information about topic prior to executing.")
    private boolean quiet;

    public KafkaTopicsTruncate() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.debug("shutting down.");
        }));
    }

    @Override
    public void run() {
        try (TruncateTopic command = new TruncateTopic(config(), new StdConsole())) {
            command.execute(topic, execute, force);
        }
    }


    private Map<String, Object> config() {

        final Map<String, Object> map = new HashMap<>();

        commandConfig.ifPresent(file -> map.putAll(loadProperties(file)));
        bootstrapServer.ifPresent(server -> map.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, server));

        if (!map.containsKey(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG)) {
            throw new CommandException(String.format("%s must be defined with command line option or within configuration file.", CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG));
        }

        return map;
    }
}
