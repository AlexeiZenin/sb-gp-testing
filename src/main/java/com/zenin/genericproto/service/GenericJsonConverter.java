package com.zenin.genericproto.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.zenin.genericproto.service.enhancers.ITimestampEnhancer;
import org.springframework.stereotype.Service;

@Service
public class GenericJsonConverter {
  private final JsonFormat.Printer jsonMapper;
  private final Gson gson;
  private final ITimestampEnhancer timestampEnhancer;

  public GenericJsonConverter(
      JsonFormat.Printer jsonMapper,
      GsonBuilder gsonBuilder,
      ITimestampEnhancer timestampEnhancer) {
    this.jsonMapper = jsonMapper;
    this.gson = gsonBuilder.create();
    this.timestampEnhancer = timestampEnhancer;
  }

  public JsonObject toJson(DynamicMessage event) {
    final String json = convertEventToJson(event);
    return timestampEnhancer.enhanceTimestamps(gson.fromJson(json, JsonObject.class), event);
  }

  private String convertEventToJson(DynamicMessage event) {
    String json;
    try {
      json = jsonMapper.print(event);
    } catch (InvalidProtocolBufferException e) {
      throw new RuntimeException("Failed to convert event to JSON", e);
    }
    return json;
  }
}
