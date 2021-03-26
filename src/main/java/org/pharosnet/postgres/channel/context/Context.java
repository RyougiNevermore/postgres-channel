package org.pharosnet.postgres.channel.context;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

@DataObject(generateConverter = true)
public class Context {

    protected static final String context_key = "_context";

    public static ContextHandler create() {
        return new ContextHandler();
    }

    public static Context get(RoutingContext routingContext) {
        return (Context) routingContext.data().get(context_key);
    }

    public Context() {
    }

    public Context(JsonObject jsonObject) {
        ContextConverter.fromJson(jsonObject, this);
    }

    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        ContextConverter.toJson(this, jsonObject);
        return jsonObject;
    }

    private String id;
    private JsonObject data;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public JsonObject getData() {
        return data;
    }

    public void setData(JsonObject data) {
        this.data = data;
    }

    public String getAuthorizationToken() {
        return this.data.getString("Authorization");
    }

    public void setAuthorizationToken(String value) {
        this.data.put("Authorization", value);
    }

}
