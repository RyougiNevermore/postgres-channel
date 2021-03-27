package org.pharosnet.postgres.channel.service;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.impl.JsonUtil;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Converter and mapper for {@link org.pharosnet.postgres.channel.service.TransactionForm}.
 * NOTE: This class has been automatically generated from the {@link org.pharosnet.postgres.channel.service.TransactionForm} original class using Vert.x codegen.
 */
public class TransactionFormConverter {


  public static void fromJson(Iterable<java.util.Map.Entry<String, Object>> json, TransactionForm obj) {
    for (java.util.Map.Entry<String, Object> member : json) {
      switch (member.getKey()) {
        case "command":
          if (member.getValue() instanceof String) {
            obj.setCommand((String)member.getValue());
          }
          break;
      }
    }
  }

  public static void toJson(TransactionForm obj, JsonObject json) {
    toJson(obj, json.getMap());
  }

  public static void toJson(TransactionForm obj, java.util.Map<String, Object> json) {
    if (obj.getCommand() != null) {
      json.put("command", obj.getCommand());
    }
  }
}
