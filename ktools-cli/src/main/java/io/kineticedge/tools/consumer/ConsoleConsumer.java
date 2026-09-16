package io.kineticedge.tools.consumer;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndTimestamp;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ConsoleConsumer implements Closeable {

  private static final Duration CONSUMER_TIMEOUT = Duration.ofSeconds(10L);

  private static final Logger log = LoggerFactory.getLogger(ConsoleConsumer.class);

  private final CountDownLatch shutdownLatch = new CountDownLatch(1);
  private boolean shuttingDown = false;

  private final ConsoleConsumerConfig config;
  private final KafkaConsumer<byte[], byte[]> consumer;
  private final List<TopicPartition> partitions;

  private int numMessages = 0;
  private Map<TopicPartition, Boolean> partitionUntil;

  public ConsoleConsumer(final ConsoleConsumerConfig config) {
    this(config, new KafkaConsumer<>(config.config()), partitions(config.config(), config.topics(), config.partitions()));
  }

  // exposed for testing, so KafkaConsumer and AdminClient can be mocked/emulated for testing.
  protected ConsoleConsumer(final ConsoleConsumerConfig config, KafkaConsumer<byte[], byte[]> consumer, List<TopicPartition> partitions) {
    this.config = config;
    this.consumer = consumer;
    this.partitions = partitions;
  }

  public void start(Function<ConsumerRecord<byte[], byte[]>, Boolean> function) {

    try {

      if (config.end() != null) {
        partitionUntil = partitions.stream().collect(Collectors.toMap(tp -> tp, tp -> Boolean.FALSE));
      }

      consumer.assign(partitions);

      seek();

      Map<TopicPartition, Long> timestamps = partitions.stream().collect(Collectors.toMap(item -> item, item -> config.start()));

      Map<TopicPartition, OffsetAndTimestamp> map = consumer.offsetsForTimes(timestamps, CONSUMER_TIMEOUT);
      map.forEach((k, v) -> {
        if (v != null) {
          consumer.seek(k, v.offset());
        } else {
          consumer.seekToEnd(Collections.singleton(k));
        }
      });

    } catch (final Exception e) {
      log.error("unable to get topic metadata, {}.", e.getMessage());
      shuttingDown = true;
      shutdownLatch.countDown();
    }

    try {
      while (!shuttingDown) {
        poll(function);
      }
    } catch (WakeupException e) {
      // wakeup is a way to interrupt the poll sooner so the shutdown will avoid waiting the
      // poll duration, just catch and do nothing with it.
      log.debug("wakeup", e);
    } finally {
      log.debug("closing consumer.");
      shutdownLatch.countDown();
      if (!shuttingDown) {
        log.error("abnormal thread termination, exiting.");
      }
    }
  }

  private void poll(Function<ConsumerRecord<byte[], byte[]>, Boolean> function) {
    for (final ConsumerRecord<byte[], byte[]> rec : consumer.poll(config.pollingDuration())) {
      if (config.end() != null && rec.timestamp() > config.end()) {
        partitionUntil.put(new TopicPartition(rec.topic(), rec.partition()), Boolean.TRUE);
        if (partitionUntil.values().stream().allMatch(Boolean::booleanValue)) {
          // all partitions are past the end-time, so exit.
          shuttingDown = true;
          break;
        }
        // this partition is past end-time, so continue to next record
        continue;
      }

      final boolean processed = function.apply(rec);

      if (processed && messageCount()) {
        break;
      }
    }
  }

  private boolean messageCount() {
    numMessages++;
    if (config.maxMessages() != null && numMessages >= config.maxMessages()) {
      shuttingDown = true;
      return true;
    }
    return false;
  }

  public void close() {
    shuttingDown = true;
    consumer.wakeup();
  }

  public void awaitShutdown() {
    awaitShutdown(30L, TimeUnit.SECONDS);
  }

  public void awaitShutdown(final long timeout, final TimeUnit unit) {
    try {
      shutdownLatch.await(timeout, unit);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }



  /**
   * Use admin client to find the number of partitions for each topic and create a full partition list
   * for all partitions for all topics.
   */
  private static List<TopicPartition> partitions(Map<String, Object> config, Collection<String> topics, List<Integer> selectedPartitions) {
    final List<TopicPartition> partitions = new ArrayList<>();
    try (Admin admin = Admin.create(config)) {
      try {
        admin.describeTopics(topics).allTopicNames().get().forEach((k, v) -> {
          v.partitions().forEach(info -> {
            if (selectedPartitions.isEmpty() || selectedPartitions.contains(info.partition())) {
              partitions.add(new TopicPartition(k, info.partition()));
            }
          });
        });
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new KafkaException(e);
      } catch (final ExecutionException e) {
        throw new KafkaException(e);
      }
      return partitions;
    }
  }

  private void seek() {
    final Map<TopicPartition, Long> map = partitions.stream().collect(Collectors.toMap(item -> item, item -> config.start()));
    consumer.offsetsForTimes(map).forEach((k, v) -> {
      if (v != null) {
        consumer.seek(k, v.offset());
      } else {
        consumer.seekToEnd(Collections.singleton(k));
      }
    });
  }

}

