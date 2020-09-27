package com.zenin.genericproto.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.SeekToCurrentErrorHandler;
import org.springframework.util.backoff.FixedBackOff;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

@Data
@Configuration
@Slf4j
@ConfigurationProperties(prefix = KafkaConfig.PREFIX)
@Validated
public class KafkaConfig implements InitializingBean {
  public static final String PREFIX = "zenin.kafka";
  @NotBlank private String topicPattern;

  @Min(0)
  private long maxRetries = FixedBackOff.UNLIMITED_ATTEMPTS;

  @Min(0)
  private long retryIntervalMillis = 1000L;

  @Bean
  public SeekToCurrentErrorHandler seekToCurrentErrorHandler() {
    return new SeekToCurrentErrorHandler(new FixedBackOff(retryIntervalMillis, maxRetries));
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    log.info(this.toString());
  }
}
