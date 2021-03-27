package org.pharosnet.postgres.channel.service;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.impl.JsonUtil;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Converter and mapper for {@link org.pharosnet.postgres.channel.service.TransactionResult}.
 * NOTE: This class has been automatically generated from the {@link org.pharosnet.postgres.channel.service.TransactionResult} original class using Vert.x codegen.
 */
public class TransactionResultConverter {


  public static void fromJson(Iterable<java.util.Map.Entry<String, Object>> json, TransactionResult obj) {
    for (java.util.Map.Entry<String, Object> member : json) {
      switch (member.getKey()) {
        case "transactionId":
          if (member.getValue() instanceof String) {
            obj.setTransactionId((String)member.getValue());
          }
          break;
      }
    }
  }

  public static void toJson(TransactionResult obj, JsonObject json) {
    toJson(obj, json.getMap());
  }

  public static void toJson(TransactionResult obj, java.util.Map<String, Object> json) {
    if (obj.getTransactionId() != null) {
      json.put("transactionId", obj.getTransactionId());
    }
  }
}
