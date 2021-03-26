package org.pharosnet.postgres.channel.config.http;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.impl.JsonUtil;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Converter and mapper for {@link org.pharosnet.postgres.channel.config.http.HttpConfig}.
 * NOTE: This class has been automatically generated from the {@link org.pharosnet.postgres.channel.config.http.HttpConfig} original class using Vert.x codegen.
 */
public class HttpConfigConverter {


  public static void fromJson(Iterable<java.util.Map.Entry<String, Object>> json, HttpConfig obj) {
    for (java.util.Map.Entry<String, Object> member : json) {
      switch (member.getKey()) {
        case "backlog":
          if (member.getValue() instanceof Number) {
            obj.setBacklog(((Number)member.getValue()).intValue());
          }
          break;
        case "compress":
          if (member.getValue() instanceof JsonObject) {
            obj.setCompress(new org.pharosnet.postgres.channel.config.http.CompressConfig((io.vertx.core.json.JsonObject)member.getValue()));
          }
          break;
        case "discovery":
          if (member.getValue() instanceof JsonObject) {
            obj.setDiscovery(new org.pharosnet.postgres.channel.config.http.DiscoveryConfig((io.vertx.core.json.JsonObject)member.getValue()));
          }
          break;
        case "enableLogActivity":
          if (member.getValue() instanceof Boolean) {
            obj.setEnableLogActivity((Boolean)member.getValue());
          }
          break;
        case "host":
          if (member.getValue() instanceof String) {
            obj.setHost((String)member.getValue());
          }
          break;
        case "htpasswdFile":
          if (member.getValue() instanceof String) {
            obj.setHtpasswdFile((String)member.getValue());
          }
          break;
        case "netNative":
          if (member.getValue() instanceof JsonObject) {
            obj.setNetNative(new org.pharosnet.postgres.channel.config.http.NetNativeConfig((io.vertx.core.json.JsonObject)member.getValue()));
          }
          break;
        case "port":
          if (member.getValue() instanceof Number) {
            obj.setPort(((Number)member.getValue()).intValue());
          }
          break;
        case "ssl":
          if (member.getValue() instanceof JsonObject) {
            obj.setSsl(new org.pharosnet.postgres.channel.config.http.SSLConfig((io.vertx.core.json.JsonObject)member.getValue()));
          }
          break;
      }
    }
  }

  public static void toJson(HttpConfig obj, JsonObject json) {
    toJson(obj, json.getMap());
  }

  public static void toJson(HttpConfig obj, java.util.Map<String, Object> json) {
    if (obj.getBacklog() != null) {
      json.put("backlog", obj.getBacklog());
    }
    if (obj.getCompress() != null) {
      json.put("compress", obj.getCompress().toJson());
    }
    if (obj.getDiscovery() != null) {
      json.put("discovery", obj.getDiscovery().toJson());
    }
    if (obj.getEnableLogActivity() != null) {
      json.put("enableLogActivity", obj.getEnableLogActivity());
    }
    if (obj.getHost() != null) {
      json.put("host", obj.getHost());
    }
    if (obj.getHtpasswdFile() != null) {
      json.put("htpasswdFile", obj.getHtpasswdFile());
    }
    if (obj.getNetNative() != null) {
      json.put("netNative", obj.getNetNative().toJson());
    }
    if (obj.getPort() != null) {
      json.put("port", obj.getPort());
    }
    if (obj.getSsl() != null) {
      json.put("ssl", obj.getSsl().toJson());
    }
  }
}
