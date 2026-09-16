package io.kineticedge.tools.cmd.kccf;

import com.fasterxml.jackson.databind.JsonNode;
import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.json.JsonSchema;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDe;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaDeserializer;
import io.kineticedge.tools.cmd.kccf.config.KccfConfig;
import io.kineticedge.tools.cmd.kccf.deserializer.AvroDeserializer;
import io.kineticedge.tools.cmd.kccf.deserializer.JsonSchemaDeserializer;
import io.kineticedge.tools.cmd.truncate.TestConsole;
import io.kineticedge.tools.console.Color;
import io.kineticedge.tools.exception.CommandException;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

class JsonPathTest {

  private static final String COLOR_RESET = "\033[0m";

  private static final byte[] JSON = read("example.json");
  private static final String JSON_SCHEMA = readAsString("schemas/purchaseOrder.json");
  private static final String AVRO_SCHEMA = readAsString("schemas/purchaseOrder.avsc");

  private TestConsole testConsole;

  @BeforeEach
  public void beforeEach() {
    testConsole = new TestConsole();
  }

  @ParameterizedTest
  @CsvSource({
          "$..[?(@.sku =~ /0.+/)], false, false, true",
          "$..[?(@.sku =~ /99.+/)], false, false, false",
          "$..[?(@.sku == \"001\")], false, false, true",
          "$.items[0][?(@.sku == \"001\")], false, false, true",
          "$.items[1][?(@.sku == \"001\")], false, false, false",
          "$.items[99][?(@.sku == \"001\")], false, false, false",
          "$.key, false, false, false",

          "$..[?(@.sku =~ /0.+/)], true, false, true",
          "$..[?(@.sku =~ /99.+/)], true, false, false",
          "$..[?(@.sku == \"001\")], true, false, true",
          "$.items[0][?(@.sku == \"001\")], true, false, false",
          "$.items[1][?(@.sku == \"001\")], true, false, false",
          "$.items[99][?(@.sku == \"001\")], true, false, false",
          "$.key, true, false, true",

          "$..[?(@.sku =~ /0.+/)], false, true, true",
          "$..[?(@.sku =~ /99.+/)], false, true, false",
          "$..[?(@.sku == \"001\")], false, true, true",
          "$..items[0][?(@.sku == \"001\")], false, true, true",
          "$..items[1][?(@.sku == \"001\")], false, true, false",
          "$..items[99][?(@.sku == \"001\")], false, true, false",
          "$.key, false, true, false",

          "$..[?(@.sku =~ /0.+/)], true, true, true",
          "$..[?(@.sku =~ /99.+/)], true, true, false",
          "$..[?(@.sku == \"001\")], true, true, true",
          "$..items[0][?(@.sku == \"001\")], true, true, true",
          "$..items[1][?(@.sku == \"001\")], true, true, false",
          "$..items[99][?(@.sku == \"001\")], true, true, false",
          "$.key, true, true, true",

  })
  void testFilter(final String filter, final boolean includeKey, final boolean includeMetadata, final boolean success) {

    KccfConfig config = kccfConfig("JSON", filter, includeKey, includeMetadata, Collections.emptyList());
    // KccfConfig config = kccfConfig("JSON_SCHEMA", filter, includeKey, includeMetadata, Collections.emptyList());

    JsonConsole x = new JsonConsole(config);

    ConsumerRecord<byte[], byte[]> rec = new ConsumerRecord<>("topic", 0, 0L, "key".getBytes(StandardCharsets.UTF_8), JSON);

    x.display(rec);

    testConsole.flush();

    System.out.println(testConsole.asString());
    Assertions.assertEquals(success, !testConsole.asString().isEmpty());
  }

  @ParameterizedTest
  @CsvSource({
          "RED=$..[?(@.sku =~ /2.+/)],1,0",
          "$..[?(@.sku =~ /2.+/)],1,0",
          "GREEN=$..[?(@.sku =~ /2.+/)],0,1",
          "GREEN=$..[?(@.sku =~ /0.+/)],0,3",
          "GREEN=$..items,0,1",
          "GREEN=$..items[*],0,4",
  })
  void testHighlightNonOverlap(String highlight, int red, int green) {

    KccfConfig config = kccfConfig("JSON", "$.*", false, false, Collections.singletonList(highlight));

    JsonConsole x = new JsonConsole(config);

    ConsumerRecord<byte[], byte[]> rec = new ConsumerRecord<>("topic", 0, 0L, "key".getBytes(StandardCharsets.UTF_8), JSON);

    x.display(rec);

    testConsole.flush();

    String result = testConsole.asString();

    Assertions.assertEquals(red, StringUtils.countMatches(result, Color.RED.getAscii()));
    Assertions.assertEquals(green, StringUtils.countMatches(result, Color.GREEN.getAscii()));

    Assertions.assertEquals(red + green, StringUtils.countMatches(result, COLOR_RESET));

    Assertions.assertEquals(0, StringUtils.countMatches(result, Color.BLACK.getAscii()));
    Assertions.assertEquals(0, StringUtils.countMatches(result, Color.YELLOW.getAscii()));
    Assertions.assertEquals(0, StringUtils.countMatches(result, Color.PURPLE.getAscii()));
    Assertions.assertEquals(0, StringUtils.countMatches(result, Color.CYAN.getAscii()));
    Assertions.assertEquals(0, StringUtils.countMatches(result, Color.WHITE.getAscii()));


  }

  @ParameterizedTest
  @CsvSource({
          "RED=$..items,YELLOW=$..sku,5,4",
          "RED=$..items[*],YELLOW=$..sku,8,4",
  })
  void testHighlightOverlap(String highlight, String highlight2, int red, int yellow) {

    KccfConfig config = kccfConfig("JSON", "$.*", false, false, List.of(highlight, highlight2));

    JsonConsole x = new JsonConsole(config);

    ConsumerRecord<byte[], byte[]> rec = new ConsumerRecord<>("topic", 0, 0L, "key".getBytes(StandardCharsets.UTF_8), JSON);

    x.display(rec);

    testConsole.flush();

    String result = testConsole.asString();

    Assertions.assertEquals(red, StringUtils.countMatches(result, Color.RED.getAscii()));
    Assertions.assertEquals(yellow, StringUtils.countMatches(result, Color.YELLOW.getAscii()));

    //
    Assertions.assertEquals(red - yellow, StringUtils.countMatches(result, COLOR_RESET));

    Assertions.assertEquals(0, StringUtils.countMatches(result, Color.BLACK.getAscii()));
    Assertions.assertEquals(0, StringUtils.countMatches(result, Color.GREEN.getAscii()));
    Assertions.assertEquals(0, StringUtils.countMatches(result, Color.PURPLE.getAscii()));
    Assertions.assertEquals(0, StringUtils.countMatches(result, Color.CYAN.getAscii()));
    Assertions.assertEquals(0, StringUtils.countMatches(result, Color.WHITE.getAscii()));
  }


  @ParameterizedTest
  @CsvSource({
          "RED=$..items,YELLOW=$..sku,5,4",
          "RED=$..items[*],YELLOW=$..sku,8,4",
  })
  void testJsonSchema(String highlight, String highlight2, int red, int yellow) {

    final KccfConfig config = kccfConfig("JSON-SCHEMA", "$.*", false, false, List.of(highlight, highlight2));

    final JsonConsole jsonConsole = new JsonConsole(config);

    JsonSchema schema = new JsonSchema(JSON_SCHEMA);

    // register schema and create 0x00 + registered schema id as 4 additional bytes.
    final byte[] prefix = registerJsonSchema(jsonConsole, schema);

    final ConsumerRecord<byte[], byte[]> rec = new ConsumerRecord<>("topic", 0, 0L, "key".getBytes(StandardCharsets.UTF_8), concat(prefix, JSON));

    jsonConsole.display(rec);

    testConsole.flush();

    String result = testConsole.asString();

    Assertions.assertEquals(red, StringUtils.countMatches(result, Color.RED.getAscii()));
    Assertions.assertEquals(yellow, StringUtils.countMatches(result, Color.YELLOW.getAscii()));
    Assertions.assertEquals(red - yellow, StringUtils.countMatches(result, COLOR_RESET));
  }

  @ParameterizedTest
  @CsvSource({
          "RED=$..userId,YELLOW=$..storeId,1,1"
  })
  void testAvroSchema(String highlight, String highlight2, int red, int yellow) {

    final KccfConfig config = kccfConfig("AVRO", "$.*", false, false, List.of(highlight, highlight2));

    final JsonConsole jsonConsole = new JsonConsole(config);

    AvroSchema schema = new AvroSchema(AVRO_SCHEMA);

    // register schema and create 0x00 + registered schema id as 4 additional bytes.
    final byte[] prefix = registerAvroSchema(jsonConsole, schema);

    final ConsumerRecord<byte[], byte[]> rec = new ConsumerRecord<>("topic", 0, 0L, "key".getBytes(StandardCharsets.UTF_8), concat(prefix, avroMessage()));

    jsonConsole.display(rec);

    testConsole.flush();

    String result = testConsole.asString();

    Assertions.assertEquals(red, StringUtils.countMatches(result, Color.RED.getAscii()));
    Assertions.assertEquals(yellow, StringUtils.countMatches(result, Color.YELLOW.getAscii()));
    Assertions.assertEquals(red + yellow, StringUtils.countMatches(result, COLOR_RESET));
  }

  @SuppressWarnings("unchecked")
  private static byte[] registerJsonSchema(JsonConsole x, ParsedSchema schema) {
    try {
      final Field field = JsonConsole.class.getDeclaredField("deserializer");
      field.setAccessible(true);
      final JsonSchemaDeserializer deserializer = (JsonSchemaDeserializer) field.get(x);

      final Field field2 = JsonSchemaDeserializer.class.getDeclaredField("innerDeserializer");
      field2.setAccessible(true);
      final KafkaJsonSchemaDeserializer<JsonNode> inner = (KafkaJsonSchemaDeserializer<JsonNode>) field2.get(deserializer);

      Field field3 = AbstractKafkaSchemaSerDe.class.getDeclaredField("schemaRegistry");
      field3.setAccessible(true);
      MockSchemaRegistryClient src = (MockSchemaRegistryClient) field3.get(inner);

      int id = src.register("foo", schema);

      return ByteBuffer.allocate(5).put((byte) 0).putInt(id).array();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static byte[] registerAvroSchema(JsonConsole x, ParsedSchema schema) {
    try {
      final Field field = JsonConsole.class.getDeclaredField("deserializer");
      field.setAccessible(true);
      final AvroDeserializer deserializer = (AvroDeserializer) field.get(x);

      final Field field2 = AvroDeserializer.class.getDeclaredField("innerDeserializer");
      field2.setAccessible(true);
      final KafkaAvroDeserializer inner = (KafkaAvroDeserializer) field2.get(deserializer);

      Field field3 = AbstractKafkaSchemaSerDe.class.getDeclaredField("schemaRegistry");
      field3.setAccessible(true);
      MockSchemaRegistryClient src = (MockSchemaRegistryClient) field3.get(inner);

      int id = src.register("foo", schema);

      return ByteBuffer.allocate(5).put((byte) 0).putInt(id).array();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  private KccfConfig kccfConfig(final String format, final String filter, final boolean includeKey, final boolean includeMetadata, final List<String> highlights) {
    KccfConfig config = new KccfConfig();

    List<String> args = new ArrayList<>();
    args.add("--schema-registry");
    args.add("mock://localhost:9999");
    args.add("--format");
    args.add(format);
    args.add("--filter");
    args.add(filter);
    if (includeKey) {
      args.add("--include-key");
    }
    if (includeMetadata) {
      args.add("--include-metadata");
    }
    args.add("--highlights");
    //args.add("$..sku");
    args.addAll(highlights);

    new CommandLine(config).parseArgs(args.toArray(new String[0]));

    try {
      Field field = KccfConfig.class.getDeclaredField("console");
      field.setAccessible(true);
      field.set(config, testConsole);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    return config;
  }


  private static byte[] concat(final byte[] part1, final byte[] part2) {
    final byte[] result = new byte[part1.length + part2.length];
    System.arraycopy(part1, 0, result, 0, part1.length);
    System.arraycopy(part2, 0, result, part1.length, part2.length);
    return result;
  }

  public byte[] avroMessage() {
    try {
      org.apache.avro.Schema schema = new Schema.Parser().parse(AVRO_SCHEMA);


      GenericRecord record = new GenericData.Record(schema);

      record.put("timestamp", System.currentTimeMillis());
      record.put("orderId", "SV-00001549");
      record.put("userId", "001");
      record.put("storeId", "00A");
      record.put("items", new GenericData.Array<>(0, schema.getField("items").schema()));

      // Create a writer to serialize the record
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      DatumWriter<GenericRecord> writer = new GenericDatumWriter<>(schema);
      Encoder encoder = EncoderFactory.get().binaryEncoder(out, null);

      // Write the data and get the bytes
      writer.write(record, encoder);
      encoder.flush();
      out.close();

      return out.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static String readAsString(String resource) {
    return new String(read(resource));
  }

  private static byte[] read(String resource) {
    try {
      InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
        return reader.lines().collect(Collectors.joining(System.lineSeparator())).getBytes(StandardCharsets.UTF_8);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}