package io.kineticedge.tools.cmd.kccf.deserializer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaDeserializer;
import io.kineticedge.tools.cmd.kccf.jackson.ObjectMapperFactory;
import org.apache.kafka.common.serialization.Deserializer;

import java.util.Map;

public class JsonSchemaDeserializer implements Deserializer<JsonNode> {

  private static final ObjectMapper objectMapper = ObjectMapperFactory.objectMapper();

  private final KafkaJsonSchemaDeserializer<JsonNode> innerDeserializer;

  public JsonSchemaDeserializer() {
    innerDeserializer = new KafkaJsonSchemaDeserializer<>();
  }

  @Override
  public void configure(Map<String, ?> configs, boolean isKey) {
    innerDeserializer.configure(configs, isKey);
  }

  @Override
  public JsonNode deserialize(String topic, byte[] data) {
    return objectMapper.convertValue(innerDeserializer.deserialize(topic, data), JsonNode.class);
  }

  @Override
  public void close() {
    innerDeserializer.close();
  }
}
