package org.pharosnet.postgres.channel.config.security;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class HtpasswdSecurityConfig {

    public HtpasswdSecurityConfig() {
    }

    public HtpasswdSecurityConfig(JsonObject jsonObject) {
        HtpasswdSecurityConfigConverter.fromJson(jsonObject, this);
    }

    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        HtpasswdSecurityConfigConverter.toJson(this, jsonObject);
        return jsonObject;
    }

    private String username;
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
