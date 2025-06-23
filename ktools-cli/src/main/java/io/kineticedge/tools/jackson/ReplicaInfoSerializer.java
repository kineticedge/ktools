package io.kineticedge.tools.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.apache.kafka.clients.admin.ReplicaInfo;
import org.apache.kafka.common.TopicPartition;

import java.io.IOException;

public class ReplicaInfoSerializer extends StdSerializer<ReplicaInfo> {

  public ReplicaInfoSerializer() {
    super(ReplicaInfo.class);
  }

  @Override
  public void serialize(ReplicaInfo value, JsonGenerator gen, SerializerProvider provider) throws IOException {
    gen.writeStartObject();
    gen.writeNumberField("size", value.size());
    gen.writeNumberField("offsetLag", value.offsetLag());
    gen.writeBooleanField("future", value.isFuture());
    gen.writeEndObject();
  }

}
