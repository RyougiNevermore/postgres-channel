package org.pharosnet.postgres.channel.context;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.impl.JsonUtil;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Converter and mapper for {@link org.pharosnet.postgres.channel.context.Context}.
 * NOTE: This class has been automatically generated from the {@link org.pharosnet.postgres.channel.context.Context} original class using Vert.x codegen.
 */
public class ContextConverter {


  public static void fromJson(Iterable<java.util.Map.Entry<String, Object>> json, Context obj) {
    for (java.util.Map.Entry<String, Object> member : json) {
      switch (member.getKey()) {
        case "authorizationToken":
          if (member.getValue() instanceof String) {
            obj.setAuthorizationToken((String)member.getValue());
          }
          break;
        case "data":
          if (member.getValue() instanceof JsonObject) {
            obj.setData(((JsonObject)member.getValue()).copy());
          }
          break;
        case "id":
          if (member.getValue() instanceof String) {
            obj.setId((String)member.getValue());
          }
          break;
      }
    }
  }

  public static void toJson(Context obj, JsonObject json) {
    toJson(obj, json.getMap());
  }

  public static void toJson(Context obj, java.util.Map<String, Object> json) {
    if (obj.getAuthorizationToken() != null) {
      json.put("authorizationToken", obj.getAuthorizationToken());
    }
    if (obj.getData() != null) {
      json.put("data", obj.getData());
    }
    if (obj.getId() != null) {
      json.put("id", obj.getId());
    }
  }
}
