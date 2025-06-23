package io.kineticedge.tools.cmd.leaders;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kineticedge.tools.cmd.kccf.jackson.ObjectMapperFactory;
import io.kineticedge.tools.console.Console;
import io.kineticedge.tools.exception.CommandException;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.NewPartitionReassignment;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.admin.TopicListing;
import org.apache.kafka.common.ElectionType;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MoveLeadersOffBroker implements Closeable {

  private static final ObjectMapper objectMapper = ObjectMapperFactory.objectMapper();

  private final Admin admin;
  private final Console console;

  public MoveLeadersOffBroker(final Map<String, Object> adminConfig, final Console console) {
    admin = Admin.create(adminConfig);
    this.console = console;
  }

  @Override
  public void close() {
    admin.close();
  }

  public void execute(int brokerId, List<String> topics, boolean execute) {
    try {
      doExecute(brokerId, topics, execute);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      console.err().println("Interrupted while executing command: " + e.getMessage());
    } catch (ExecutionException e) {
      console.err().println("Error executing command: " + e.getMessage());
    }
  }

  public void reverse(int brokerId, File jsonFile, boolean execute) {
    try {
      doReverse(brokerId, jsonFile, execute);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      console.err().println("Interrupted while executing command: " + e.getMessage());
    } catch (ExecutionException e) {
      console.err().println("Error executing command: " + e.getMessage());
    }
  }

  public void doReverse(int brokerId, File jsonFile, boolean execute) throws InterruptedException, ExecutionException {

    Map<String, Map<Integer, List<Integer>>> restore = readFile(jsonFile);

    Map<TopicPartition, Optional<NewPartitionReassignment>> changes = undoReassignment(brokerId, currentAssignments(), restore);

    toConsole(forDisplay(changes));

    if (execute) {
      admin.alterPartitionReassignments(changes).all().get();
      admin.electLeaders(ElectionType.PREFERRED, changes.keySet()).all().get();
    } else {
      console.println("use --execute to perform this reassignment.");
    }
  }

  public void doExecute(int brokerId, List<String> topics, boolean execute) throws InterruptedException, ExecutionException {

    final Map<TopicPartition, Optional<NewPartitionReassignment>> reassignments = createReassignments(brokerId, topics);

    if (!reassignments.isEmpty()) {

      Map<String, Map<Integer, List<Integer>>> plannedChanges = forDisplay(reassignments);

      toConsole(plannedChanges);

      if (execute) {
        admin.alterPartitionReassignments(reassignments).all().get();
        admin.electLeaders(ElectionType.PREFERRED, reassignments.keySet()).all().get();
      } else {
        console.println("use --execute to perform this reassignment.");
      }
    } else {
      console.println("{}");
    }
  }

  /**
   * Return the given partition assignments for all topics.
   */
  @NotNull
  private Map<String, Map<Integer, List<Integer>>> currentAssignments() throws InterruptedException, ExecutionException {
    return new HashSet<>(admin
            .listTopics()
            .names()
            .get())
            .stream()
            .map(t -> admin.describeTopics(Collections.singleton(t)).allTopicNames())
            .map(f -> {
              try {
                return f.get();
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new CommandException(e);
              } catch (ExecutionException e) {
                throw new CommandException(e);
              }
            })
            .flatMap(m -> m.entrySet().stream())
            .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    e -> e.getValue().partitions().stream()
                            .collect(Collectors.toMap(
                                    TopicPartitionInfo::partition,
                                    p -> p.replicas().stream()
                                            .map(Node::id)
                                            .toList(),
                                    (a, b) -> a,
                                    TreeMap::new
                            ))
            ));
  }

  /**
   * If the broker is the lead replica for any of the given partitions, it is moved to the end of the list.
   */
  @NotNull
  private Map<TopicPartition, Optional<NewPartitionReassignment>> createReassignments(int brokerId, List<String> topics) throws InterruptedException, ExecutionException {
    Map<TopicPartition, Optional<NewPartitionReassignment>> reassignments = new HashMap<>();

    for (TopicListing topic : admin.listTopics().listings().get()) {

      if (!topics.isEmpty() && !topics.contains(topic.name())) {
        continue;
      }

      DescribeTopicsResult topicInfo = admin.describeTopics(Collections.singleton(topic.name()));

      TopicDescription td = topicInfo.topicNameValues().get(topic.name()).get();

      for (TopicPartitionInfo partition : td.partitions()) {
        if (partition.leader().id() == brokerId) {
          List<Integer> currentReplicas = partition.replicas().stream().map(Node::id).toList();

          List<Integer> newReplicaOrder = Stream.concat(currentReplicas.stream().filter(id -> id != brokerId), Stream.of(brokerId)).toList();

          TopicPartition tp = new TopicPartition(topic.name(), partition.partition());
          reassignments.put(tp, Optional.of(new NewPartitionReassignment(newReplicaOrder)));
        }
      }
    }
    return reassignments;
  }

  private Map<String, Map<Integer, List<Integer>>> readFile(File restoreJsonFile) {
    try {
      return objectMapper.readValue(restoreJsonFile, new TypeReference<Map<String, Map<Integer, List<Integer>>>>() {
      });
    } catch (Exception e) {
      throw new CommandException(e);
    }
  }


  private Map<TopicPartition, Optional<NewPartitionReassignment>> undoReassignment(int brokerId,
                                                                                   Map<String, Map<Integer, List<Integer>>> current,
                                                                                   Map<String, Map<Integer, List<Integer>>> restore) {
    Map<TopicPartition, Optional<NewPartitionReassignment>> changes = new HashMap<>();

    // For each topic in the restore map
    restore.forEach((topic, partitions) -> {
      Map<Integer, List<Integer>> currentPartitions = current.get(topic);
      if (currentPartitions != null) {
        // For each partition in the restore map
        partitions.forEach((partition, originalReplicas) -> {
          if (originalReplicas.getLast() == brokerId) {
            List<Integer> currentReplicas = new ArrayList<>(currentPartitions.get(partition));
            // Remove broker and add it to front to make it leader
            currentReplicas.remove(Integer.valueOf(brokerId));
            currentReplicas.addFirst(brokerId);

            // Create TopicPartition and NewPartitionReassignment
            TopicPartition tp = new TopicPartition(topic, partition);
            changes.put(tp, Optional.of(new NewPartitionReassignment(currentReplicas)));
          }
        });
      }
    });

    return changes;
  }

  private Map<String, Map<Integer, List<Integer>>> forDisplay(Map<TopicPartition, Optional<NewPartitionReassignment>> reassignments) {
    return reassignments.entrySet().stream()
            .collect(Collectors.groupingBy(
                    e -> e.getKey().topic(),
                    Collectors.toMap(
                            e -> e.getKey().partition(),
                            e -> e.getValue().map(NewPartitionReassignment::targetReplicas).orElse(List.of()),
                            (a, b) -> a,
                            TreeMap::new
                    )
            ));
  }

  private void toConsole(Map<?, ?> map) {
    try {
      objectMapper.writerWithDefaultPrettyPrinter().writeValue(console.out(), map);
      console.println("");
    } catch (IOException e) {
      throw new CommandException(e);
    }
  }

}