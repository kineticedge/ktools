package io.kineticedge.tools.util;

import org.apache.kafka.common.TopicPartitionReplica;

import java.util.Comparator;

public class TopicPartitionReplicaComparator implements Comparator<TopicPartitionReplica> {

  @Override
  public int compare(TopicPartitionReplica tpr1, TopicPartitionReplica tpr2) {

    final boolean t1Underscore = tpr1.topic().startsWith("_");
    final boolean t2Underscore = tpr2.topic().startsWith("_");

    if (t1Underscore != t2Underscore) {
      return t1Underscore ? -1 : 1;
    }


    int topicCompare = tpr1.topic().compareTo(tpr2.topic());
    if (topicCompare != 0) {
      return topicCompare;
    }

   int partitionCompare = Integer.compare(tpr1.partition(), tpr2.partition());
    if (partitionCompare != 0) {
      return partitionCompare;
    }

    return Integer.compare(tpr1.brokerId(), tpr2.brokerId());
  }

}