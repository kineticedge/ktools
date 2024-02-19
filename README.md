# KTools

KTools is a set of tools for making development easier with Apache Kafka.

## License

This is licensed under Apache 2.0 Licensing.

## Disclaimer

Any cli tools provided within this project are designed and only recommended to be used for development purposes, 
not for production cluster.

## Installation

* Download the release .tar artifact.

* `tar xfv ./ktools-cli-v0.0.1.tar`

* Consider adding `./ktools-cli-v0.0.1/bin` to your classpath.

* `./ktools-cli-v0.0.1/bin/kafka-topic-truncate`

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

```
kafka-topic-truncate --bootstrap-server localhost:9092 --topic example
4 messages to be deleted over 4 partitions
enable --execute to issue command
```

```
kafka-topic-truncate --bootstrap-server localhost:9092 --topic example --execute
4 messages to be deleted over 4 partitions
```

```
kafka-topic-truncate --bootstrap-server localhost:9092 --topic example --execute
no messages to delete.
```