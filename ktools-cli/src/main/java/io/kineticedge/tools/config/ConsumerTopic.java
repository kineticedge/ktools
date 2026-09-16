package io.kineticedge.tools.config;

import io.kineticedge.tools.util.IntegerListConverter;
import picocli.CommandLine;

import java.util.Collections;
import java.util.List;

public class ConsumerTopic {

  @CommandLine.Option(names = {"--topic"}, description = "the Kafka topic to be consumed.", required = true, order = 3)
  protected String topic;

  @CommandLine.Option(names = {"--partitions"}, description = "specific partitions to consume, otherwise all topics for the topic are consumed.", arity = "0..*", order = 4, converter = IntegerListConverter.class)
  protected List<Integer> partitions = Collections.emptyList();

  public String topic() {
    return topic;
  }

  public List<Integer> partitions() {
    return partitions;
  }
}
