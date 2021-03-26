package org.pharosnet.postgres.channel.service;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.ServiceReference;
import lombok.extern.slf4j.Slf4j;
import org.pharosnet.postgres.channel.context.Context;
import org.pharosnet.postgres.channel.database.Databases;

import java.util.Optional;

@Slf4j
public class AbstractServiceImpl {

    private static final String host_key = "_postgres_channel_hosts";

    public AbstractServiceImpl(Vertx vertx) {
        this.vertx = vertx;
        this.databases = Databases.get(vertx);
        this.discovery = vertx.getOrCreateContext().get("_discovery");
    }

    private final Vertx vertx;
    private final Databases databases;
    private final ServiceDiscovery discovery;

    protected final String getHostId() {
        return vertx.getOrCreateContext().get("_discovery_id");
    }

    protected Future<Boolean> isLocal(String txTd) {
        Promise<Boolean> promise = Promise.promise();
        this.fetchHostRecord(txTd)
                .onSuccess(hostId -> {
                    if (hostId.isEmpty()) {
                        promise.fail("transaction id is lost");
                        return;
                    }
                    promise.complete(hostId.get().equals(this.getHostId()));
                })
                .onFailure(promise::fail);
        return promise.future();
    }

    protected Future<Void> putTransactionId(String txId) {
        return this.vertx.sharedData().getAsyncMap(host_key)
                .compose(map -> map.put(txId, this.getHostId()));
    }

    private Future<Optional<String>> getHostIdByTransactionId(String txId) {
        Promise<Optional<String>> promise = Promise.promise();
        this.vertx.sharedData().getAsyncMap(host_key)
                .compose(map -> map.get(txId))
                .onSuccess(r -> {
                    if (log.isDebugEnabled()) {
                        log.error("get host id {} by transaction id {}", r, txId);
                    }
                    if (r == null) {
                        promise.complete(Optional.empty());
                        return;
                    }
                    if (r instanceof String) {
                        String hostId = (String) r;
                        if (hostId.isBlank()) {
                            promise.complete(Optional.empty());
                            return;
                        }
                        promise.complete(Optional.of(hostId));
                        return;
                    }
                    promise.fail(String.format("host id of transaction id %s is not string type", txId));
                })
                .onFailure(e -> {
                    log.error("get host id by transaction id {} is failed", txId);
                    promise.complete(Optional.empty());
                });
        return promise.future();
    }

    private Future<Optional<Record>> fetchHostRecord(String hostId) {
        Promise<Optional<Record>> promise = Promise.promise();
        if (this.discovery == null) {
            promise.complete(Optional.empty());
            return promise.future();
        }
        this.discovery.getRecord(r -> r.getRegistration().equals(this.getHostId()))
                .onSuccess(record -> {
                    if (record == null) {
                        promise.complete(Optional.empty());
                        return;
                    }
                    promise.complete(Optional.of(record));
                })
                .onFailure(e -> {
                    log.error("get host({}) record failed", hostId);
                    promise.complete(Optional.empty());
                });
        return promise.future();
    }

    protected Future<JsonArray> remoteInvoke(String remoteHostId, Context context, QueryForm form) {
        Promise<JsonArray> promise = Promise.promise();
        this.fetchHostRecord(remoteHostId)
                .onSuccess(record -> {
                    if (record.isEmpty()) {
                        promise.fail("transaction is lost");
                        return;
                    }
                    ServiceReference reference = discovery.getReference(record.get());
                    HttpClient client = reference.getAs(HttpClient.class);
                    client.request(HttpMethod.POST, "query")
                            .compose(request -> {
                                return request.putHeader("x-request-id", context.getId())
                                        .putHeader("Authorization", context.getAuthorizationToken())
                                        .send(form.toJson().encode());
                            })
                            .onSuccess(rr -> {
                                rr.body()
                                        .compose(buf -> Future.succeededFuture(new JsonArray(buf)))
                                        .onSuccess(promise::complete)
                                        .onFailure(promise::fail);
                                reference.release();
                            })
                            .onFailure(promise::fail);

                })
                .onFailure(promise::fail);
        return promise.future();
    }

}
