# KTools

KTools is a set of tools for making development easier with Apache Kafka.

## License

This is licensed under Apache 2.0 Licensing.

## Disclaimer

Any cli tools provided within this project are designed and only recommended to be used for development purposes, 
not for production cluster.

## Installation

* Download the release .tar artifact.

* `tar xfv ./ktools-cli-v0.0.2.tar`

* Consider adding `./ktools-cli-v0.0.2/bin` to your classpath.

* `./ktools-cli-v0.0.2/bin/kafka-topic-truncate`
* `./ktools-cli-v0.0.2/bin/kafka-console-consumer-filter`

* This project is build with Gradle assembly plugin and picocli command-line library. As tools are added,
scripts in the bin directory will be included to wrap the call to picocli to allow for a seamless integration.


## KTools CLI

The `ktools-cli` artifact provides command-line interface tools.

### `kafka-topic-truncate`

This tool will delete the contents of a topic. It leverages the Kafka Admin API `deleteRecords`. There are many times
during development, where it can become handy to quickly delete the contents of a topic; especially with clusters
where deleting topics is not enabled. This tool will delete all offsets up to the latest offset on all partitions
of a topic.

* `--topic` The topic to truncate

* `--bootstrap-server` the kafka server to connect to.

* `--command-config` any additional configuration parameters needed to connect to the kafka server; this can
contain `bootstrap.servers` allowing for the `--bootstrap-server` flag to be optional.

* `--execute` actually perform the operation w/out this switch it is a dry-run operation giving you messages
that will be deleted and the number of partitions for the topic.

* `--force` the delete operation is only available on topics that have a `delete` `cleanup.policy`. The force
switch will have kafka-topic-truncate temporarily add this cleanup policy, so messages in it can be deleted
(truncate the topic).

#### examples

```bash
kafka-topic-truncate --bootstrap-server localhost:9092 --topic example
4 messages to be deleted over 4 partitions
enable --execute to issue command
```

```bash
kafka-topic-truncate --bootstrap-server localhost:9092 --topic example --execute
4 messages to be deleted over 4 partitions
```

```bash
kafka-topic-truncate --bootstrap-server localhost:9092 --topic example --execute
no messages to delete.
```


### `kafka-console-consumer-filter`

This tool will filter JSON, JSON Schema, and Avro messages by a JSON Path filter. It includes additional
operations to highlight message fragments (using JSON Path) and other controls to control on where
consumption starts from and stops at.

* `--topic` the topic to truncate

* `--partitions` which partitions to consume (defaults is all partitions of the topic).

* `--bootstrap-server` the kafka server to connect to.

* `--consumer.config` any additional configuration parameters needed to connect to the kafka server; this can
  contain `bootstrap.servers` allowing for the `--bootstrap-server` flag to be optional.

* `--format` the format of the messages, currently supported: 'json', Confluent's SR based 'json-schema', and Confluent's SR based 'avro'.

* `--filter` filter records to only those that match this JSON Path.

* `--highlights` JSON Path to highlight COLOR=Path (COLOR= optional).

* `--default-color` if highlight is not supplied with a color, this default highlight color is used. The default default-color is RED.

* `--schema-registry` required for 'avro' formatted messages (can be defined in the schema-registry config).

* `--schema-registry.config` property file containing configs for Kafka consumer.

* `--include-key` wrap the message in `value` element and include a string representation of the key as `key`.

* `--include-metadata` wrap the message in a `value` element and include the metadata in a `metadata` element.

* `--max-messages` maximum number of messages to read.

* `--start` the time to start consuming from. use 'earliest' for beginning or 'latest' or 'now' for end; latest is the default, format: yyyy-MM-dd(T, )HH:mm:ss.SSSZ.

* `--rewind` additional duration to *subtract* from starting time, most useful with 'earliest' and 'latest'.

* `--end` the time to end consuming, use 'now' to indicate current time, format: yyyy-MM-dd(T, )HH:mm:ss.SSSZ.

* `--forward` additional duration to *add* to ending time, most useful with 'now', can be negative.

#### examples

```bash
kafka-console-consumer-filter --bootstrap-server localhost:9092 --topic orders-pickup \
    --filter "$..[?(@.price < 1.0)]" \
    --highlight "BLUE=$..items" \
    --highlight "RED=$..[?(@.price < 1.0)].price" \
    --highlight "GREEN=$..[?(@.price > 30.0)].price" \
    --highlight "YELLOW=$..sku" \
    --include-key \
    --include-metadata \
    --start earliest \
    --end now
```

```bash
kafka-console-consumer-filter --bootstrap-server localhost:9092 --topic orders-pickup \
    --filter "$..[?( @.items.length() >= 4 )]" \
    --highlight "BLUE=$..items" \
    --highlight "$..price" \
    --highlight "Purple=$..name" \
    --rewind 1d \
    --end now
```

```bash
kafka-console-consumer-filter --bootstrap-server localhost:9092 --topic purchase-orders \
    --format avro \
    --schema-registry http://localhost:8081 \
    --highlight "BLUE=$..orderId" \
    --highlight "$..price" \
    --start "2024-03-11T17Z" \
    --end now
```
