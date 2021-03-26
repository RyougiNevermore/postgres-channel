package org.pharosnet.postgres.channel.config.security;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.impl.JsonUtil;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Converter and mapper for {@link org.pharosnet.postgres.channel.config.security.HtpasswdSecurityConfig}.
 * NOTE: This class has been automatically generated from the {@link org.pharosnet.postgres.channel.config.security.HtpasswdSecurityConfig} original class using Vert.x codegen.
 */
public class HtpasswdSecurityConfigConverter {


  public static void fromJson(Iterable<java.util.Map.Entry<String, Object>> json, HtpasswdSecurityConfig obj) {
    for (java.util.Map.Entry<String, Object> member : json) {
      switch (member.getKey()) {
        case "password":
          if (member.getValue() instanceof String) {
            obj.setPassword((String)member.getValue());
          }
          break;
        case "username":
          if (member.getValue() instanceof String) {
            obj.setUsername((String)member.getValue());
          }
          break;
      }
    }
  }

  public static void toJson(HtpasswdSecurityConfig obj, JsonObject json) {
    toJson(obj, json.getMap());
  }

  public static void toJson(HtpasswdSecurityConfig obj, java.util.Map<String, Object> json) {
    if (obj.getPassword() != null) {
      json.put("password", obj.getPassword());
    }
    if (obj.getUsername() != null) {
      json.put("username", obj.getUsername());
    }
  }
}
