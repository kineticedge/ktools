package io.kineticedge.tools.cmd.truncate;

import io.kineticedge.tools.exception.CommandException;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.CreateTopicsOptions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

class TruncateTopicTest {

  private static final Logger log = LoggerFactory.getLogger(TruncateTopicTest.class);

  private static final String IMAGE = "apache/kafka-native:latest";

  protected static final org.testcontainers.kafka.KafkaContainer kafka = new org.testcontainers.kafka.KafkaContainer(
          DockerImageName.parse(IMAGE).asCompatibleSubstituteFor("apache/kafka"))
          .withStartupTimeout(Duration.ofSeconds(15))
          .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "false")
          .withLogConsumer(outputFrame -> log.info(outputFrame.getUtf8String()));

  //private static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka")).withKraft();

  private static Map<String, Object> config;

  private static KafkaProducer<String, String> producer;

  private TestConsole console;

  @BeforeAll
  static void start() throws ExecutionException, InterruptedException {

    kafka.start();

    config = Map.ofEntries(Map.entry(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers()));

    producer = new KafkaProducer<>(Map.ofEntries(
            Map.entry(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers()),
            Map.entry(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class),
            Map.entry(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class)
    ));

    try (Admin admin = Admin.create(config)) {
      admin.createTopics(
              List.of(
                      new NewTopic("topic_a", 4, (short) -1).configs(Map.ofEntries(Map.entry("cleanup.policy", "delete"))),
                      new NewTopic("topic_b", 4, (short) -1).configs(Map.ofEntries(Map.entry("cleanup.policy", "delete,compact"))),
                      new NewTopic("topic_c", 4, (short) -1).configs(Map.ofEntries(Map.entry("cleanup.policy", "compact"))),
                      new NewTopic("topic_d", 4, (short) -1).configs(Map.ofEntries(Map.entry("cleanup.policy", "delete")))
              ),
              new CreateTopicsOptions()
      ).all().get();
    }
  }

  @AfterAll
  static void stop() {
    producer.close();
    kafka.stop();
  }

  @BeforeEach
  void before() {
    console = new TestConsole();
  }

  @Test
  void testInvalidTopic() {
    try (TruncateTopic truncateTopic = new TruncateTopic(config, console)) {
      Assertions.assertThrows(CommandException.class, () -> {
        truncateTopic.execute("invalid", true, false);
      });
    }
  }

  @Test
  void testTopicWithDeleteCleanupPolicy() {

    publish("topic_a", "k1", "v1");
    publish("topic_a", "k2", "v2");

    try (TruncateTopic truncateTopic = new TruncateTopic(config, console)) {
      truncateTopic.execute("topic_a", true, false);

      Assertions.assertEquals(
              """
                      2 messages to be deleted over 4 partitions
                      """,
              console.asString()
      );

      Assertions.assertEquals(
              "",
              console.errAsString()
      );

      //
      console.reset();

      publish("topic_a", "k3", "v3");

      truncateTopic.execute("topic_a", true, true);

      Assertions.assertEquals(
              """
                      1 messages to be deleted over 4 partitions
                      """,
              console.asString());
      Assertions.assertEquals("""
                      topic topic_a has a delete cleanup policy, --force is not necessary.
                      """,
              console.errAsString());

      //
      console.reset();

      truncateTopic.execute("topic_a", true, true);

      Assertions.assertEquals("""
                      no messages to delete.
                      """,
              console.asString());

    }
  }

  @Test
  void testTopicWithDeleteAndCompactCleanupPolicy() {

    publish("topic_b", "k1", "v1");

    try (TruncateTopic truncateTopic = new TruncateTopic(config, console)) {
      truncateTopic.execute("topic_b", true, false);

      Assertions.assertEquals("""
                      1 messages to be deleted over 4 partitions
                      """,
              console.asString());

      Assertions.assertEquals("", console.errAsString());

    }
  }

  @Test
  void testTopicWithCleanupPolicy() {

    publish("topic_c", "k1", "v1");

    try (TruncateTopic truncateTopic = new TruncateTopic(config, console)) {

      //

      truncateTopic.execute("topic_c", true, false);
      Assertions.assertEquals("""
                      1 messages to be deleted over 4 partitions
                      """,
              console.asString());
      Assertions.assertEquals("""
                      topic topic_c does not have a delete cleanup policy with, use '--force' which will add and then remove the 'delete' cleanup policy.
                      """,
              console.errAsString());
      checkCleanupPolicyDoesNotHaveDelete(truncateTopic);

      //
      console.reset();

      truncateTopic.execute("topic_c", true, true);
      Assertions.assertEquals("""
                      1 messages to be deleted over 4 partitions
                      """,
              console.asString());
      Assertions.assertEquals("",
              console.errAsString());
      checkCleanupPolicyDoesNotHaveDelete(truncateTopic);

      //
      console.reset();

      truncateTopic.execute("topic_c", true, true);
      Assertions.assertEquals("""
                      no messages to delete.
                      """,
              console.asString());
      Assertions.assertEquals("",
              console.errAsString());
      checkCleanupPolicyDoesNotHaveDelete(truncateTopic);

    }
  }

  @Test
  void testTopicWithDeleteCleanupPolicyNoExecute() {

    publish("topic_d", "k1", "v1");
    publish("topic_d", "k2", "v2");

    try (TruncateTopic truncateTopic = new TruncateTopic(config, console)) {
      truncateTopic.execute("topic_d", false, false);

      Assertions.assertEquals("""
                      2 messages to be deleted over 4 partitions
                      enable --execute to issue command
                      """,
              console.asString());
      Assertions.assertEquals("", console.errAsString());

      truncateTopic.execute("topic_d", false, false);

      Assertions.assertEquals("""
                      2 messages to be deleted over 4 partitions
                      enable --execute to issue command
                      2 messages to be deleted over 4 partitions
                      enable --execute to issue command
                      """,
              console.asString());
      Assertions.assertEquals("", console.errAsString());

    }
  }

  private static void checkCleanupPolicyDoesNotHaveDelete(TruncateTopic truncateTopic) {
    //instead of rewriting this business-logic here, leverage the private method of truncate topic to verify.
    // no in a purest standpoint, using this to test code is not correct, because I assume this is working;
    // but I am assuming the check logic is valid to test the other logic within the class.
    try {
      Method method = TruncateTopic.class.getDeclaredMethod("hasDeleteCleanupPolicy", String.class);
      method.setAccessible(true);
      Boolean value = (Boolean) method.invoke(truncateTopic, "topic_c");
      Assertions.assertFalse(value);
    } catch (Exception e) {
      Assertions.fail("unable to verify cleanup.policy remained unchanged.");
    }
  }

  private void publish(final String topic, final String key, final String value) {
    producer.send(new ProducerRecord<>(topic, key, value));
    producer.flush();
  }

  private static String bootstrapServers() {
    return kafka.getBootstrapServers();
  }

}