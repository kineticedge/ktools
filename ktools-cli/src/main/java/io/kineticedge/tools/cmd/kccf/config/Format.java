package io.kineticedge.tools.cmd.kccf.config;

public enum Format {

  JSON(false),
  JSON_SCHEMA(true),
  AVRO(true);

  private final boolean usesSchemaRegistry;

  Format(final boolean usesSchemaRegistry) {
    this.usesSchemaRegistry = usesSchemaRegistry;
  }

  public boolean usesSchemaRegistry() {
    return usesSchemaRegistry;
  }

}
