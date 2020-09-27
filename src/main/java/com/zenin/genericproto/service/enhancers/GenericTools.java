package com.zenin.genericproto.service.enhancers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Timestamp;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.google.protobuf.Descriptors.FieldDescriptor.Type;

@Service
public class GenericTools {

  public Map.Entry<String, JsonElement> getJsonElementEntry(
      final String path, final JsonObject jsonObject) {
    String[] split = path.split("\\.");
    if (split.length == 0) {
      split = new String[] {path};
    }
    JsonObject intermediate = jsonObject;
    Map.Entry<String, JsonElement> res = null;
    for (int i = 0; i < split.length; i++) {
      final var memberName = split[i];
      if (i == split.length - 1) {
        res =
            intermediate.entrySet().stream()
                .filter(entry -> entry.getKey().equals(memberName))
                .findFirst()
                .get();
        break;
      }
      intermediate = intermediate.getAsJsonObject(memberName);
    }

    return res;
  }

  public Timestamp getTimestamp(final DynamicMessage event, final String path) {
    String[] split = path.split("\\.");
    if (split.length == 0) {
      split = new String[] {path};
    }

    Timestamp res = null;
    DynamicMessage intermediate = event;
    for (int i = 0; i < split.length; i++) {
      intermediate =
          (DynamicMessage)
              intermediate.getField(intermediate.getDescriptorForType().findFieldByName(split[i]));
      if (i == split.length - 1) {
        res = parseToTimestamp(intermediate);
        break;
      }
    }

    return res;
  }

  public Timestamp parseToTimestamp(DynamicMessage timestampDynamic) {
    Timestamp res;
    try {
      res = Timestamp.parseFrom(timestampDynamic.toByteArray());
    } catch (InvalidProtocolBufferException e) {
      throw new RuntimeException(e);
    }
    return res;
  }

  public Set<String> getTimestampPaths(final Set<FieldDescriptor> schema, final String basePath) {
    Set<String> paths = new HashSet<String>();

    for (FieldDescriptor fieldDescriptor : schema) {
      final var type = fieldDescriptor.getType();
      if (fieldDescriptor.getType().equals(Type.MESSAGE)
          && fieldDescriptor
              .getMessageType()
              .getFullName()
              .equals(Timestamp.getDescriptor().getFullName())) {
        paths.add(
            basePath.equals("")
                ? fieldDescriptor.getName()
                : basePath + "." + fieldDescriptor.getName());
      } else {
        // continue the DFS
        if (type.equals(Type.MESSAGE)) {
          paths.addAll(
              getTimestampPaths(
                  new HashSet<>(fieldDescriptor.getMessageType().getFields()),
                  basePath.equals("")
                      ? fieldDescriptor.getName()
                      : basePath + "." + fieldDescriptor.getName()));
        }
      }
    }

    return paths;
  }
}
