package com.zenin.genericproto.service.enhancers;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Timestamp;

import java.util.Set;

public class UnixEnhancer implements ITimestampEnhancer {
  private final GenericTools genericTools;

  public UnixEnhancer(GenericTools genericTools) {
    this.genericTools = genericTools;
  }

  @Override
  public JsonObject enhanceTimestamps(JsonObject jsonObject, DynamicMessage dynamicMessage) {
    Set<String> timestampLocations =
        genericTools.getTimestampPaths(dynamicMessage.getAllFields().keySet(), "");
    timestampLocations.forEach(location -> setUnixTime(jsonObject, location, dynamicMessage));
    return jsonObject;
  }

  void setUnixTime(JsonObject jsonObject, String timestampLocation, DynamicMessage event) {
    final var entry = genericTools.getJsonElementEntry(timestampLocation, jsonObject);
    final var timestamp = genericTools.getTimestamp(event, timestampLocation);
    entry.setValue(new JsonPrimitive(timestamp.getSeconds()));
  }
}
