package com.zenin.genericproto.config;

import com.google.protobuf.util.JsonFormat;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProtobufConfig {

  @Bean
  public JsonFormat.Printer printer() {
    return JsonFormat.printer().preservingProtoFieldNames().omittingInsignificantWhitespace();
  }
}
