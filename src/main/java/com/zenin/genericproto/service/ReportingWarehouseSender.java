package com.zenin.genericproto.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ReportingWarehouseSender {
  private final Gson gson;

  public ReportingWarehouseSender(GsonBuilder gsonBuilder) {
    this.gson = gsonBuilder.setPrettyPrinting().create();
  }

  public void sendToWarehouse(JsonObject jsonObject) {
    // purely for demo purposes
    log.info("\n{}", gson.toJson(jsonObject));
  }
}
