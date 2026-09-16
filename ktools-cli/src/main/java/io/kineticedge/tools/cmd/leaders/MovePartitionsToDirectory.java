package io.kineticedge.tools.cmd.leaders;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.kineticedge.tools.cmd.kccf.jackson.ObjectMapperFactory;
import io.kineticedge.tools.config.MoveDirectories;
import io.kineticedge.tools.console.Console;
import io.kineticedge.tools.domain.BrokerAndDirectory;
import io.kineticedge.tools.exception.CommandException;
import io.kineticedge.tools.util.TopicPartitionComparator;
import io.kineticedge.tools.util.TopicPartitionReplicaComparator;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.DescribeReplicaLogDirsResult;
import org.apache.kafka.clients.admin.LogDirDescription;
import org.apache.kafka.clients.admin.ReplicaInfo;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionReplica;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.utils.Utils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class MovePartitionsToDirectory implements Closeable {

  private static final Logger log = org.slf4j.LoggerFactory.getLogger(MovePartitionsToDirectory.class);

  static final String BROKER_LEVEL_LOG_DIR_THROTTLE = "replica.alter.log.dirs.io.max.bytes.per.second";

  private static final ObjectMapper objectMapper = ObjectMapperFactory.objectMapper();

  private final Admin admin;
  private final Console console;

  public MovePartitionsToDirectory(final Map<String, Object> adminConfig, final Console console) {
    admin = Admin.create(adminConfig);
    this.console = console;
  }

  @Override
  public void close() {
    admin.close();
  }

  public void execute(MoveDirectories moveDirectories) {


    try {

      if (moveDirectories.operations().getOperationType() == MoveDirectories.Operations.OperationType.FINAL) {
        doFinish(moveDirectories.broker());
        return;
      } else if (moveDirectories.operations().getOperationType() == MoveDirectories.Operations.OperationType.CANCEL) {
        console.println("Cancelling all partition moves for broker " + moveDirectories.broker());
        doCancel(moveDirectories.broker(), moveDirectories.fromDirectory(), moveDirectories.execute());
        return;
      }

      doExecute(moveDirectories);

      Map<BrokerAndDirectory, List<TopicPartition>> status = doStatus(moveDirectories.broker());
      console.println("partitions being moved");
      toConsole(status);

      //doCancel(moveDirectories.broker());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      console.err().println("Interrupted while executing command: " + e.getMessage());
    } catch (ExecutionException e) {
      console.err().println("Error executing command: " + e.getMessage());
    }
  }

  public void doCancel(int brokerId, String fromDirectory, boolean execute) throws InterruptedException, ExecutionException {


    Map<TopicPartitionReplica, String> replicas = findMovingReplicasOnBroker(brokerId);

    Map<TopicPartitionReplica, String> filtered = filterByDirectory(replicas, fromDirectory);

    if (execute) {
      toConsole(filtered);
      //modifyLogDirThrottle(brokerId, moveDirectories.throttle());
      admin.alterReplicaLogDirs(filtered).all().get();
    } else {
      console.println("use --execute to perform this reassignment.");
    }

//    Map<Integer, Map<String, LogDirDescription>> logDirInfo = admin
//            .describeLogDirs(List.of(brokerId))
//            .allDescriptions()
//            .get();
//
//    Map<TopicPartitionReplica, String> replicasToCancel = new HashMap<>();
//
//    for (Map.Entry<Integer, Map<String, LogDirDescription>> brokerEntry : logDirInfo.entrySet()) {
//      int bid = brokerEntry.getKey();
//      for (Map.Entry<String, LogDirDescription> dirEntry : brokerEntry.getValue().entrySet()) {
//        String currentLogDir = dirEntry.getKey();
//        System.out.println("Broker " + bid + " current log dir: " + currentLogDir);
//        dirEntry.getValue().replicaInfos().forEach((topicPartition, replicaInfo) -> {
//          System.out.println("Broker " + bid + " replica: " + topicPartition + " in log dir: " + currentLogDir);
//          if (replicaInfo.isFuture()) {
//            System.out.println("!!!!!!!!!!!!!!!!!!!");
//            TopicPartitionReplica tpr = new TopicPartitionReplica(topicPartition.topic(), topicPartition.partition(), bid);
//            // Use the current log directory instead of null
//            replicasToCancel.put(tpr, currentLogDir);
//          }
//        });
//      }
//    }
//
//    if (!replicasToCancel.isEmpty()) {
//      admin.alterReplicaLogDirs(replicasToCancel).all().get();
//      System.out.printf("Cancelled moves for %d replicas: %s%n",
//              replicasToCancel.size(),
//              replicasToCancel.keySet());
//    } else {
//      System.out.println("No active partition moves found to cancel.");
//    }

  }

  public Map<BrokerAndDirectory, List<TopicPartition>> doStatus(int brokerId) throws InterruptedException, ExecutionException {

    final List<TopicPartitionReplica> replicasToCheck = getTopicPartitionReplicas(brokerId);

    return admin.describeReplicaLogDirs(replicasToCheck).all().get().entrySet().stream()
            .filter(entry -> entry.getValue().getFutureReplicaLogDir() != null)
            .collect(Collectors.groupingBy(
                    entry -> new BrokerAndDirectory(
                            entry.getKey().brokerId(),
                            entry.getValue().getFutureReplicaLogDir()
                    ),
                    TreeMap::new,
                    Collectors.mapping(
                            entry -> new TopicPartition(entry.getKey().topic(), entry.getKey().partition()),
                            Collectors.collectingAndThen(
                                    Collectors.toList(),
                                    list -> {
                                      list.sort(new TopicPartitionComparator());
                                      return list;
                                    }
                            )
                    )
            ));
  }

  public void doFinish(int brokerId) throws InterruptedException, ExecutionException {
    Map<BrokerAndDirectory, List<TopicPartition>> status = doStatus(brokerId);
    if (status.isEmpty()) {
      console.println("no moves in flight, clearing throttle.");
      clearBrokerLevelThrottles(brokerId);
    } else {
      console.println("partitions still being moved");
      toConsole(status);
    }
  }

  public void doExecute(MoveDirectories moveDirectories) throws InterruptedException, ExecutionException {

    int brokerId = moveDirectories.broker();
    String fromDirectory = moveDirectories.fromDirectory();
    String toDirectory = moveDirectories.toDirectory();

    Set<String> logDirs = admin.describeLogDirs(List.of(brokerId)).descriptions().get(brokerId).get().keySet();

    if (!logDirs.contains(fromDirectory)) {
      throw new CommandException("Broker " + brokerId + " does not have log directory " + fromDirectory);
    }

    if (!logDirs.contains(toDirectory)) {
      throw new CommandException("Broker " + brokerId + " does not have log directory " + toDirectory);
    }

    if (fromDirectory.equals(toDirectory)) {
      throw new CommandException("to and from directories cannot be the same.");
    }

    // all TopicPartitionReplicas and their direct directory
    Map<TopicPartitionReplica, String> current = findReplicasOnBroker(brokerId);

    // filtered to only those on the given directory
    Map<TopicPartitionReplica, String> filtered = filterByDirectory(current, fromDirectory);

    Map<TopicPartitionReplica, String> proposal = convert(filtered, toDirectory);

//    console.printf("topics to move from %s to %s%n%n", fromDirectory, toDirectory);
//    proposal.keySet().stream()
//            .sorted(new TopicPartitionReplicaComparator())
//            .map(tpr -> "\t" + tpr.topic() + "-" + tpr.partition())
//            .forEach(console::println);
//    console.printf("\n");


//    if (filtered.isEmpty()) {
//      return;
//    }

    if (moveDirectories.execute()) {

      modifyLogDirThrottle(brokerId, moveDirectories.throttle());

      admin.alterReplicaLogDirs(proposal).all().get();
      //Map<String, LogDirDescription> result = admin.describeLogDirs(List.of(brokerId)).descriptions().get(brokerId).get();
      //toConsole(result);

      // checkLogMoveStatus(brokerId);

    } else {
      console.println("use --execute to perform this reassignment.");
    }
  }

  private static Map<TopicPartitionReplica, String> filterByDirectory(Map<TopicPartitionReplica, String> current, String fromDirectory) {
    return current.entrySet()
            .stream()
            .filter(entry -> entry.getValue().equals(fromDirectory))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private static Map<TopicPartitionReplica, String> convert(Map<TopicPartitionReplica, String> current, String toDirectory) {
    return current.entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> toDirectory));
  }


  /**
   * for a given broker-id, give all TopicPartition replicas where 'futureReplicaLogDir' is null.
   */
  private Map<TopicPartitionReplica, String> findReplicasOnBroker(int brokerId) throws ExecutionException, InterruptedException {

    final List<TopicPartitionReplica> replicasToCheck = getTopicPartitionReplicas(brokerId);

    return admin.describeReplicaLogDirs(replicasToCheck).all().get().entrySet().stream()
            .filter(entry -> {
//              console.errPrintf("%s - current=%s,%s future=%s,%s%n",
//                      entry.getKey(),
//                      entry.getValue().getCurrentReplicaLogDir(),
//                      entry.getValue().getCurrentReplicaOffsetLag(),
//                      entry.getValue().getFutureReplicaLogDir(),
//                      entry.getValue().getFutureReplicaOffsetLag()
//              );
              return entry.getValue().getFutureReplicaLogDir() == null;
            })
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getCurrentReplicaLogDir()));
  }

  /**
   * for a given broker-id, give all TopicPartition replicas where 'futureReplicaLogDir' is null.
   */
  private Map<TopicPartitionReplica, String> findMovingReplicasOnBroker(int brokerId) throws ExecutionException, InterruptedException {

    final List<TopicPartitionReplica> replicasToCheck = getTopicPartitionReplicas(brokerId);

    return admin.describeReplicaLogDirs(replicasToCheck).all().get().entrySet().stream()
            .filter(entry -> entry.getValue().getFutureReplicaLogDir() != null)
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getCurrentReplicaLogDir()));
  }

  @NotNull
  private List<TopicPartitionReplica> getTopicPartitionReplicas(int brokerId) throws InterruptedException, ExecutionException {
    final Set<String> topics = admin.listTopics().names().get();
    final Map<String, TopicDescription> topicDescriptions = admin.describeTopics(topics).allTopicNames().get();
    return topicDescriptions.values().stream()
            .flatMap(topicDescription -> topicDescription.partitions().stream()
                    .filter(partitionInfo -> partitionInfo.replicas().stream().anyMatch(node -> node.id() == brokerId))
                    .map(partitionInfo -> new TopicPartitionReplica(topicDescription.name(), partitionInfo.partition(), brokerId)))
            .toList();
  }


  private void toConsole(Map<?, ?> map) {
    try {
      objectMapper.writerWithDefaultPrettyPrinter().writeValue(console.out(), map);
      console.println("");
    } catch (IOException e) {
      throw new CommandException(e);
    }
  }

  private void toConsole(Collection<?> collection) {
    try {
      objectMapper.writerWithDefaultPrettyPrinter().writeValue(console.out(), collection);
      console.println("");
    } catch (IOException e) {
      throw new CommandException(e);
    }
  }


//  static Map<TopicPartitionReplica, LogDirMoveState> findLogDirMoveStates(Admin adminClient,
//                                                                          Map<TopicPartitionReplica, String> targetMoves
//  ) throws ExecutionException, InterruptedException {
//    Map<TopicPartitionReplica, DescribeReplicaLogDirsResult.ReplicaLogDirInfo> replicaLogDirInfos = adminClient
//            .describeReplicaLogDirs(targetMoves.keySet()).all().get();
//
//    return targetMoves.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> {
//      TopicPartitionReplica replica = e.getKey();
//      String targetLogDir = e.getValue();
//
//      if (!replicaLogDirInfos.containsKey(replica))
//        return new MissingReplicaMoveState(targetLogDir);
//
//      DescribeReplicaLogDirsResult.ReplicaLogDirInfo info = replicaLogDirInfos.get(replica);
//
//      if (info.getCurrentReplicaLogDir() == null)
//        return new MissingLogDirMoveState(targetLogDir);
//
//      if (info.getFutureReplicaLogDir() == null) {
//        if (info.getCurrentReplicaLogDir().equals(targetLogDir))
//          return new CompletedMoveState(targetLogDir);
//
//        return new CancelledMoveState(info.getCurrentReplicaLogDir(), targetLogDir);
//      }
//
//      return new ActiveMoveState(info.getCurrentReplicaLogDir(), targetLogDir, info.getFutureReplicaLogDir());
//    }));
//  }

  public void checkLogMoveStatus(int brokerId) throws ExecutionException, InterruptedException {

    final Map<Integer, Map<String, LogDirDescription>> logDirInfo = admin
            .describeLogDirs(List.of(brokerId))
            .allDescriptions()
            .get();

    for (Map.Entry<Integer, Map<String, LogDirDescription>> brokerEntry : logDirInfo.entrySet()) {

      final int id = brokerEntry.getKey();

      for (Map.Entry<String, LogDirDescription> dirEntry : brokerEntry.getValue().entrySet()) {
        String logDir = dirEntry.getKey();
        LogDirDescription description = dirEntry.getValue();

        // Check replicas for any that are marked as "future"
        for (Map.Entry<TopicPartition, ReplicaInfo> replicaEntry : description.replicaInfos().entrySet()) {
          if (replicaEntry.getValue().isFuture()) {
            console.printf("Broker %d: Moving partition %s in directory %s%n",
                    id,
                    replicaEntry.getKey(),
                    logDir);
          }
        }
      }
    }
  }


  private void cancelAllLogDirectoryMoves(int brokerId) throws Exception {

    Map<Integer, Map<String, LogDirDescription>> logDirInfo = admin
            .describeLogDirs(List.of(brokerId))
            .allDescriptions()
            .get();

    // Collect all TopicPartitionReplicas that are being moved (have future replicas)
    Map<TopicPartitionReplica, String> replicasToCancel = new HashMap<>();

    for (Map.Entry<Integer, Map<String, LogDirDescription>> brokerEntry : logDirInfo.entrySet()) {
      for (Map.Entry<String, LogDirDescription> dirEntry : brokerEntry.getValue().entrySet()) {
        dirEntry.getValue().replicaInfos().forEach((topicPartition, replicaInfo) -> {
          if (replicaInfo.isFuture()) {
            TopicPartitionReplica tpr = new TopicPartitionReplica(
                    topicPartition.topic(),
                    topicPartition.partition(),
                    brokerEntry.getKey()
            );
            // Pass null as the target directory to cancel the move
            replicasToCancel.put(tpr, null);
          }
        });
      }
    }

    if (!replicasToCancel.isEmpty()) {
      // Cancel the moves by setting target directory to null
      admin.alterReplicaLogDirs(replicasToCancel)
              .all()
              .get();  // wait for completion

      System.out.printf("Cancelled moves for %d replicas: %s%n",
              replicasToCancel.size(),
              replicasToCancel.keySet());
    } else {
      System.out.println("No active partition moves found to cancel.");
    }
  }


  private void modifyLogDirThrottle(int brokerId, long logDirThrottle) throws ExecutionException, InterruptedException {
    if (logDirThrottle >= 0) {
      Map<ConfigResource, Collection<AlterConfigOp>> configs = new HashMap<>();

      List<AlterConfigOp> ops = new ArrayList<>();
      ops.add(new AlterConfigOp(new ConfigEntry(BROKER_LEVEL_LOG_DIR_THROTTLE, Long.toString(logDirThrottle)), AlterConfigOp.OpType.SET));
      configs.put(new ConfigResource(ConfigResource.Type.BROKER, Long.toString(brokerId)), ops);

      admin.incrementalAlterConfigs(configs).all().get();

      console.println("The replica-alter-dir throttle limit was set to " + logDirThrottle + " B/s");
    }
  }

  private void clearBrokerLevelThrottles(int brokerId) throws ExecutionException, InterruptedException {
    Map<ConfigResource, Collection<AlterConfigOp>> configOps = new HashMap<>();
    configOps.put(new ConfigResource(ConfigResource.Type.BROKER, Integer.toString(brokerId)), List.of(new AlterConfigOp(new ConfigEntry(BROKER_LEVEL_LOG_DIR_THROTTLE, null), AlterConfigOp.OpType.DELETE)));
    admin.incrementalAlterConfigs(configOps).all().get();
  }

}