package com.zenin.genericproto.test;

import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.KafkaContainer;

@Slf4j
public class KafkaContainerSBAware extends KafkaContainer {

  @Override
  public void start() {
    super.start();
    log.info("Setting bootstrap servers to: {}", getBootstrapServers());
    System.setProperty("spring.kafka.bootstrap-servers", getBootstrapServers());
  }
}
