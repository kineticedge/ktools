package io.kineticedge.tools.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.Instant;

public class InstantSerializer extends JsonSerializer<Instant> {
    public void serialize(final Instant value, final JsonGenerator gen, final SerializerProvider provider) throws IOException {
        gen.writeString(value.toString());
    }
}
