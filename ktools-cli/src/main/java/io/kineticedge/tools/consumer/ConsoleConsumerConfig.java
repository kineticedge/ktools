package io.kineticedge.tools.consumer;

import io.kineticedge.tools.config.ConsoleControls;
import io.kineticedge.tools.config.ConsumerTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class ConsoleConsumerConfig {

  private final Map<String, Object> kafkaConfig;

  private List<String> topics;
  private List<Integer> partitions = Collections.emptyList();

  private Duration pollingDuration = Duration.ofMillis(200);

  private Integer maxMessages;
  private Long start;
  private Long end;

  public ConsoleConsumerConfig(Map<String, Object> kafkaConfig) {
    this.kafkaConfig = kafkaConfig;
  }

  public ConsoleConsumerConfig withConsumerTopic(final ConsumerTopic consumerTopic) {
    this.topics = Collections.singletonList(consumerTopic.topic());
    this.partitions = Objects.requireNonNullElse(consumerTopic.partitions(), Collections.emptyList());
    return this;
  }

  public ConsoleConsumerConfig withConsoleControls(final ConsoleControls controlControls) {
    this.start = controlControls.start();
    this.end = controlControls.end();
    controlControls.maxMessages().ifPresent(value -> this.maxMessages = value);
    return this;
  }

  public ConsoleConsumerConfig withPollingDuration(Duration pollingDuration) {
    this.pollingDuration = pollingDuration;
    return this;
  }

  public List<Integer> partitions() {
    return partitions;
  }

  public Long start() {
    return start;
  }

  public Long end() {
    return end;
  }

  public Integer maxMessages() {
    return maxMessages;
  }

  public List<String> topics() {
    return topics;
  }

  public Duration pollingDuration() {
    return pollingDuration;
  }

  public Map<String, Object> config() {

    final Map<String, Object> config = new HashMap<>();

    // default settings
    config.put(org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
    config.put(org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);

    config.putAll(kafkaConfig);

    // non-changeable settings
    config.put(org.apache.kafka.clients.consumer.ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
    config.put(org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG, String.format("cli-%s", UUID.randomUUID()));

    return config;
  }

  public Map<String, Object> adminConfig() {

    final Map<String, Object> config = new HashMap<>(kafkaConfig);

    // cannot remove all consumer configs, as that is too aggressive, but removing the obvious ones to avoid reduce logging noise.
    config.remove(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG);
    config.remove(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG);
    config.remove(ConsumerConfig.GROUP_ID_CONFIG);
    config.remove(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG);
    config.remove(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG);
    config.remove(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG);
    config.remove(ConsumerConfig.FETCH_MIN_BYTES_CONFIG);
    config.remove(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG);
    config.remove(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG);
    config.remove(ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG);

    return config;
  }


}
