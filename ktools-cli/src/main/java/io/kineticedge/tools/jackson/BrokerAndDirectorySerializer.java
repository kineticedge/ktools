package io.kineticedge.tools.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import io.kineticedge.tools.domain.BrokerAndDirectory;
import org.apache.kafka.common.TopicPartition;

import java.io.IOException;

public class BrokerAndDirectorySerializer extends StdSerializer<BrokerAndDirectory> {

  public BrokerAndDirectorySerializer() {
    super(BrokerAndDirectory.class);
  }

  @Override
  public void serialize(BrokerAndDirectory value, JsonGenerator gen, SerializerProvider provider) throws IOException {
    gen.writeString(value.brokerId() + ":" + value.directory());
  }
}
