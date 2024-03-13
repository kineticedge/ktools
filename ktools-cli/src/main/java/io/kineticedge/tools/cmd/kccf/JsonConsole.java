package io.kineticedge.tools.cmd.kccf;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import io.kineticedge.tools.cmd.kccf.config.Format;
import io.kineticedge.tools.cmd.kccf.config.KccfConfig;
import io.kineticedge.tools.cmd.kccf.deserializer.AvroDeserializer;
import io.kineticedge.tools.cmd.kccf.deserializer.JsonDeserializer;
import io.kineticedge.tools.cmd.kccf.deserializer.JsonSchemaDeserializer;
import io.kineticedge.tools.cmd.kccf.jackson.FilterGenerator;
import io.kineticedge.tools.cmd.kccf.jackson.PrettyPrinter;
import io.kineticedge.tools.cmd.kccf.util.HighlightService;
import io.kineticedge.tools.console.Color;
import io.kineticedge.tools.console.Console;
import io.kineticedge.tools.exception.CommandException;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class JsonConsole {

  private static final Logger log = LoggerFactory.getLogger(JsonConsole.class);
  private static final ObjectMapper objectMapper = new ObjectMapper();
  private static final Configuration configuration = Configuration.builder()
          .jsonProvider(new JacksonJsonNodeJsonProvider())
          .options(Option.ALWAYS_RETURN_LIST).build();

  private final Console console;
  private final JsonPath filter;
  private final List<Pair<Color, JsonPath>> highlights;

  private final Deserializer<JsonNode> deserializer;
  private final HighlightService highlightService;
  private final JsonGenerator jsonGenerator;

  private final boolean includeKey;
  private final boolean includeMetadata;

//  public JsonConsole(Console console, final Format format, final JsonPath filter, final List<Pair<Color,JsonPath>> highlights, Map<String, Object> schemaRegistryConfig) {

  public JsonConsole(KccfConfig config) {
    this.console = config.console();
    this.filter = config.filter();
    this.highlights = config.highlights();
    this.highlightService = new HighlightService();

    this.includeKey = config.includeKey();
    this.includeMetadata = config.includeMetadata();

    // create the deserializer used to read the topic.
    if (config.format() == Format.JSON) {
      deserializer = new JsonDeserializer();
    } else if (config.format() == Format.JSON_SCHEMA) {
      deserializer = new JsonSchemaDeserializer();
      deserializer.configure(config.srConfig(), false);
    } else if (config.format() == Format.AVRO) {
      deserializer = new AvroDeserializer();
      deserializer.configure(config.srConfig(), false);
    } else {
      throw new CommandException(String.format("no deserializer defined for format=%s", config.format()));
    }

    // create the JsonGenerator that is used for rendering the JSON with highlights.
    try {
      JsonFactory jsonFactory = new JsonFactory(objectMapper); //seems to work even w/out setting codec.
      jsonGenerator = new FilterGenerator(jsonFactory.createGenerator(console.out()), highlightService);
      //gen.setCodec(objectMapper); // use to be need, not anymore don't know why.
      jsonGenerator.setPrettyPrinter(new PrettyPrinter(highlightService));
    } catch (final IOException e) {
      throw new CommandException(e);
    }

  }

  private JsonNode convert(final ConsumerRecord<byte[], byte[]> consumerRecord) {
    JsonNode node = deserializer.deserialize(consumerRecord.topic(), consumerRecord.value());

    if (includeKey || includeMetadata) {

      final ObjectNode wrapper = JsonNodeFactory.instance.objectNode();

      if (includeKey) {
        wrapper.put("key", new String(consumerRecord.key(), StandardCharsets.UTF_8));
      }

      if (includeMetadata) {
        final ObjectNode metadata = JsonNodeFactory.instance.objectNode();
        metadata.put("partition", consumerRecord.partition());
        metadata.put("offset", consumerRecord.offset());
        metadata.put("timestamp", consumerRecord.timestamp());
        wrapper.set("metadata", metadata);
      }

      wrapper.set("value", node);

      node = wrapper;
    }

    return node;
  }


  public boolean display(final ConsumerRecord<byte[], byte[]> consumerRecord) {

    try {
      final JsonNode node = convert(consumerRecord);

      if (filter != null) {
        final ArrayNode filtered = JsonPath.using(configuration).parse(node).read(filter, ArrayNode.class);
        if (filtered.isEmpty()) {
          return false;
        }
      }

      highlights.forEach(highlight -> {
        highlightService.setHighlights(highlight.getLeft(), JsonPath.using(configuration).parse(node).read(highlight.getRight(), ArrayNode.class));
      });

      objectMapper.writeValue(jsonGenerator, node);

      highlightService.clearHighlights();

      return true;

    } catch (Exception e) {
      final String key = new String(consumerRecord.key(), StandardCharsets.UTF_8);
      log.error("unable to parse value for key={}", key, e);
      console.err().println(String.format("unable to read value for key=%s as json", key));
      return false;
    }
  }

}
