package com.zenin.genericproto;

import com.google.protobuf.Message;
import com.zenin.genericproto.config.KafkaConfig;
import com.zenin.genericproto.service.ReportingWarehouseSender;
import com.zenin.genericproto.test.KafkaContainerSBAware;
import com.zenin.genericproto.test.MockRegistryBeans;
import com.zenin.models.EnvironmentReadingsOuterClass.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import static com.google.protobuf.util.Timestamps.fromMillis;
import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest
@Testcontainers
@Slf4j
@ActiveProfiles("test")
public class TransientErrorTest {
  public static final int NUM_PARTITIONS = 3;
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

  @Autowired KafkaListenerEndpointRegistry registry;
  @Autowired KafkaTemplate<String, Message> producer;
  @MockBean ReportingWarehouseSender mockedSender;
  @Autowired KafkaConfig kafkaConfig;
  @Autowired KafkaProperties kafkaProperties;

  @Test
  void contextLoads() {
    waitForAssignment();
  }

  @Test
  void sendDummyEvents_Expect3TransientErrors_ProcessEventSuccess() {
    waitForAssignment();

    // setup stubs
    final int NUM_ERRORS = 3;
    final var NUM_MSGS = 3;
    final CountDownLatch successLatch =
        makeSenderThrowException(
            NUM_ERRORS,
            mockedSender,
            NUM_MSGS,
            new RuntimeException("HTTP 503 SERVICE_UNAVAILABLE"));

    IntStream.range(0, NUM_MSGS).forEach(this::sendSomeDummyEvent);

    assertTrue(isLatchDone(NUM_ERRORS, successLatch));
  }

  /** Bonus :), check skip logic works if exception non-retriable */
  @Test
  @Disabled
  void sendDummyEvents_ExpectFatalError_SkipEvent() {
    waitForAssignment();

    // setup stubs
    final int NUM_ERRORS = 1;
    final var NUM_MSGS_TOTAL = 5;
    final var NUM_MSGS_PROCESSED =
        NUM_MSGS_TOTAL - (NUM_ERRORS * kafkaProperties.getListener().getConcurrency());
    final CountDownLatch successLatch =
        makeSenderThrowException(
            NUM_ERRORS, mockedSender, NUM_MSGS_PROCESSED, new ClassCastException(""));

    IntStream.range(0, NUM_MSGS_TOTAL).forEach(this::sendSomeDummyEvent);

    assertTrue(isLatchDone(0, successLatch));
  }

  private boolean isLatchDone(int numOfRetries, CountDownLatch successLatch) {
    try {
      final long GRACE_TIME_MILLIS = 1000L;
      return successLatch.await(
          kafkaConfig.getRetryIntervalMillis() * numOfRetries + GRACE_TIME_MILLIS, MILLISECONDS);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private CountDownLatch makeSenderThrowException(
      final int numThrowsPerThread,
      ReportingWarehouseSender mockSender,
      int expectedNumberOfMessages,
      Exception exceptionToThrow) {
    var successMsgLatch = new CountDownLatch(expectedNumberOfMessages);
    doAnswer(
            new Answer<Void>() {
              ThreadLocal<Integer> throwCount = ThreadLocal.withInitial(() -> 0);

              @Override
              public Void answer(InvocationOnMock invocationOnMock) throws Exception {
                throwCount.set(throwCount.get() + 1);
                if (throwCount.get() <= numThrowsPerThread) {
                  throw exceptionToThrow;
                } else {
                  log.info(
                      "Success in sending to warehouse: [{}]",
                      invocationOnMock.getArgument(0).toString());
                  successMsgLatch.countDown();
                  return null;
                }
              }
            })
        .when(mockSender)
        .sendToWarehouse(any());

    return successMsgLatch;
  }

  private void sendSomeDummyEvent(int i) {
    try {
      producer.send(TOPIC, i % NUM_PARTITIONS, null, getSomeEvent(i)).get();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  private Message getSomeEvent(int i) {
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
