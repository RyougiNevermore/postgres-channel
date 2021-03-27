package org.pharosnet.postgres.channel;

import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Log4J2LoggerFactory;
import io.vertx.core.*;
import io.vertx.core.impl.cpu.CpuCoreSensor;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import lombok.extern.slf4j.Slf4j;
import org.pharosnet.postgres.channel.config.Config;
import org.pharosnet.postgres.channel.http.HttpVerticle;

import java.util.Optional;

@Slf4j
public class Main {

    static {
        System.setProperty("postgres-channel.logger.level", Optional.ofNullable(System.getenv("POSTGRES_CHANNEL_LOGGER_LEVEL")).orElse("info").trim());
        System.setProperty("postgres-channel.logger.appender", Optional.ofNullable(System.getenv("POSTGRES_CHANNEL_LOGGER_APPENDER")).orElse("ASYNC").trim());

        System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.Log4j2LogDelegateFactory");
        System.setProperty("hazelcast.logging.type", "log4j2");
        InternalLoggerFactory.setDefaultFactory(Log4J2LoggerFactory.INSTANCE);

        DatabindCodec.mapper()
                .registerModule(new ParameterNamesModule())
                .registerModule(new JavaTimeModule())
                .registerModule(new Jdk8Module());
    }

    public static void main(String[] args) throws Exception {

        Promise<Future<Void>> promise = Promise.promise();
        createVertx()
                .onSuccess(r -> {
                    Vertx vertx = r;
                    if (log.isDebugEnabled()) {
                        log.debug("native transport: {}", vertx.isNativeTransportEnabled());
                    }
                    Config.read(vertx)
                            .compose(config -> {
                                DeploymentOptions deploymentOptions = new DeploymentOptions();
                                deploymentOptions.setWorker(true).setWorkerPoolSize(CpuCoreSensor.availableProcessors() * 2);
                                deploymentOptions.setConfig(config);
                                return vertx.deployVerticle(new HttpVerticle(), deploymentOptions);
                            })
                            .onSuccess(deploymentId -> {
                                if (log.isDebugEnabled()) {
                                    log.debug("startup succeed，{}", deploymentId);
                                }
                                CloseHooker hooker = new CloseHooker(vertx, deploymentId);
                                Runtime.getRuntime().addShutdownHook(hooker);
                                promise.complete(hooker.closeCallback());
                            })
                            .onFailure(e -> {
                                log.error("startup failed", e);
                                promise.fail(e);
                            });
                })
                .onFailure(e -> {
                    log.error("startup failed", e);
                    promise.fail(new Exception("startup failed", e));
                });

        Thread.sleep(10 * 1000);

    }

    private static Future<Vertx> createVertx() {
        Promise<Vertx> promise = Promise.promise();

        VertxOptions vertxOptions = new VertxOptions();

        boolean nativeTransported = Optional.ofNullable(System.getenv("NATIVE")).orElse("true").strip().equalsIgnoreCase("true");
        if (nativeTransported) {
            if (log.isDebugEnabled()) {
                log.debug("set prefer native transport true");
            }
            vertxOptions.setPreferNativeTransport(true);
        }

        Integer eventLoops = Optional.ofNullable(Integer.getInteger(Optional.ofNullable(System.getenv("EVENT_LOOP")).orElse("0").strip())).orElse(0);

        if (eventLoops > 0) {
            if (log.isDebugEnabled()) {
                log.debug("set event loop pool size be {}", eventLoops);
            }
            vertxOptions.setEventLoopPoolSize(eventLoops);
        }

        boolean clusterEnabled = Optional.ofNullable(System.getenv("CLUSTER")).orElse("false").trim().equalsIgnoreCase("true");
        if (clusterEnabled) {
            String clusterConfigPath = Optional.ofNullable(System.getenv("CLUSTER_CONFIG")).orElse("").trim();
            if (clusterConfigPath.length() == 0) {
                promise.fail("System env named CLUSTER_CONFIG is empty.");
                return promise.future();
            }
            System.setProperty("vertx.hazelcast.config", clusterConfigPath);
            ClusterManager clusterManager = new HazelcastClusterManager();
            vertxOptions.setClusterManager(clusterManager);

            Vertx.clusteredVertx(vertxOptions, r -> {
                if (r.succeeded()) {
                    promise.complete(r.result());
                } else {
                    promise.fail(new Exception("create cluster model failed", r.cause()));
                }
            });
        } else {
            promise.complete(Vertx.vertx(vertxOptions));
        }

        return promise.future();
    }

}
