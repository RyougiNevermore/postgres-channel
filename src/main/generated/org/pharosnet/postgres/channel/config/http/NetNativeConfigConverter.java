package org.pharosnet.postgres.channel.config.http;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.impl.JsonUtil;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Converter and mapper for {@link org.pharosnet.postgres.channel.config.http.NetNativeConfig}.
 * NOTE: This class has been automatically generated from the {@link org.pharosnet.postgres.channel.config.http.NetNativeConfig} original class using Vert.x codegen.
 */
public class NetNativeConfigConverter {


  public static void fromJson(Iterable<java.util.Map.Entry<String, Object>> json, NetNativeConfig obj) {
    for (java.util.Map.Entry<String, Object> member : json) {
      switch (member.getKey()) {
        case "reusePort":
          if (member.getValue() instanceof Boolean) {
            obj.setReusePort((Boolean)member.getValue());
          }
          break;
        case "tcpCork":
          if (member.getValue() instanceof Boolean) {
            obj.setTcpCork((Boolean)member.getValue());
          }
          break;
        case "tcpFastOpen":
          if (member.getValue() instanceof Boolean) {
            obj.setTcpFastOpen((Boolean)member.getValue());
          }
          break;
        case "tcpQuickAck":
          if (member.getValue() instanceof Boolean) {
            obj.setTcpQuickAck((Boolean)member.getValue());
          }
          break;
      }
    }
  }

  public static void toJson(NetNativeConfig obj, JsonObject json) {
    toJson(obj, json.getMap());
  }

  public static void toJson(NetNativeConfig obj, java.util.Map<String, Object> json) {
    if (obj.getReusePort() != null) {
      json.put("reusePort", obj.getReusePort());
    }
    if (obj.getTcpCork() != null) {
      json.put("tcpCork", obj.getTcpCork());
    }
    if (obj.getTcpFastOpen() != null) {
      json.put("tcpFastOpen", obj.getTcpFastOpen());
    }
    if (obj.getTcpQuickAck() != null) {
      json.put("tcpQuickAck", obj.getTcpQuickAck());
    }
  }
}
