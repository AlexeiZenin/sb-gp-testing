package com.zenin.genericproto.config;

import com.google.gson.GsonBuilder;
import com.zenin.genericproto.service.enhancers.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;

@Configuration
@ConfigurationProperties(prefix = JsonConfig.PREFIX)
@Data
@Slf4j
@Validated
public class JsonConfig implements InitializingBean {
  public static final String PREFIX = "zenin.json";
  public static final String TIMESTAMP_MODE = "timestampMode";
  @NotNull private TimestampMode timestampMode = TimestampMode.UNIX;

  @Bean
  public GsonBuilder gsonBuilder() {
    return new GsonBuilder();
  }

  @Bean
  @ConditionalOnProperty(prefix = PREFIX, name = TIMESTAMP_MODE, havingValue = "GOOGLE")
  public ITimestampEnhancer googleStrategy() {
    return new NoOpEnhancer();
  }

  @Bean
  @ConditionalOnProperty(prefix = PREFIX, name = TIMESTAMP_MODE, havingValue = "UNIX")
  public ITimestampEnhancer unixStrategy(GenericTools genericTools) {
    return new UnixEnhancer(genericTools);
  }

  @Bean
  @ConditionalOnProperty(prefix = PREFIX, name = TIMESTAMP_MODE, havingValue = "PRESERVE_PROTO")
  public ITimestampEnhancer protoPreservingStrategy(GenericTools genericTools) {
    return new ProtoPreservingEnhancer(genericTools);
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    log.info(this.toString());
  }

  /** Options for how the processor should enhance the JSON conversion from Protobuf */
  public enum TimestampMode {
    /**
     * Uses the default {@link com.google.protobuf.util.JsonFormat.Printer} output for {@link
     * com.google.protobuf.Timestamp}.
     */
    GOOGLE,
    /**
     * Sets all {@link com.google.protobuf.Timestamp Timestamps} to be serialized as UNIX timestamps
     */
    UNIX,
    /**
     * Keeps the proto structure of the protobuf {@link com.google.protobuf.Timestamp} object,
     * printing seconds and nanos
     */
    PRESERVE_PROTO
  }
}
