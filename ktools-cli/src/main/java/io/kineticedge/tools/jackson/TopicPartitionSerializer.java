package io.kineticedge.tools.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.apache.kafka.common.TopicPartition;

import java.io.IOException;

public class TopicPartitionSerializer extends StdSerializer<TopicPartition> {

  public TopicPartitionSerializer() {
    super(TopicPartition.class);
  }

  @Override
  public void serialize(TopicPartition value, JsonGenerator gen, SerializerProvider provider)
          throws IOException {
    gen.writeString(value.topic() + "-" + value.partition());
//    gen.writeStartObject();
//    gen.writeStringField("topic", value.topic());
//    gen.writeNumberField("partition", value.partition());
//    gen.writeEndObject();
  }
}
