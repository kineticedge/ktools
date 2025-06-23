package io.kineticedge.tools.util;

import org.apache.kafka.common.TopicPartition;

import java.util.Comparator;

public class TopicPartitionComparator implements Comparator<TopicPartition> {

  @Override
  public int compare(TopicPartition tpr1, TopicPartition tpr2) {

    final boolean t1Underscore = tpr1.topic().startsWith("_");
    final boolean t2Underscore = tpr2.topic().startsWith("_");

    if (t1Underscore != t2Underscore) {
      return t1Underscore ? -1 : 1;
    }


    int topicCompare = tpr1.topic().compareTo(tpr2.topic());
    if (topicCompare != 0) {
      return topicCompare;
    }

    return Integer.compare(tpr1.partition(), tpr2.partition());
  }

}