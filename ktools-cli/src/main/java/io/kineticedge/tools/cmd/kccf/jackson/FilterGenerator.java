package io.kineticedge.tools.cmd.kccf.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.JsonGeneratorDelegate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.kineticedge.tools.cmd.kccf.util.HighlightService;

import java.io.IOException;

public class FilterGenerator extends JsonGeneratorDelegate {

  private final HighlightService highlightService;

  public FilterGenerator(JsonGenerator d, HighlightService highlightService) {
    super(d);
    this.highlightService = highlightService;
  }

  @Override
  public void writeString(String text) throws IOException {
    final JsonNode jsonNode = jsonNode();
    super.writeRawValue(highlightService.pushColor(jsonNode) + "\"" + text + "\"" + highlightService.popColor(jsonNode));
  }

  @Override
  public void writeNumber(int value) throws IOException {
    final JsonNode jsonNode = jsonNode();
    super.writeRawValue(highlightService.pushColor(jsonNode) + value + highlightService.popColor(jsonNode));
  }

  @Override
  public void writeNumber(double value) throws IOException {
    final JsonNode jsonNode = jsonNode();
    super.writeRawValue(highlightService.pushColor(jsonNode) + value + highlightService.popColor(jsonNode));
  }

  @Override
  public void writeNumber(short value) throws IOException {
    final JsonNode jsonNode = jsonNode();
    super.writeRawValue(highlightService.pushColor(jsonNode) + value + highlightService.popColor(jsonNode));
  }

  @Override
  public void writeNumber(long value) throws IOException {
    final JsonNode jsonNode = jsonNode();
    super.writeRawValue(highlightService.pushColor(jsonNode) + value + highlightService.popColor(jsonNode));
  }

  @Override
  public void writeBoolean(boolean state) throws IOException {
    final JsonNode jsonNode = jsonNode();
    super.writeRawValue(highlightService.pushColor(jsonNode) + state + highlightService.popColor(jsonNode));
  }

  @Override
  public void writeNull() throws IOException {
    final JsonNode jsonNode = jsonNode();
    super.writeRawValue(highlightService.pushColor(jsonNode) + "null" + highlightService.popColor(jsonNode));
  }

  private JsonNode jsonNode() {
    if (currentValue() == null || getOutputContext().getCurrentName() == null) {
      return null;
    } else if (currentValue() instanceof ObjectNode objectNode) {
      return objectNode.get(getOutputContext().getCurrentName());
    } else {
      return null;
    }
  }
}