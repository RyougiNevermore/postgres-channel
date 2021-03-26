package org.pharosnet.postgres.channel.config.postgres;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

import java.util.List;

@DataObject(generateConverter = true)
public class PostgresConfig {

    public PostgresConfig() {
    }

    public PostgresConfig(JsonObject jsonObject) {
        PostgresConfigConverter.fromJson(jsonObject, this);
    }

    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        PostgresConfigConverter.toJson(this, jsonObject);
        return jsonObject;
    }

    // standalone, master_slaver, xl
    private String kind;

    // [0] = standalone or master, [1,n] = slavers, [0, n] = xl
    private List<NodeConfig> nodes;

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public List<NodeConfig> getNodes() {
        return nodes;
    }

    public void setNodes(List<NodeConfig> nodes) {
        this.nodes = nodes;
    }

}
