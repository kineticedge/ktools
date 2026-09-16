package io.kineticedge.tools.config;

import io.kineticedge.tools.util.DurationConverter;
import io.kineticedge.tools.util.InstantConverter;
import picocli.CommandLine;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType") // a picocli convention.
public class ConsoleControls {

  @CommandLine.Option(names = {"--max-messages"}, description = "maximum number of messages to read.", order = 90)
  protected Optional<Integer> maxMessages;

  @CommandLine.Option(names = {"--start"}, description = "the time to start consuming from. use 'earliest' for beginning or 'latest' or 'now' for end; latest is the default, format: yyyy-MM-dd(T, )HH:mm:ss.SSSZ.", converter = InstantConverter.class, order = 91)
  protected Optional<Instant> start;

  @CommandLine.Option(names = {"--rewind"}, description = "additional duration to *subtract* from starting time, most useful with 'earliest' and 'latest'.", converter = DurationConverter.class, order = 92)
  protected Optional<Duration> rewind;

  @CommandLine.Option(names = {"--end"}, description = "the time to end consuming, use 'now' to indicate current time, format: yyyy-MM-dd(T, )HH:mm:ss.SSSZ.", converter = InstantConverter.class, order = 93)
  protected Optional<Instant> end;

  @CommandLine.Option(names = {"--forward"}, description = "additional duration to *add* to ending time, most useful with 'now', can be negative.", converter = DurationConverter.class, order = 94)
  protected Optional<Duration> forward;

  public Optional<Integer> maxMessages() {
    return maxMessages;
  }

  public Long start() {
    final long s = start.orElse(Instant.now()).minus(rewind.orElse(Duration.ofSeconds(0L))).toEpochMilli();
    return (s >= 0) ? s : 0L;
  }

  /**
   * Time to stop reading. If a forward duration is supplied, without an end timestamp, then now() is used
   * as the end timestamp. If an end timestamp is provided without a forward duration, then 0s is used
   * for the forwarding amount. However, if both are not provided, then empty is returned; indicated that
   * the consumer should not be limited by time for consumption.
   */
  public Long end() {

    if (end.isEmpty() && forward.isEmpty()) {
      return null;
    }

    final long e = end.orElse(Instant.now()).plus(forward.orElse(Duration.ofSeconds(0L))).toEpochMilli();
    return (e >= 0) ? e : 0L;
  }

}
