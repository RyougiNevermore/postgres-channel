package org.pharosnet.postgres.channel.config.http;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.impl.JsonUtil;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Converter and mapper for {@link org.pharosnet.postgres.channel.config.http.DiscoveryConfig}.
 * NOTE: This class has been automatically generated from the {@link org.pharosnet.postgres.channel.config.http.DiscoveryConfig} original class using Vert.x codegen.
 */
public class DiscoveryConfigConverter {


  public static void fromJson(Iterable<java.util.Map.Entry<String, Object>> json, DiscoveryConfig obj) {
    for (java.util.Map.Entry<String, Object> member : json) {
      switch (member.getKey()) {
        case "consul":
          if (member.getValue() instanceof JsonObject) {
            obj.setConsul(new org.pharosnet.postgres.channel.config.http.DiscoveryConsulConfig((io.vertx.core.json.JsonObject)member.getValue()));
          }
          break;
        case "kind":
          if (member.getValue() instanceof String) {
            obj.setKind((String)member.getValue());
          }
          break;
      }
    }
  }

  public static void toJson(DiscoveryConfig obj, JsonObject json) {
    toJson(obj, json.getMap());
  }

  public static void toJson(DiscoveryConfig obj, java.util.Map<String, Object> json) {
    if (obj.getConsul() != null) {
      json.put("consul", obj.getConsul().toJson());
    }
    if (obj.getKind() != null) {
      json.put("kind", obj.getKind());
    }
  }
}
