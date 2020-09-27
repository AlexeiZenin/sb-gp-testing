package com.zenin.genericproto.test;

import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Message;
import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializer;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.kafka.DefaultKafkaConsumerFactoryCustomizer;
import org.springframework.boot.autoconfigure.kafka.DefaultKafkaProducerFactoryCustomizer;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.ProducerFactory;

import java.util.Map;

@TestConfiguration
@Slf4j
public class MockRegistryBeans {
  private final KafkaAutoConfiguration kafkaAutoConfiguration;

  public MockRegistryBeans(KafkaAutoConfiguration kafkaAutoConfiguration) {
    this.kafkaAutoConfiguration = kafkaAutoConfiguration;
  }

  @Bean
  SchemaRegistryClient mockRegistry() {
    return new MockSchemaRegistryClient();
  }

  @Bean
  DefaultKafkaConsumerFactoryCustomizer mockedConsumerFactory() {
    return consumerFactory -> {
      var typedFactory = (DefaultKafkaConsumerFactory<String, DynamicMessage>) consumerFactory;
      final var valueDeserializer = new KafkaProtobufDeserializer<DynamicMessage>(mockRegistry());
      valueDeserializer.configure(Map.of("schema.registry.url", "http://mock.com:8081"), false);
      typedFactory.setValueDeserializer(valueDeserializer);
      typedFactory.setKeyDeserializer(new StringDeserializer());
    };
  }

  @Bean
  DefaultKafkaProducerFactoryCustomizer mockedProducerFactory() {
    return producerFactory -> {
      final var typedFactory = (DefaultKafkaProducerFactory<String, Message>) producerFactory;
      final var valueSerializer = new KafkaProtobufSerializer<>(mockRegistry());
      valueSerializer.configure(Map.of("schema.registry.url", "http://mock.com:8081"), false);
      typedFactory.setValueSerializer(valueSerializer);
      typedFactory.setKeySerializer(new StringSerializer());
    };
  }
}
