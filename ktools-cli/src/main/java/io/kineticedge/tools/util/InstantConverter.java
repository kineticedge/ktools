package io.kineticedge.tools.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;

public class InstantConverter implements CommandLine.ITypeConverter<Instant> {

  private static final Logger log = LoggerFactory.getLogger(InstantConverter.class);

  private static final DateTimeFormatter formatterTSeparated = new DateTimeFormatterBuilder()
          .appendPattern("yyyy-MM-dd['T'HH[:mm[:ss][.SSS]]][' 'HH[:mm[:ss][.SSS]]][XXX][' 'XXX][' 'VV]")
          .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
          .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
          .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
          .parseDefaulting(ChronoField.NANO_OF_SECOND, 0)
          .toFormatter();

  @Override
  public Instant convert(String value) {
    if ("latest".equalsIgnoreCase(value) || "now".equalsIgnoreCase(value)) {
      return Instant.now();
    } else if ("earliest".equalsIgnoreCase(value)) {
      return Instant.ofEpochMilli(0L);
    } else {
      final Instant instant = parse(value);
      log.debug("instant conversion input={}, output={}", value, instant);
      return instant;
    }
  }

  private static Instant parse(final String value) {
    final TemporalAccessor ta = formatterTSeparated.parseBest(value, ZonedDateTime::from, LocalDateTime::from);
    if (ta instanceof LocalDateTime localDateTime) {
      // ZoneOffset of UTC is an option, but it is so easy to add Z, and hard to add "local"
      return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
    } else {
      return ZonedDateTime.from(ta).toInstant();
    }
  }

}
