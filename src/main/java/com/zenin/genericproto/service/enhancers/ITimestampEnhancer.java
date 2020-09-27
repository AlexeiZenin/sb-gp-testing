package com.zenin.genericproto.service.enhancers;

import com.google.gson.JsonObject;
import com.google.protobuf.DynamicMessage;

public interface ITimestampEnhancer {
  JsonObject enhanceTimestamps(JsonObject jsonObject, DynamicMessage dynamicMessage);
}
