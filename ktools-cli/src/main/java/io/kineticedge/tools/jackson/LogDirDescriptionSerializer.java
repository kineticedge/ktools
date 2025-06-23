package io.kineticedge.tools.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.apache.kafka.clients.admin.LogDirDescription;
import org.apache.kafka.clients.admin.ReplicaInfo;
import org.apache.kafka.common.TopicPartition;

import java.io.IOException;
import java.util.Map;

public class LogDirDescriptionSerializer extends StdSerializer<LogDirDescription> {

  public LogDirDescriptionSerializer() {
    super(LogDirDescription.class);
  }

  @Override
  public void serialize(LogDirDescription value, JsonGenerator gen, SerializerProvider provider)
          throws IOException {
    gen.writeStartObject();
    gen.writeFieldName("replicas");
    gen.writeStartObject();

    for (Map.Entry<TopicPartition, ReplicaInfo> entry : value.replicaInfos().entrySet()) {
      TopicPartition tp = entry.getKey();
      gen.writeFieldName(tp.topic() + "-" + tp.partition());
      provider.defaultSerializeValue(entry.getValue(), gen);
    }

    gen.writeEndObject();
    gen.writeEndObject();
  }

}
