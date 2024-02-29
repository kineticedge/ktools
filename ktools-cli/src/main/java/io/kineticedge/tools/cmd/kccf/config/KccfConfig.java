package io.kineticedge.tools.cmd.kccf.config;

import com.jayway.jsonpath.JsonPath;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.kineticedge.tools.console.Color;
import io.kineticedge.tools.console.Console;
import io.kineticedge.tools.console.StdConsole;
import io.kineticedge.tools.util.ColorConverter;
import io.kineticedge.tools.util.FormatConverter;
import io.kineticedge.tools.util.HighlightConverter;
import io.kineticedge.tools.util.JsonPathConverter;
import io.kineticedge.tools.util.PropertiesUtil;
import org.apache.commons.lang3.tuple.Pair;
import picocli.CommandLine;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType") // a picocli convention.
public class KccfConfig {

  @CommandLine.Option(names = {"--format"}, defaultValue = "json", description = "the format of the messages, currently supported: 'json', Confluent's SR based 'json-schema', and Confluent's SR based 'avro'.", converter = FormatConverter.class, order = 41)
  private Format format;

  @CommandLine.Option(names = {"--filter"}, description = "filter records to only those that match this JSON Path.", converter = JsonPathConverter.class, order = 42)
  private JsonPath filter = null;

  @CommandLine.Option(names = {"--highlights", "--highlight"}, arity = "0..*", description = "JSON Path to highlight COLOR=Path (COLOR= optional).", converter = HighlightConverter.class, order = 43)
  private List<Pair<Color, JsonPath>> highlights = Collections.emptyList();

  @CommandLine.Option(names = {"--default-color"}, description = "if highlight is not supplied with a color, this default highlight color is used. The default default-color is RED.", defaultValue = "RED", converter = ColorConverter.class, order = 44)
  private Color defaultColor;

  @CommandLine.Option(names = {"--schema-registry"}, description = "required for 'avro' formatted messages (can be defined in the schema-registry config).", order = 45)
  private Optional<String> schemaRegistryUrl;

  @CommandLine.Option(names = {"--schema-registry.config", "--schema-registry-config"}, description = "property file containing configs for Kafka consumer.", order = 46)
  protected Optional<File> schemaRegistryConfig;

  @CommandLine.Option(names = {"--include-key"}, defaultValue = "false", description = "include a string representation in a 'key' element, value is nested under 'value'.", order = 48)
  private boolean includeKey;

  @CommandLine.Option(names = {"--include-metadata"}, defaultValue = "false", description = "include the topic metadata in a 'metadata' element, value is nested under 'value'.", order = 49)
  private boolean includeMetadata;

  private Console console = new StdConsole();

  public Console console() {
    return console;
  }

  public Format format() {
    return format;
  }

  public JsonPath filter() {
    return filter;
  }

  public List<Pair<Color, JsonPath>> highlights() {
    return applyDefault(highlights, defaultColor);
  }

  public boolean includeKey() {
    return includeKey;
  }

  public boolean includeMetadata() {
    return includeMetadata;
  }

  public Map<String, Object> srConfig() {

    final Map<String, Object> map = new HashMap<>();

    if (!format.usesSchemaRegistry()) {
      return map;
    }

    map.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, "false");

    // ideally schema-registry configured separately for easier understanding of where
    // properties are pulled in from, but not required. Will use consumer.config, if sr-config
    // is not defined.
//    schemaRegistryConfig.ifPresentOrElse(
//            file -> map.putAll(loadProperties(file)),
//            () -> commandConfig.ifPresent(file -> map.putAll(loadProperties(file)))
//    );

    schemaRegistryConfig.ifPresent(
            file -> map.putAll(PropertiesUtil.loadProperties(file))
    );

    schemaRegistryUrl.ifPresent(server -> map.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, server));

    if (!map.containsKey(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG)) {
      map.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, "http://localhost:8081");
    }

    return map;
  }


  private static List<Pair<Color, JsonPath>> applyDefault(final List<Pair<Color, JsonPath>> highlights, final Color defaultColor) {
    return highlights.stream().map(pair -> (pair.getLeft() == null) ? Pair.of(defaultColor, pair.getRight()) : pair).toList();
  }

}
