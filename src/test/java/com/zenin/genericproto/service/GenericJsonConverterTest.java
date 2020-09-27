package com.zenin.genericproto.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.util.Timestamps;
import com.zenin.genericproto.service.enhancers.GenericTools;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.zenin.models.NestedTimestampOuterClass.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class GenericJsonConverterTest {

  private GenericTools genericJsonConverter;

  @BeforeEach
  public void setup() {
    genericJsonConverter = new GenericTools();
  }

  @Test
  void getJsonElementAtPath_MutateSuccessful() {
    final var top = new JsonObject();
    final var aObject = new JsonObject();
    final var bObject = new JsonObject();
    bObject.addProperty("b", 123);
    aObject.add("a", bObject);

    top.add("test", aObject);

    final var entry = genericJsonConverter.getJsonElementEntry("test.a.b", top);
    assertTrue(entry.getValue().isJsonPrimitive());
    assertTrue(entry.getKey().equals("b"));
    assertTrue(entry.getValue().getAsInt() == 123);

    entry.setValue(new JsonPrimitive(57L));

    final var entryAfterMutation = genericJsonConverter.getJsonElementEntry("test.a.b", top);

    assertTrue(entryAfterMutation.getValue().isJsonPrimitive());
    assertTrue(entryAfterMutation.getKey().equals("b"));
    assertTrue(entryAfterMutation.getValue().getAsLong() == 57L);
  }

  @Test
  void getTimestamp() {
    final var topLevel = Timestamps.fromMillis(123);
    final var nested = Timestamps.fromMillis(459);
    final var oneof = Timestamps.fromMillis(899);
    NestedTimestamp nestedTimestamp =
        NestedTimestamp.newBuilder()
            .setTimeOfReading(topLevel)
            .setNested(Nested.newBuilder().setNestedTime(nested).build())
            .setTemperatureReading(TempartureReading.newBuilder().setNestedTime(oneof).build())
            .build();
    DynamicMessage msg = DynamicMessage.newBuilder(nestedTimestamp).build();

    final var topTime = genericJsonConverter.getTimestamp(msg, "time_of_reading");
    log.info(topTime.toString());
    assertTrue(topTime.equals(topTime));

    final var nestedTime = genericJsonConverter.getTimestamp(msg, "nested.nested_time");
    log.info(nestedTime.toString());
    assertTrue(nestedTime.equals(nested));

    final var oneofTime = genericJsonConverter.getTimestamp(msg, "temperature_reading.nested_time");
    log.info(oneofTime.toString());
    assertTrue(oneofTime.equals(oneof));
  }

  @Test
  void getTimestampPaths() {
    final var topLevel = Timestamps.fromMillis(123);
    final var nested = Timestamps.fromMillis(459);
    final var oneof = Timestamps.fromMillis(899);
    NestedTimestamp nestedTimestamp =
        NestedTimestamp.newBuilder()
            .setTimeOfReading(topLevel)
            .setNested(Nested.newBuilder().setNestedTime(nested).build())
            .setTemperatureReading(TempartureReading.newBuilder().setNestedTime(oneof).build())
            .build();
    DynamicMessage msg = DynamicMessage.newBuilder(nestedTimestamp).build();

    final var paths = genericJsonConverter.getTimestampPaths(msg.getAllFields().keySet(), "");
    log.info(paths.toString());
  }
}
