package io.kineticedge.tools.cmd.truncate;

import io.kineticedge.tools.console.Console;
import io.kineticedge.tools.console.StdConsole;
import io.kineticedge.tools.exception.CommandException;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.DescribeConfigsResult;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.RecordsToDelete;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.config.TopicConfig;

import java.io.Closeable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class TruncateTopic implements Closeable {

  public static final String CLEANUP_POLICY = "cleanup.policy";
  public static final String CLEANUP_POLICY_DELETE = "delete";

  private final Admin admin;
  private final Console console;

  public TruncateTopic(final Map<String, Object> adminConfig, final Console console) {
    admin = Admin.create(adminConfig);
    this.console = console;
  }

  @Override
  public void close() {
    admin.close();
  }

  public void execute(final String topic, final boolean execute, final boolean force)  {

    final List<TopicPartition> topicPartitions = topicPartitions(topic);
    final Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> earliestOffsets = getEarliest(topicPartitions);
    final Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> latestOffsets = getLatest(topicPartitions);

    long count = numberOfMessages(topic, earliestOffsets, latestOffsets);

    if (count == 0) {
      console.out("no messages to delete.");
      return;
    }

    console.out(String.format("%d messages to be deleted over %d partitions", count, topicPartitions.size()));

    final boolean hasDeleteCleanupPolicy  = hasDeleteCleanupPolicy(topic);

    if (!hasDeleteCleanupPolicy && !force) {
      console.err(String.format("topic %s does not have a delete cleanup policy with, use '--force' which will add and then remove the 'delete' cleanup policy.", topic));
      return;
    } else if (hasDeleteCleanupPolicy && force) {
      console.err(String.format("topic %s has a delete cleanup policy, --force is not necessary.", topic));
    }

    if (execute) {
      try {
        if (!hasDeleteCleanupPolicy) {
          addDeleteCleanupPolicy(topic);
        }
        deleteRecords(latestOffsets);
      } finally {
        if (!hasDeleteCleanupPolicy) {
          removeDeleteCleanupPolicy(topic);
        }
      }
    } else {
      console.out("enable --execute to issue command");
    }
  }


  private long numberOfMessages(String topic, Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> earliestOffsets, Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> latestOffsets) {
    long count = 0;
    for (TopicPartition tp : topicPartitions(topic)) {
      long earliestOffset = earliestOffsets.get(tp).offset();
      long latestOffset = latestOffsets.get(tp).offset();
      long difference = latestOffset - earliestOffset;
      count += difference;
    }
    return count;
  }

  private void deleteRecords(Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> latestOffsets) {
    try {
      admin.deleteRecords(generateRecordsToDelete(latestOffsets)).all().get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new CommandException("the deleteRecords operation was interrupted, aborting; it may have still completed.");
    } catch (ExecutionException e) {
      throw new CommandException("deleteRecords exception", e);
    }
  }

  private static Map<TopicPartition, RecordsToDelete> generateRecordsToDelete(Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> latestOffsets) {
    return latestOffsets.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> RecordsToDelete.beforeOffset(entry.getValue().offset())));
  }

  private Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> getLatest(List<TopicPartition> partitions) {
    Map<TopicPartition, OffsetSpec> input = partitions.stream().collect(Collectors.toMap(tp -> tp, tp -> OffsetSpec.latest()));
    return getOffsets(input);
  }

  private Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> getEarliest(List<TopicPartition> partitions) {
    Map<TopicPartition, OffsetSpec> earliest = partitions.stream().collect(Collectors.toMap(tp -> tp, tp -> OffsetSpec.earliest()));
    return getOffsets(earliest);
  }

  private Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> getOffsets(Map<TopicPartition, OffsetSpec> earliest) {
    try {
      return admin.listOffsets(earliest).all().get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new CommandException("listOffsets operation interrupted, deleteRecords will not executed.");
    } catch (ExecutionException e) {
      throw new CommandException("listOffsets exception", e);
    }
  }

  private List<TopicPartition> topicPartitions(final String topic) {
    try {
      return admin.describeTopics(Collections.singleton(topic)).allTopicNames().get().get(topic).partitions().stream().map(info -> new TopicPartition(topic, info.partition())).toList();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new CommandException("describeTopics operation interrupted, deleteRecords will not executed.");
    } catch (ExecutionException e) {
      throw new CommandException("describeTopics exception", e);
    }
  }

  private boolean hasDeleteCleanupPolicy(String topicName)  {
    try {

      final ConfigResource configResource = new ConfigResource(ConfigResource.Type.TOPIC, topicName);
      final DescribeConfigsResult configsResult = admin.describeConfigs(Collections.singleton(configResource));
      final Config config = configsResult.all().get().get(configResource);

      return config.get(TopicConfig.CLEANUP_POLICY_CONFIG).value().contains(CLEANUP_POLICY_DELETE);

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new CommandException("describeTopics operation interrupted, deleteRecords will not executed.");
    } catch (ExecutionException e) {
      throw new CommandException("describeTopics exception", e);
    }

  }

  private void addDeleteCleanupPolicy(String topic) {
    try {
      admin.incrementalAlterConfigs(
              Map.ofEntries(
                      Map.entry(
                              new ConfigResource(ConfigResource.Type.TOPIC, topic),
                              Collections.singleton(new AlterConfigOp(new ConfigEntry(CLEANUP_POLICY, CLEANUP_POLICY_DELETE), AlterConfigOp.OpType.APPEND)
                              )
                      )
              )).all().get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new CommandException("incrementalAlterConfigs operation interrupted, ...");
    } catch (ExecutionException e) {
      throw new CommandException("incrementalAlterConfigs exception", e);
    }
  }

  private void removeDeleteCleanupPolicy(String topic) {
    try {
      admin.incrementalAlterConfigs(
              Map.ofEntries(
                      Map.entry(
                              new ConfigResource(ConfigResource.Type.TOPIC, topic),
                              Collections.singleton(new AlterConfigOp(new ConfigEntry(CLEANUP_POLICY, CLEANUP_POLICY_DELETE), AlterConfigOp.OpType.SUBTRACT)
                              )
                      )
              )).all().get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new CommandException("incrementalAlterConfigs operation interrupted, was not able to remove the 'delete' cleanup.policy.");
    } catch (ExecutionException e) {
      throw new CommandException("incrementalAlterConfigs exception, was not able to remove the 'delete' cleanup.policy", e);
    }
  }

}
