package io.kineticedge.tools.cmd.kccf.jackson;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class ObjectMapperFactory {

  private ObjectMapperFactory() {
  }

  /**
   * Constructs an Object Mapper that uses a JsonNode Factory that will not reuse Boolean and Null Nodes,
   * so JsonPath selection is distinct. Since an object mapper is not immutable, always create a new
   * instance so a modification of an objectMapper by a given instance does not impact another.
   */
  public static ObjectMapper objectMapper() {
    return new ObjectMapper()
            .setNodeFactory(new AvoidReuseJsonNodeFactory())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
//            .registerModule(new com.fasterxml.jackson.datatype.Jdk8Module())
            .registerModule(new JavaTimeModule())
            .findAndRegisterModules();

  }


}
