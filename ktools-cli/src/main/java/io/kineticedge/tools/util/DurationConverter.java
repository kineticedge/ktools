package io.kineticedge.tools.util;

import picocli.CommandLine;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DurationConverter implements CommandLine.ITypeConverter<Duration> {

    private static final Pattern SIMPLE = Pattern.compile("^([+-]?\\d+)([a-zA-Z]{0,2})$");

    private static final Map<String, ChronoUnit> CONVERSIONS = Map.ofEntries(
        Map.entry("ms", ChronoUnit.MILLIS),
        Map.entry("", ChronoUnit.MILLIS),
        Map.entry("s", ChronoUnit.SECONDS),
        Map.entry("m", ChronoUnit.MINUTES),
        Map.entry("h", ChronoUnit.HOURS),
        Map.entry("d", ChronoUnit.DAYS)
    );

    @Override
    public Duration convert(String s)  {
        final Matcher matcher = SIMPLE.matcher(s);
        if (matcher.matches()) {
            return Duration.of(Long.parseLong(matcher.group(1)), CONVERSIONS.get(matcher.group(2).toLowerCase()));
        }
        // if parsing doesn't work, set to 0.
        return Duration.of(0, ChronoUnit.MILLIS);
    }
}
