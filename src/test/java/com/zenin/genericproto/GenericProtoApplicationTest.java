package com.zenin.genericproto;

import com.google.protobuf.Message;
import com.zenin.genericproto.test.KafkaContainerSBAware;
import com.zenin.genericproto.test.MockRegistryBeans;
import com.zenin.models.EnvironmentReadingsOuterClass.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import static com.google.protobuf.util.Timestamps.fromMillis;
import static java.lang.System.currentTimeMillis;

@SpringBootTest
@Testcontainers
@Slf4j
public class GenericProtoApplicationTest {
  public static final int NUM_PARTITIONS = 2;
  public static final String TOPIC = "prod.readings";
  @Container public static KafkaContainer kafka = new KafkaContainerSBAware();

  @Configuration
  @Import({GenericProtoApplication.class, MockRegistryBeans.class})
  public static class Beans {
    @Bean
    NewTopic testTopic() {
      return new NewTopic(TOPIC, NUM_PARTITIONS, (short) 1);
    }
  }

  @Autowired private KafkaListenerEndpointRegistry registry;
  @Autowired private KafkaTemplate<String, Message> producer;

  @Test
  void contextLoads() {
    waitForAssignment();
  }

  @Test
  void sendDummyEvents() {
    waitForAssignment();
    IntStream.range(0, 50).forEach(this::sendDummyEvent);
  }

  private void sendDummyEvent(int i) {
    try {
      producer.send(TOPIC, getEvent(i)).get();
      Thread.sleep(1000);
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  private Message getEvent(int i) {
    final var reading =
        EnvironmentReadings.newBuilder()
            .setReadingId(generateID())
            .setDeviceId(generateID())
            .setLongitude(Math.random() * 70)
            .setLatitude(Math.random() * 70)
            .setTimeOfReading(fromMillis(currentTimeMillis()))
            .setElevationInMeters(random().nextInt(-500, 3000));

    int readingSelection = i % (EnvironmentReadings.ReadingsCase.values().length - 1);
    switch (readingSelection) {
      case 0:
        reading.setTemperatureReading(
            TempartureReading.newBuilder()
                .setTemperatureInCelsius(random().nextInt(-50, 50))
                .build());
        break;
      case 1:
        reading
            .setPrecipitationReading(
                PrecipitationReading.newBuilder().setDeltaInMillimetres(random().nextInt(0, 100)))
            .build();
        break;
      case 2:
        reading.setPhReading(
            PHReading.newBuilder()
                .setPhValue(random().nextInt(0, 15))
                .setPhType(PHType.forNumber(random().nextInt(1, 4)))
                .build());
        break;
      default:
        throw new RuntimeException();
    }

    return reading.build();
  }

  private ThreadLocalRandom random() {
    return ThreadLocalRandom.current();
  }

  private String generateID() {
    return UUID.randomUUID().toString();
  }

  private void waitForAssignment() {
    registry
        .getListenerContainers()
        .forEach(container -> ContainerTestUtils.waitForAssignment(container, NUM_PARTITIONS));
  }
}
