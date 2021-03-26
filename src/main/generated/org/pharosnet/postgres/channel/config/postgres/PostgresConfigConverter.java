package org.pharosnet.postgres.channel.config.postgres;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.impl.JsonUtil;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Converter and mapper for {@link org.pharosnet.postgres.channel.config.postgres.PostgresConfig}.
 * NOTE: This class has been automatically generated from the {@link org.pharosnet.postgres.channel.config.postgres.PostgresConfig} original class using Vert.x codegen.
 */
public class PostgresConfigConverter {


  public static void fromJson(Iterable<java.util.Map.Entry<String, Object>> json, PostgresConfig obj) {
    for (java.util.Map.Entry<String, Object> member : json) {
      switch (member.getKey()) {
        case "kind":
          if (member.getValue() instanceof String) {
            obj.setKind((String)member.getValue());
          }
          break;
        case "nodes":
          if (member.getValue() instanceof JsonArray) {
            java.util.ArrayList<org.pharosnet.postgres.channel.config.postgres.NodeConfig> list =  new java.util.ArrayList<>();
            ((Iterable<Object>)member.getValue()).forEach( item -> {
              if (item instanceof JsonObject)
                list.add(new org.pharosnet.postgres.channel.config.postgres.NodeConfig((io.vertx.core.json.JsonObject)item));
            });
            obj.setNodes(list);
          }
          break;
      }
    }
  }

  public static void toJson(PostgresConfig obj, JsonObject json) {
    toJson(obj, json.getMap());
  }

  public static void toJson(PostgresConfig obj, java.util.Map<String, Object> json) {
    if (obj.getKind() != null) {
      json.put("kind", obj.getKind());
    }
    if (obj.getNodes() != null) {
      JsonArray array = new JsonArray();
      obj.getNodes().forEach(item -> array.add(item.toJson()));
      json.put("nodes", array);
    }
  }
}
