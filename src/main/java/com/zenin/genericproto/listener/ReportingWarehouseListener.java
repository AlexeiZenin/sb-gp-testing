package com.zenin.genericproto.listener;

import com.google.protobuf.DynamicMessage;
import com.zenin.genericproto.service.GenericJsonConverter;
import com.zenin.genericproto.service.ReportingWarehouseSender;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class ReportingWarehouseListener {
  private final GenericJsonConverter converter;
  private final ReportingWarehouseSender warehouseSender;

  public ReportingWarehouseListener(
      GenericJsonConverter converter, ReportingWarehouseSender warehouseSender) {
    this.converter = converter;
    this.warehouseSender = warehouseSender;
  }

  @KafkaListener(topicPattern = "#{kafkaConfig.getTopicPattern()}")
  public void processEvent(ConsumerRecord<String, DynamicMessage> kafkaRecord) {
    DynamicMessage event = kafkaRecord.value();
    warehouseSender.sendToWarehouse(converter.toJson(event));
  }
}
