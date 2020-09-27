package com.zenin.genericproto.service;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReportingWarehouseSenderTest {

  private ReportingWarehouseSender warehouseSender;

  @BeforeEach
  public void setup() {
    warehouseSender = new ReportingWarehouseSender(new GsonBuilder());
  }

  @Test
  void sendToWarehouse() {
    final var jsonObject = new JsonObject();
    jsonObject.addProperty("testkey", 123);
    warehouseSender.sendToWarehouse(jsonObject);
  }
}
