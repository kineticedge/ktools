package io.kineticedge.tools.config;

import picocli.CommandLine;

import java.util.Collections;
import java.util.List;

public class MoveDirectories {

  public static class Start {

    @CommandLine.Option(names = {"--start"})
    private boolean start;

    @CommandLine.Option(names = {"--throttle"}, description = "throttling command", defaultValue = "-1", order = 10)
    private long throttle;
  }

  public static class Finish {
    @CommandLine.Option(names = {"--finish"}, required = false)
    private boolean finish;
  }

  public static class Operations {
    public enum OperationType {
      START, STATUS, FINAL, CANCEL
    }

    private OperationType operationType;

    @CommandLine.Option(names = {"--start"}, description = "Start the move operation for this broker")
    public void setStart(boolean start) {
      if (start) {
        operationType = OperationType.START;
      }
    }

    @CommandLine.Option(names = {"--status"}, description = "Check status of move operations for this broker")
    public void setStatus(boolean status) {
      if (status) {
        operationType = OperationType.STATUS;
      }
    }

    @CommandLine.Option(names = {"--final"}, description = "Execute the final move operation")
    public void setComplete(boolean complete) {
      if (complete) {
        operationType = OperationType.FINAL;
      }
    }

    @CommandLine.Option(names = {"--cancel"}, description = "Execute the final move operation")
    public void setCancel(boolean cancel) {
      if (cancel) {
        operationType = OperationType.CANCEL;
      }
    }

    public OperationType getOperationType() {
      return operationType;
    }

  }

  @CommandLine.ArgGroup(exclusive = false, multiplicity = "0,1")
  private Start start = new Start();

  @CommandLine.ArgGroup(exclusive = false, multiplicity = "0,1")
  private Finish finish = new Finish();


//  @CommandLine.ArgGroup(exclusive = true, multiplicity = "1", order = 2, heading = "Operation to perform:%n")
  private Operations operations = new Operations();

  //@CommandLine.Option(names = {"--broker"}, description = "the broker to have leaders evicted.", required = true, order = 100)
  private int broker;

  //@CommandLine.Option(names = {"--from-directory"}, description = "from directory", required = true, order = 110)
  private String fromDirectory;

//  @CommandLine.Option(names = {"--to-directory"}, description = "to directory", required = true, order = 120)
  private String toDirectory;

  @CommandLine.Option(names = {"--topics"},  description = "only move for the list of topics provided", order = 130)
  private List<String> topics = Collections.emptyList();

 // @CommandLine.Option(names = {"--execute"}, description = "execute operation, only a dry-run w/out this.", defaultValue = "false", order = 140)
  private boolean execute;

//  @CommandLine.Option(names = {"--throttle"}, description = "throttling command", defaultValue = "-1", order = 150)
  private long throttle;

  public Operations operations() {
    return operations;
  }

  public int broker() {
    return broker;
  }

  public String fromDirectory() {
    return fromDirectory;
  }

  public String toDirectory() {
    return toDirectory;
  }

  public List<String> topics() {
    return topics;
  }

  public boolean execute() {
    return execute;
  }

  public long throttle() {
    return throttle;
  }

}
