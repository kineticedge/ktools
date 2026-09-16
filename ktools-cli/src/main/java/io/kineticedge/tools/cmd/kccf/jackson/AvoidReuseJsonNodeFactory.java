package io.kineticedge.tools.cmd.kccf.jackson;

import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;

/**
 * BooleanNode and NullNode are reused, this means that the JsonNode returned from
 * JsonPath would match all instance of NullNode, True, or False. This ensures
 * that each instance is unique, so the highlight routine will not match more
 * than what is expected.
 */
public class AvoidReuseJsonNodeFactory extends JsonNodeFactory {

  @Override
  public BooleanNode booleanNode(boolean v) {
    return new NoReuseBooleanNode(v);
  }

  @Override
  public NullNode nullNode() {
    return new NoReuseNullNode();
  }

  public static class NoReuseBooleanNode extends BooleanNode {
    public NoReuseBooleanNode(boolean v) {
      super(v);
    }
  }

  public static class NoReuseNullNode extends NullNode {
    public NoReuseNullNode() {
      super();
    }
  }
}