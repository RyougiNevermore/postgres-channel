package org.pharosnet.postgres.channel.service;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class TransactionForm {

    public TransactionForm() {
    }

    public TransactionForm(JsonObject jsonObject) {
        TransactionFormConverter.fromJson(jsonObject, this);
    }

    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        TransactionFormConverter.toJson(this, jsonObject);
        return jsonObject;
    }


    private String command;

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

}
