package com.zenin.genericproto.service.enhancers;

import com.google.gson.JsonObject;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Timestamp;

import java.util.Set;

public class ProtoPreservingEnhancer implements ITimestampEnhancer {
  private final GenericTools genericTools;

  public ProtoPreservingEnhancer(GenericTools genericTools) {
    this.genericTools = genericTools;
  }

  @Override
  public JsonObject enhanceTimestamps(JsonObject jsonObject, DynamicMessage dynamicMessage) {
    Set<String> timestampLocations =
        genericTools.getTimestampPaths(dynamicMessage.getAllFields().keySet(), "");
    timestampLocations.forEach(
        location -> setProtoStructureTime(jsonObject, location, dynamicMessage));
    return jsonObject;
  }

  void setProtoStructureTime(
      final JsonObject jsonObject, final String timestampLocation, final DynamicMessage event) {
    final var entry = genericTools.getJsonElementEntry(timestampLocation, jsonObject);
    final var timestamp = genericTools.getTimestamp(event, timestampLocation);

    JsonObject jsonTimestamp = new JsonObject();
    jsonTimestamp.addProperty("seconds", timestamp.getSeconds());
    jsonTimestamp.addProperty("nanos", timestamp.getNanos());

    entry.setValue(jsonTimestamp);
  }
}
