package org.pharosnet.postgres.channel.service;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class QueryForm {

    public QueryForm() {
    }

    public QueryForm(JsonObject jsonObject) {
        QueryFormConverter.fromJson(jsonObject, this);
    }

    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        QueryFormConverter.toJson(this, jsonObject);
        return jsonObject;
    }


    private String query;
    private JsonArray args;
    private Boolean slaverMode;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public JsonArray getArgs() {
        return args;
    }

    public void setArgs(JsonArray args) {
        this.args = args;
    }

    public Boolean getSlaverMode() {
        return slaverMode;
    }

    public void setSlaverMode(Boolean slaverMode) {
        this.slaverMode = slaverMode;
    }

}
