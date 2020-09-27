package com.zenin.genericproto.service.enhancers;

import com.google.gson.JsonObject;
import com.google.protobuf.DynamicMessage;

public class NoOpEnhancer implements ITimestampEnhancer {
  @Override
  public JsonObject enhanceTimestamps(JsonObject jsonObject, DynamicMessage dynamicMessage) {
    return jsonObject;
  }
}
