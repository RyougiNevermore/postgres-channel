package org.pharosnet.postgres.channel.config.postgres;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.impl.JsonUtil;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Converter and mapper for {@link org.pharosnet.postgres.channel.config.postgres.NodeConfig}.
 * NOTE: This class has been automatically generated from the {@link org.pharosnet.postgres.channel.config.postgres.NodeConfig} original class using Vert.x codegen.
 */
public class NodeConfigConverter {


  public static void fromJson(Iterable<java.util.Map.Entry<String, Object>> json, NodeConfig obj) {
    for (java.util.Map.Entry<String, Object> member : json) {
      switch (member.getKey()) {
        case "database":
          if (member.getValue() instanceof String) {
            obj.setDatabase((String)member.getValue());
          }
          break;
        case "host":
          if (member.getValue() instanceof String) {
            obj.setHost((String)member.getValue());
          }
          break;
        case "maxPoolSize":
          if (member.getValue() instanceof Number) {
            obj.setMaxPoolSize(((Number)member.getValue()).intValue());
          }
          break;
        case "password":
          if (member.getValue() instanceof String) {
            obj.setPassword((String)member.getValue());
          }
          break;
        case "port":
          if (member.getValue() instanceof Number) {
            obj.setPort(((Number)member.getValue()).intValue());
          }
          break;
        case "ssl":
          if (member.getValue() instanceof Boolean) {
            obj.setSsl((Boolean)member.getValue());
          }
          break;
        case "sslCertPath":
          if (member.getValue() instanceof String) {
            obj.setSslCertPath((String)member.getValue());
          }
          break;
        case "sslMode":
          if (member.getValue() instanceof String) {
            obj.setSslMode((String)member.getValue());
          }
          break;
        case "user":
          if (member.getValue() instanceof String) {
            obj.setUser((String)member.getValue());
          }
          break;
      }
    }
  }

  public static void toJson(NodeConfig obj, JsonObject json) {
    toJson(obj, json.getMap());
  }

  public static void toJson(NodeConfig obj, java.util.Map<String, Object> json) {
    if (obj.getDatabase() != null) {
      json.put("database", obj.getDatabase());
    }
    if (obj.getHost() != null) {
      json.put("host", obj.getHost());
    }
    if (obj.getMaxPoolSize() != null) {
      json.put("maxPoolSize", obj.getMaxPoolSize());
    }
    if (obj.getPassword() != null) {
      json.put("password", obj.getPassword());
    }
    if (obj.getPort() != null) {
      json.put("port", obj.getPort());
    }
    if (obj.getSsl() != null) {
      json.put("ssl", obj.getSsl());
    }
    if (obj.getSslCertPath() != null) {
      json.put("sslCertPath", obj.getSslCertPath());
    }
    if (obj.getSslMode() != null) {
      json.put("sslMode", obj.getSslMode());
    }
    if (obj.getUser() != null) {
      json.put("user", obj.getUser());
    }
  }
}
