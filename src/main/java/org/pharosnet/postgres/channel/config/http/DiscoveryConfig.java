package org.pharosnet.postgres.channel.config.http;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class DiscoveryConfig {


    public DiscoveryConfig() {
    }

    public DiscoveryConfig(JsonObject jsonObject) {
        DiscoveryConfigConverter.fromJson(jsonObject, this);
    }

    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        DiscoveryConfigConverter.toJson(this, jsonObject);
        return jsonObject;
    }


    private String kind;

    private DiscoveryConsulConfig consul;


    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public DiscoveryConsulConfig getConsul() {
        return consul;
    }

    public void setConsul(DiscoveryConsulConfig consul) {
        this.consul = consul;
    }
}
