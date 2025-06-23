package io.kineticedge.tools.domain;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.kineticedge.tools.jackson.BrokerAndDirectorySerializer;

public class BrokerAndDirectory implements Comparable<BrokerAndDirectory> {

  private final int brokerId;
  private final String directory;

  public BrokerAndDirectory(int brokerId, String directory) {
    this.brokerId = brokerId;
    this.directory = directory;
  }

  public int brokerId() {
    return brokerId;
  }

  public String directory() {
    return directory;
  }

  @JsonSerialize(using = BrokerAndDirectorySerializer.class)
  public String toString() {
    return brokerId + ":" + directory;
  }

  @Override
  public int compareTo(BrokerAndDirectory other) {
    int brokerCompare = Integer.compare(this.brokerId, other.brokerId);
    if (brokerCompare != 0) {
      return brokerCompare;
    }
    return this.directory.compareTo(other.directory);
  }
}
