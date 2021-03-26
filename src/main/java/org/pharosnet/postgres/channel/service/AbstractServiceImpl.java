package org.pharosnet.postgres.channel.service;

import com.github.benmanes.caffeine.cache.Cache;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.pharosnet.postgres.channel.context.Context;
import org.pharosnet.postgres.channel.database.CachedTransaction;
import org.pharosnet.postgres.channel.database.Databases;

import java.util.Optional;
import java.util.UUID;

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

    protected Future<Boolean> isLocal(String requestId) {
        Promise<Boolean> promise = Promise.promise();
        this.getTransactionIdAndHostIdByRequestId(requestId)
                .onSuccess(ids -> {
                    if (ids.isEmpty()) {
                        promise.complete(true);
                        return;
                    }
                    promise.complete(ids.get().getString(1).equals(this.getHostId()));
                })
                .onFailure(promise::fail);
        return promise.future();
    }

    protected Future<Void> putTransaction(String requestId, SqlConnection connection, Transaction transaction) {
        String txId = UUID.randomUUID().toString();
        this.transactions.put(requestId, new CachedTransaction(connection, transaction));
        return this.vertx.sharedData().getAsyncMap(host_key)
                .compose(map -> map.put(requestId, String.format("%s@%s", txId, this.getHostId()), this.transactionCachedTTL));
    }

    protected Future<Optional<CachedTransaction>> getTransaction(String requestId) {
        try {
            CachedTransaction transaction = this.transactions.getIfPresent(requestId);
            return Future.succeededFuture(Optional.ofNullable(transaction));
        } catch (Exception e) {
            if (log.isWarnEnabled()) {
                log.warn("get transaction failed by request({})", requestId);
            }
            return Future.succeededFuture(Optional.empty());
        }
    }

    protected Future<Void> releaseTransaction(String requestId) {
        return this.vertx.sharedData().getAsyncMap(host_key)
                .compose(map -> {
                    map.remove(requestId);
                    this.transactions.invalidate(requestId);
                    return Future.succeededFuture();
                });
    }

    private Future<Optional<JsonArray>> getTransactionIdAndHostIdByRequestId(String requestId) {
        Promise<Optional<JsonArray>> promise = Promise.promise();
        this.vertx.sharedData().getAsyncMap(host_key)
                .compose(map -> map.get(requestId))
                .onSuccess(r -> {
                    if (log.isDebugEnabled()) {
                        log.error("get transaction id and host id {} by request id {}", r, requestId);
                    }
                    if (r == null) {
                        promise.complete(Optional.empty());
                        return;
                    }
                    if (r instanceof String) {
                        String token = (String) r;
                        if (token.isBlank()) {
                            promise.complete(Optional.empty());
                            return;
                        }
                        String[] items = token.split("@");
                        promise.complete(Optional.of(new JsonArray().add(items[0]).add(items[1])));
                        return;
                    }
                    promise.fail(String.format("transaction id and host id of request id %s is not string type", requestId));
                })
                .onFailure(e -> {
                    log.error("get transaction id and host id by request id {} is failed", requestId);
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

    protected Future<JsonArray> remoteInvoke(String path, Context context, QueryForm form) {
        if (this.discovery == null) {
            return Future.failedFuture("server is not in distribute mode");
        }
        Promise<JsonArray> promise = Promise.promise();
        this.getTransactionIdAndHostIdByRequestId(context.getId())
                .compose(ids -> {
                    if (ids.isEmpty()) {
                        return Future.failedFuture("request id is lost");
                    }
                    String hostId = ids.get().getString(1);
                    return Future.succeededFuture(hostId);
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
                                return request.putHeader("x-request-id", context.getId())
                                        .putHeader("Authorization", context.getAuthorizationToken())
                                        .send(form.toJson().encode());
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
