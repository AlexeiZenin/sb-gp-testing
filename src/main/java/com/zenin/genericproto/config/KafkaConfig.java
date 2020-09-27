package com.zenin.genericproto.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;

@Data
@Configuration
@Slf4j
@ConfigurationProperties(prefix = KafkaConfig.PREFIX)
@Validated
public class KafkaConfig implements InitializingBean {
  public static final String PREFIX = "zenin.kafka";
  @NotBlank private String topicPattern;

  @Override
  public void afterPropertiesSet() throws Exception {
    log.info(this.toString());
  }
}
