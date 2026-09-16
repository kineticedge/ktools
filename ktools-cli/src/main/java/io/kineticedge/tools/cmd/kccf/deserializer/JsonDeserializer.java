package io.kineticedge.tools.cmd.kccf.deserializer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kineticedge.tools.cmd.kccf.jackson.ObjectMapperFactory;
import io.kineticedge.tools.exception.DeserializationException;
import org.apache.kafka.common.serialization.Deserializer;

import java.io.IOException;

public class JsonDeserializer implements Deserializer<JsonNode> {

  private static final ObjectMapper objectMapper = ObjectMapperFactory.objectMapper();

  @Override
  public JsonNode deserialize(String topic, byte[] data) {
    try {
      return objectMapper.readTree(data);
    } catch (IOException e) {
      throw new DeserializationException(e);
    }
  }
}
