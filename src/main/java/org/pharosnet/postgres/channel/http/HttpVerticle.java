package org.pharosnet.postgres.channel.http;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.ServiceDiscoveryOptions;
import io.vertx.servicediscovery.consul.ConsulServiceImporter;
import io.vertx.servicediscovery.types.HttpEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.pharosnet.postgres.channel.config.http.DiscoveryConfig;
import org.pharosnet.postgres.channel.config.http.HttpConfig;
import org.pharosnet.postgres.channel.config.postgres.PostgresConfig;
import org.pharosnet.postgres.channel.database.Databases;
import org.pharosnet.postgres.channel.service.QueryService;
import org.pharosnet.postgres.channel.service.TransactionService;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class HttpVerticle extends AbstractVerticle {

    private Http http;
    private Databases databases;
    private ServiceDiscovery discovery;
    private List<MessageConsumer<JsonObject>> consumers;


    public void register() {
        this.consumers = new ArrayList<>();
        this.consumers.add(QueryService.register(this.vertx));
        this.consumers.add(TransactionService.register(this.vertx));
    }

    public Future<Void> unregister() {
        Promise<Void> promise = Promise.promise();
        if (consumers == null) {
            promise.complete();
            return promise.future();
        }

        CompositeFuture compositeFuture = CompositeFuture.all(consumers.stream().map(consumer -> {
            Promise<Void> unregisterPromise = Promise.promise();
            consumer.unregister(unregisterPromise);
            return unregisterPromise.future();
        }).collect(Collectors.toList()));

        compositeFuture.onSuccess(r -> promise.complete());
        compositeFuture.onFailure(promise::fail);

        return promise.future();
    }

    @Override
    public void start(Promise<Void> promise) throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("NativeTransportEnabled = {}", vertx.isNativeTransportEnabled());
        }

        HttpConfig httpConfig = new HttpConfig(this.config().getJsonObject("http"));
        if (httpConfig.getDiscovery() != null) {
            DiscoveryConfig discoveryConfig = httpConfig.getDiscovery();
            String hostname = Optional.ofNullable(System.getenv("HOSTNAME")).orElse("").trim();
            String ip;
            try {
                InetAddress addr = InetAddress.getByName(hostname);
                ip = addr.getHostAddress();
            } catch (UnknownHostException e) {
                log.error("can not get ip by HOSTNAME({})", hostname, e);
                throw new UnknownHostException(String.format("an not get ip by HOSTNAME(%s), %s", hostname, e.getMessage()));
            }

            String discoveryKind = Optional.ofNullable(discoveryConfig.getKind()).orElse("none").trim();
            if (!"none".equals(discoveryKind)) {
                this.discovery = ServiceDiscovery.create(vertx,
                        new ServiceDiscoveryOptions()
                                .setAnnounceAddress(ip)
                                .setName("postgres-channel"));
                if (discoveryKind.equals("consul")) {
                    this.discovery.registerServiceImporter(new ConsulServiceImporter(),
                            new JsonObject()
                                    .put("host", discoveryConfig.getConsul().getHost())
                                    .put("port", discoveryConfig.getConsul().getPort())
                                    .put("scan-period", discoveryConfig.getConsul().getScanPeriod())
                                    .put("acl_token", discoveryConfig.getConsul().getAclToken()));

                } else if (discoveryKind.equals("kubernetes")) {
//                this.discovery.registerServiceImporter(new KubernetesServiceImporter(),
//                        new JsonObject());
                } else if (discoveryKind.equals("docker")) {
//                this.discovery.registerServiceImporter(new DockerLinksServiceImporter(),
//                        new JsonObject());
                } else {
                    promise.fail("discovery kind in config is invalid, must be one of consul, kubernetes and docker");
                    return;
                }
            }

            this.vertx.getOrCreateContext().put("_discovery", this.discovery);
        }


        this.databases = new Databases(vertx, new PostgresConfig(config().getJsonObject("postgres")));

        this.databases.check()
                .onFailure(e -> {
                    log.error("can not connect to postgres");
                    promise.fail(e);
                })
                .onSuccess(v -> {
                    this.http = new Http(this.vertx, this.config());
                    this.register();
                    this.http.run()
                            .onSuccess(r -> {
                                if (this.discovery == null) {
                                    promise.complete();
                                    return;
                                }
                                Record record = HttpEndpoint.createRecord("postgres-channel", httpConfig.getHost(), httpConfig.getPort(), "/");
                                this.discovery.publish(record)
                                        .onSuccess(ar -> {
                                            if (log.isDebugEnabled()) {
                                                log.debug("publish http succeed, id = {}", ar.getRegistration());
                                            }
                                            vertx.getOrCreateContext().put("_discovery_id", ar.getRegistration());
                                            promise.complete();
                                        })
                                        .onFailure(e -> {
                                            log.error("publish http failed", e);
                                            promise.fail("publish http failed");
                                        });
                            })
                            .onFailure(e -> {
                                log.error("create http server failed", e);
                                promise.fail("create http server failed");
                            });
                });
    }

    @Override
    public void stop(Promise<Void> promise) throws Exception {
        this.unregister()
                .compose(r -> {
                    if (this.discovery == null) {
                        return Future.succeededFuture();
                    }
                    return this.discovery.unpublish(vertx.getOrCreateContext().get("_discovery_id"))
                            .compose(_dr -> {
                                this.discovery.close();
                                return Future.succeededFuture();
                            });
                })
                .compose(r -> this.http.close())
                .compose(r -> this.databases.close())
                .onSuccess(r -> {
                    promise.complete();
                })
                .onFailure(e -> {
                    log.error("stop http server failed", e);
                    promise.fail("stop http server failed");
                });
    }

}
