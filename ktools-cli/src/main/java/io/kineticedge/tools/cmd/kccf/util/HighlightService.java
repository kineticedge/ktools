package io.kineticedge.tools.cmd.kccf.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.kineticedge.tools.console.Color;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.stream.StreamSupport;

public class HighlightService {

  private static final String COLOR_RESET = "\033[0m";

  private final Map<JsonNode, Color> highlighted = new IdentityHashMap<>();
  private final Deque<Color> stack = new ArrayDeque<>();

  public void setHighlights(final Color color, final ArrayNode nodes) {
    StreamSupport.stream(nodes.spliterator(), false).forEach(n -> highlighted.put(n, color));
  }

  public void clearHighlights() {
    highlighted.clear();
  }

  // JsonGenerator has Object for currentVersion datatype, handles the scenario where that isn't a JsonNode here.
  public String pushColor(Object object) {
    if (object instanceof JsonNode jsonNode) {
      return pushColor(jsonNode);
    } else {
      return Color.NONE.getAscii();
    }
  }

  /**
   * Nothing is pushed onto the stack, unless the jsonNode is in the list of highlighted elements.
   */
  public String pushColor(JsonNode jsonNode) {

    if (jsonNode == null) {
      return Color.NONE.getAscii();
    }

    final Color color = highlighted.get(jsonNode);
    if (color != null) {
      stack.push(color);
      return color.getAscii();
    } else {
      return Color.NONE.getAscii();
    }
  }

  // JsonGenerator has Object for currentVersion datatype, handles the scenario where that isn't a JsonNode here.
  public String popColor(Object object) {
    if (object instanceof  JsonNode jsonNode) {
      return popColor(jsonNode);
    } else {
      return Color.NONE.getAscii();
    }
  }

  /**
   * Nothing is popped from the stack, unless the jsonNode is in the list of highlighted elements; ensuring it was pushed in the first place.
   */
  public String popColor(JsonNode jsonNode) {

    if (jsonNode == null) {
      return Color.NONE.getAscii();
    }

    if (highlighted.containsKey(jsonNode)) {
      stack.pop();
      if (stack.isEmpty()) {
        return COLOR_RESET;
      } else {
        return stack.peek().getAscii();
      }
    } else {
      return Color.NONE.getAscii();
    }
  }

}
