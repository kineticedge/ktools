package io.kineticedge.tools;

import io.kineticedge.tools.console.Console;
import io.kineticedge.tools.console.StdConsole;
import picocli.CommandLine;

import java.util.Map;
import java.util.TreeMap;

import static java.lang.System.out;

@CommandLine.Command(name = "tool-command", description = "", subcommands = {KafkaTopicsTruncate.class})
public class ToolCommand implements Runnable {

  @CommandLine.Option(names = {"--help"}, usageHelp = true, description = "this help message.")
  private boolean helpRequested = false;

  @CommandLine.Spec
  private CommandLine.Model.CommandSpec spec;

  @Override
  public void run() {
    spec.commandLine().usage(out);
  }

  public static void main(String[] args) {

    Console console = new StdConsole();

    new CommandLine(new ToolCommand())
            .setHelpFactory((commandSpec, colorScheme) -> new CommandLine.Help(commandSpec, colorScheme) {
              @Override
              public Map<String, CommandLine.Help> subcommands() {
                return new TreeMap<>(super.subcommands());
              }
            })
            .setExecutionExceptionHandler((e, commandLine, parseResult) -> {
              CommandLine.Help.ColorScheme colorScheme = commandLine.getColorScheme();
              CommandLine.Help.Ansi.Text msg = colorScheme.errorText(e.getMessage());
              console.err(CommandLine.Help.Ansi.AUTO.new Text(msg).toString());
              return commandLine.getCommandSpec().exitCodeOnExecutionException();
            })
            .execute(args);
  }

}
