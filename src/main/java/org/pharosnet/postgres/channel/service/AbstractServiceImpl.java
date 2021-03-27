package org.pharosnet.postgres.channel.service;

import com.github.benmanes.caffeine.cache.Cache;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.pharosnet.postgres.channel.context.Context;
import org.pharosnet.postgres.channel.database.CachedTransaction;
import org.pharosnet.postgres.channel.database.Databases;

import java.util.Optional;

@Slf4j
public class AbstractServiceImpl {

    private static final String host_key = "_postgres_channel_hosts";

    public AbstractServiceImpl(Vertx vertx) {
        this.vertx = vertx;
        this.databases = Databases.get(vertx);
        this.discovery = vertx.getOrCreateContext().get("_discovery");
        this.transactions = this.databases.getTransactions();
        this.transactionCachedTTL = vertx.getOrCreateContext().get("_cache_transaction_ttl");
    }

    private final Vertx vertx;
    private final Databases databases;
    private final ServiceDiscovery discovery;
    private final Cache<@NonNull String, @NonNull CachedTransaction> transactions;
    private final long transactionCachedTTL;

    public Vertx vertx() {
        return vertx;
    }

    public Databases databases() {
        return databases;
    }

    protected final String getHostId() {
        return vertx.getOrCreateContext().get("_discovery_id");
    }

    protected Future<Boolean> isLocal(Context context) {
        if (Optional.ofNullable(context.getId()).orElse("").trim().isBlank()) {
            return Future.succeededFuture(true);
        }
        Promise<Boolean> promise = Promise.promise();
        this.getHostId(context)
                .onSuccess(hostId -> {
                    if (hostId.isEmpty()) {
                        promise.complete(true);
                        return;
                    }
                    promise.complete(hostId.get().equals(this.getHostId()));
                })
                .onFailure(promise::fail);
        return promise.future();
    }

    protected Future<Void> putTransaction(String transactionId, CachedTransaction transaction) {
        return this.vertx.sharedData().getAsyncMap(host_key)
                .compose(map -> map.put(transactionId, this.getHostId(), this.transactionCachedTTL))
                .compose(r -> {
                    this.transactions.put(transactionId, transaction);
                    return Future.succeededFuture();
                });
    }

    protected Future<Optional<CachedTransaction>> getTransaction(Context context) {
        try {
            CachedTransaction transaction = this.transactions.getIfPresent(context.getId());
            return Future.succeededFuture(Optional.ofNullable(transaction));
        } catch (Exception e) {
            if (log.isWarnEnabled()) {
                log.warn("get transaction failed by id({})", context.getId());
            }
            return Future.succeededFuture(Optional.empty());
        }
    }

    protected Future<Void> releaseTransaction(String transactionId) {
        return this.vertx.sharedData().getAsyncMap(host_key)
                .compose(map -> {
                    map.remove(transactionId);
                    try {
                        this.transactions.invalidate(transactionId);
                    } catch (Exception ignored) {
                    }
                    return Future.succeededFuture();
                });
    }

    private Future<Optional<String>> getHostId(Context context) {
        Promise<Optional<String>> promise = Promise.promise();
        this.vertx.sharedData().getAsyncMap(host_key)
                .compose(map -> map.get(context.getId()))
                .onSuccess(r -> {
                    if (log.isDebugEnabled()) {
                        log.error("get host id {} by transaction id {}", r, context.getId());
                    }
                    if (r == null) {
                        promise.complete(Optional.empty());
                        return;
                    }
                    if (r instanceof String) {
                        String hostId = (String) r;
                        if (log.isDebugEnabled()) {
                            log.debug("get host({}) by transaction({})", hostId, context.getId());
                        }
                        promise.complete(Optional.of(hostId));
                        return;
                    }
                    promise.fail(String.format("host id of transaction(%s) is not string type", context.getId()));
                })
                .onFailure(e -> {
                    log.error("get host id by transaction({}) is failed", context.getId());
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
        this.discovery.getRecord(r -> r.getRegistration().equals(hostId))
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

    protected Future<JsonArray> remoteInvoke(String path, Context context, JsonObject form) {
        if (this.discovery == null) {
            return Future.failedFuture("server is not in distribute mode");
        }
        Promise<JsonArray> promise = Promise.promise();
        this.getHostId(context)
                .compose(hostId -> {
                    if (hostId.isEmpty()) {
                        return Future.failedFuture("transaction id is lost");
                    }
                    return Future.succeededFuture(hostId.get());
                })
                .compose(this::fetchHostRecord)
                .compose(record -> {
                    if (record.isEmpty()) {
                        return Future.failedFuture(String.format("host of request(%s) is lost", context.getId()));
                    }
                    return Future.succeededFuture(discovery.getReference(record.get()));
                })
                .compose(reference -> {
                    Promise<JsonArray> remoteInvokePromise = Promise.promise();
                    HttpClient client = reference.getAs(HttpClient.class);
                    client.request(HttpMethod.POST, path)
                            .compose(request -> {
                                return request.putHeader("x-transaction-id", context.getId())
                                        .putHeader("Authorization", context.getAuthorizationToken())
                                        .send(form.encode());
                            })
                            .onSuccess(rr -> {
                                rr.body()
                                        .compose(buf -> Future.succeededFuture(new JsonArray(buf)))
                                        .onSuccess(body -> {
                                            remoteInvokePromise.complete(body);
                                            reference.release();
                                        })
                                        .onFailure(e -> {
                                            log.error("remote invoke failed at body parsing", e);
                                            remoteInvokePromise.fail("remote invoke failed at body parsing");
                                            reference.release();
                                        });
                            })
                            .onFailure(remoteInvokePromise::fail);
                    return remoteInvokePromise.future();
                })
                .onSuccess(promise::complete)
                .onFailure(promise::fail);
        return promise.future();
    }

}
