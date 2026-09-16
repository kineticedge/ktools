package io.kineticedge.tools.cmd.kccf.deserializer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.kineticedge.tools.cmd.kccf.jackson.ObjectMapperFactory;
import io.kineticedge.tools.exception.DeserializationException;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.io.JsonEncoder;
import org.apache.kafka.common.serialization.Deserializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

public class AvroDeserializer implements Deserializer<JsonNode> {

  private static final ObjectMapper objectMapper = ObjectMapperFactory.objectMapper();

  private final KafkaAvroDeserializer innerDeserializer;

  public AvroDeserializer() {
    innerDeserializer = new KafkaAvroDeserializer();
  }

  @Override
  public void configure(Map<String, ?> configs, boolean isKey) {
    innerDeserializer.configure(configs, isKey);
  }

  @Override
  public JsonNode deserialize(String topic, byte[] data) {
      return toJsonNode((GenericRecord) innerDeserializer.deserialize(topic, data));
  }

  @Override
  public void close() {
    innerDeserializer.close();
  }

  public JsonNode toJsonNode(GenericRecord genericRecord) {
    try {
      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      final DatumWriter<GenericRecord> datumWriter = new GenericDatumWriter<>(genericRecord.getSchema());
      final JsonEncoder encoder = EncoderFactory.get().jsonEncoder(genericRecord.getSchema(), outputStream);
      datumWriter.write(genericRecord, encoder);
      encoder.flush();
      outputStream.close();

      return objectMapper.readTree(outputStream.toString());
    } catch (IOException e) {
      throw new DeserializationException(e);
    }
  }
}
