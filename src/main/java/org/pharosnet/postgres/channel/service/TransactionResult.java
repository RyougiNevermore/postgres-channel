package org.pharosnet.postgres.channel.service;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject(generateConverter = true)
public class TransactionResult {

    public TransactionResult() {
    }

    public TransactionResult(JsonObject jsonObject) {
        TransactionResultConverter.fromJson(jsonObject, this);
    }

    public TransactionResult(String transactionId) {
        this.transactionId = transactionId;
    }

    public JsonObject toJson() {
        JsonObject jsonObject = new JsonObject();
        TransactionResultConverter.toJson(this, jsonObject);
        return jsonObject;
    }

    private String transactionId;

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }
}
