package io.kineticedge.tools;

import io.kineticedge.tools.config.CommandConnection;
import io.kineticedge.tools.config.ConsoleControls;
import io.kineticedge.tools.config.ConsumerConnection;
import io.kineticedge.tools.config.ConsumerTopic;
import picocli.CommandLine;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType") // a picocli convention.
public abstract class AdminCommand extends Subcommand {

  // these switches should be ordered first

  @CommandLine.ArgGroup(exclusive = false, multiplicity = "1")
  protected CommandConnection commandConnection;

}
