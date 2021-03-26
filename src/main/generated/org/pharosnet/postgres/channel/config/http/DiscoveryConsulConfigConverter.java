package org.pharosnet.postgres.channel.config.http;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.impl.JsonUtil;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Converter and mapper for {@link org.pharosnet.postgres.channel.config.http.DiscoveryConsulConfig}.
 * NOTE: This class has been automatically generated from the {@link org.pharosnet.postgres.channel.config.http.DiscoveryConsulConfig} original class using Vert.x codegen.
 */
public class DiscoveryConsulConfigConverter {


  public static void fromJson(Iterable<java.util.Map.Entry<String, Object>> json, DiscoveryConsulConfig obj) {
    for (java.util.Map.Entry<String, Object> member : json) {
      switch (member.getKey()) {
        case "aclToken":
          if (member.getValue() instanceof String) {
            obj.setAclToken((String)member.getValue());
          }
          break;
        case "host":
          if (member.getValue() instanceof String) {
            obj.setHost((String)member.getValue());
          }
          break;
        case "port":
          if (member.getValue() instanceof Number) {
            obj.setPort(((Number)member.getValue()).intValue());
          }
          break;
        case "scanPeriod":
          if (member.getValue() instanceof Number) {
            obj.setScanPeriod(((Number)member.getValue()).intValue());
          }
          break;
      }
    }
  }

  public static void toJson(DiscoveryConsulConfig obj, JsonObject json) {
    toJson(obj, json.getMap());
  }

  public static void toJson(DiscoveryConsulConfig obj, java.util.Map<String, Object> json) {
    if (obj.getAclToken() != null) {
      json.put("aclToken", obj.getAclToken());
    }
    if (obj.getHost() != null) {
      json.put("host", obj.getHost());
    }
    if (obj.getPort() != null) {
      json.put("port", obj.getPort());
    }
    if (obj.getScanPeriod() != null) {
      json.put("scanPeriod", obj.getScanPeriod());
    }
  }
}
