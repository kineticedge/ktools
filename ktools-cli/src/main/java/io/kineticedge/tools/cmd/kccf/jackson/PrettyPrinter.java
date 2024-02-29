package io.kineticedge.tools.cmd.kccf.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import io.kineticedge.tools.cmd.kccf.util.HighlightService;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class PrettyPrinter extends DefaultPrettyPrinter {

  private final transient HighlightService highlightService;

  public PrettyPrinter(final HighlightService highlightService) {
    this.highlightService = highlightService;
  }

  public PrettyPrinter(DefaultPrettyPrinter defaultPrettyPrinter, final HighlightService highlightService) {
    super(defaultPrettyPrinter);
    this.highlightService = highlightService;
  }

  @NotNull
  @Override
  public DefaultPrettyPrinter createInstance() {
    return new PrettyPrinter(this, highlightService);
  }

  @Override
  public void writeStartObject(JsonGenerator g) throws IOException {
    g.writeRaw(highlightService.pushColor(g.currentValue()));
    super.writeStartObject(g);
  }

  @Override
  public void writeEndObject(JsonGenerator g, int nrOfEntries) throws IOException {
    super.writeEndObject(g, nrOfEntries);
    g.writeRaw(highlightService.popColor(g.currentValue()));
  }

  @Override
  public void writeStartArray(JsonGenerator g) throws IOException {
    g.writeRaw(highlightService.pushColor(g.currentValue()));
    super.writeStartArray(g);
  }

  @Override
  public void writeEndArray(JsonGenerator g, int nrOfValues) throws IOException {
    super.writeEndArray(g, nrOfValues);
    g.writeRaw(highlightService.popColor(g.currentValue()));
  }

}
