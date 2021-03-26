package org.pharosnet.postgres.channel.config.http;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class DiscoveryConsulConfig {

    public DiscoveryConsulConfig() {
    }

    public DiscoveryConsulConfig(JsonObject jsonObject) {
        DiscoveryConsulConfigConverter.fromJson(jsonObject, this);
    }

    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        DiscoveryConsulConfigConverter.toJson(this, jsonObject);
        return jsonObject;
    }


    private String host;
    private Integer port;
    private String aclToken;
    private Integer scanPeriod;


    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getAclToken() {
        return aclToken;
    }

    public void setAclToken(String aclToken) {
        this.aclToken = aclToken;
    }

    public Integer getScanPeriod() {
        return scanPeriod;
    }

    public void setScanPeriod(Integer scanPeriod) {
        this.scanPeriod = scanPeriod;
    }

}
