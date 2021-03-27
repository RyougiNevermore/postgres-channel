package org.pharosnet.postgres.channel.database;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.impl.cpu.CpuCoreSensor;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.pgclient.SslMode;
import io.vertx.sqlclient.PoolOptions;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.pharosnet.postgres.channel.config.postgres.NodeConfig;
import org.pharosnet.postgres.channel.config.postgres.PostgresConfig;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class Databases {

    private static final String context_key = "_databases";

    public static Databases get(Vertx vertx) {
        return vertx.getOrCreateContext().get(context_key);
    }

    public Databases(Vertx vertx, PostgresConfig config) {
        String kind = Optional.ofNullable(config.getKind()).orElse("").trim();
        if (kind.equalsIgnoreCase("standalone")) {
            this.distributed = false;
        } else if (kind.equalsIgnoreCase("master_slaver")) {
            this.distributed = true;
        } else if (kind.equalsIgnoreCase("xl")) {
            this.distributed = true;
        } else {
            throw new RuntimeException("postgres config is invalid, kind is invalid, it must be one of standalone, master_slaver and xl");
        }
        this.index = new AtomicInteger();
        List<NodeConfig> nodeConfigs = config.getNodes();
        this.nodes = new ArrayList<>();
        for (NodeConfig nodeConfig : nodeConfigs) {
            PgConnectOptions connectOptions = new PgConnectOptions()
                    .setPort(nodeConfig.getPort())
                    .setHost(nodeConfig.getHost())
                    .setDatabase(nodeConfig.getDatabase())
                    .setUser(nodeConfig.getUser())
                    .setPassword(nodeConfig.getPassword());

            if (Optional.ofNullable(nodeConfig.getSsl()).orElse(false)) {
                connectOptions.setSsl(true);
                connectOptions.setSslMode(SslMode.of(Optional.ofNullable(nodeConfig.getSslMode()).orElse("disable").trim().toLowerCase()));
                connectOptions.setPemTrustOptions(new PemTrustOptions()
                        .addCertPath(nodeConfig.getSslCertPath()));
            }

            int maxPoolSize = Optional.ofNullable(nodeConfig.getMaxPoolSize()).orElse(0);
            if (maxPoolSize < 1) {
                maxPoolSize = CpuCoreSensor.availableProcessors() * 2;
            }
            PoolOptions poolOptions = new PoolOptions().setMaxSize(maxPoolSize);

            PgPool pool = PgPool.pool(vertx, connectOptions, poolOptions);

            this.nodes.add(pool);
        }
        vertx.getOrCreateContext().put(context_key, this);
        Duration transactionCacheTTL;
        try {
            transactionCacheTTL = Duration.parse(Optional.ofNullable(config.getTransactionCacheTTL()).orElse("PT10M"));
        } catch (Exception e) {
            log.error("parse transactionCacheTTL failed, transactionCacheTTL = {}, set PT10M default", config.getTransactionCacheTTL(), e);
            transactionCacheTTL = Duration.ofMinutes(10);
        }
        Long transactionCacheMaxSize = Optional.ofNullable(config.getTransactionCacheMaxSize()).orElse(100000L);
        this.transactions = Caffeine.newBuilder()
                .expireAfterWrite(transactionCacheTTL)
                .maximumSize(transactionCacheMaxSize)
                .evictionListener(new TransactionEvictionListener())
                .build();
        vertx.getOrCreateContext().put("_cache_transaction_ttl", transactionCacheMaxSize);
    }

    private final boolean distributed;
    private final List<PgPool> nodes;
    private final AtomicInteger index;
    private final Cache<@NonNull String, @NonNull CachedTransaction> transactions;

    public PgPool getNode() {
        if (!this.distributed) {
            return this.nodes.get(0);
        }
        return this.nodes.get(this.index.addAndGet(1) % this.nodes.size());
    }

    public PgPool getSlaver() {
        if (this.nodes.size() < 2) {
            return null;
        }
        int pos = this.index.addAndGet(1) % this.nodes.size();
        if (pos == 0) {
            pos = 1;
        }
        return this.nodes.get(pos);
    }

    public Future<Void> check() {
        Promise<Void> promise = Promise.promise();
        List<Future> futures = new ArrayList<>();
        for (PgPool pool : this.nodes) {
            Promise checkPromise = Promise.promise();
            pool.getConnection()
                    .onSuccess(connection -> {
                        connection.close();
                        checkPromise.complete();
                    })
                    .onFailure(checkPromise::fail);
            futures.add(checkPromise.future());
        }
        CompositeFuture.all(futures)
                .onSuccess(r -> {
                    promise.complete();
                })
                .onFailure(promise::fail);
        return promise.future();
    }

    public Future<Void> close() {
        Promise<Void> promise = Promise.promise();
        List<Future> futures = new ArrayList<>();
        for (PgPool pool : this.nodes) {
            futures.add(pool.close());
        }
        CompositeFuture.all(futures)
                .onSuccess(r -> {
                    this.transactions.cleanUp();
                    promise.complete();
                })
                .onFailure(e -> {
                    this.transactions.cleanUp();
                    promise.fail(e);
                });
        return promise.future();
    }

    public Cache<@NonNull String, @NonNull CachedTransaction> getTransactions() {
        return transactions;
    }

}
