package io.kineticedge.tools;

import io.kineticedge.tools.config.ConsoleControls;
import io.kineticedge.tools.config.ConsumerConnection;
import io.kineticedge.tools.config.ConsumerTopic;
import picocli.CommandLine;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType") // a picocli convention.
public abstract class ConsoleCommand extends Subcommand {

  // these switches should be ordered first

  @CommandLine.ArgGroup(exclusive = false, order = 1)
  protected ConsumerConnection consumerConnection;

  @CommandLine.ArgGroup(exclusive = false, order = 3)
  protected ConsumerTopic consumerTopic;

  // this switches should be sorted last, so start at order of 90, reserving 10-89 for console tool to list out its order.

  @CommandLine.ArgGroup(exclusive = false, order = 90)
  protected ConsoleControls consoleControls;

}
