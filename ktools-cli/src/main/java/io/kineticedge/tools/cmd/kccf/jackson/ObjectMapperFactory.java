package io.kineticedge.tools.cmd.kccf.jackson;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.kineticedge.tools.domain.BrokerAndDirectory;
import io.kineticedge.tools.jackson.BrokerAndDirectorySerializer;
import io.kineticedge.tools.jackson.LogDirDescriptionSerializer;
import io.kineticedge.tools.jackson.ReplicaInfoSerializer;
import io.kineticedge.tools.jackson.TopicPartitionSerializer;
import org.apache.kafka.clients.admin.LogDirDescription;
import org.apache.kafka.clients.admin.ReplicaInfo;
import org.apache.kafka.common.TopicPartition;

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
            .registerModule(new com.fasterxml.jackson.datatype.jdk8.Jdk8Module())
            .registerModule(new JavaTimeModule())
            .registerModule(new SimpleModule()
                    .addSerializer(TopicPartition.class, new TopicPartitionSerializer())
                    .addSerializer(ReplicaInfo .class, new ReplicaInfoSerializer())
                    .addSerializer(LogDirDescription.class, new LogDirDescriptionSerializer())
                    .addSerializer(BrokerAndDirectory.class, new BrokerAndDirectorySerializer())
            )
            .setDefaultPrettyPrinter(new DefaultPrettyPrinter().withArrayIndenter(new DefaultIndenter("  ", "\n")));
  }

}
