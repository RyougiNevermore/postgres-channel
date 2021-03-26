package org.pharosnet.postgres.channel.http;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.*;
import lombok.extern.slf4j.Slf4j;
import org.pharosnet.postgres.channel.config.http.HttpConfig;
import org.pharosnet.postgres.channel.config.http.SSLConfig;

import java.util.List;
import java.util.Optional;

@Slf4j
public class Http {

    public Http(Vertx vertx, JsonObject config) {
        this.vertx = vertx;
        this.config = new HttpConfig(config.getJsonObject("http"));
        this.vertx.getOrCreateContext().put("config", config);
    }

    private final Vertx vertx;
    private final HttpConfig config;
    private HttpServer server;

    protected Vertx vertx() {
        return vertx;
    }

    protected Future<Void> run() {
        Promise<Void> promise = Promise.promise();

        HttpServerOptions options = new HttpServerOptions();

        if (vertx.isNativeTransportEnabled() && this.config.getNetNative() != null) {
            options.setTcpFastOpen(Optional.ofNullable(this.config.getNetNative().getTcpFastOpen()).orElse(false))
                    .setTcpCork(Optional.ofNullable(this.config.getNetNative().getTcpCork()).orElse(false))
                    .setTcpQuickAck(Optional.ofNullable(this.config.getNetNative().getTcpQuickAck()).orElse(false))
                    .setReusePort(Optional.ofNullable(this.config.getNetNative().getReusePort()).orElse(false));
        }

        options.setPort(this.config.getPort());

        options.setHost(this.config.getHost());

        options.setLogActivity(Optional.ofNullable(this.config.getEnableLogActivity()).orElse(false));
        options.setAcceptBacklog(Optional.ofNullable(this.config.getBacklog()).orElse(NetServerOptions.DEFAULT_ACCEPT_BACKLOG));
        if (this.config.getCompress() != null) {
            if (log.isDebugEnabled()) {
                log.debug("set compress options. noted!");
            }
            options.setCompressionSupported(Optional.ofNullable(this.config.getCompress().getCompression()).orElse(false));
            options.setDecompressionSupported(Optional.ofNullable(this.config.getCompress().getDecompression()).orElse(false));
        }
        if (this.config.getSsl() != null) {
            if (log.isDebugEnabled()) {
                log.debug("set ssl options. noted!");
            }
            SSLConfig sslConfig = this.config.getSsl();

            boolean hasKey = false;
            String keystore = Optional.ofNullable(sslConfig.getKeystore()).orElse("").trim();
            if (keystore.length() > 0) {
                String password = Optional.ofNullable(sslConfig.getPassword()).orElse("").trim();
                if (password.length() == 0) {
                    log.error("create ssl failed, password of jks is empty.");
                    promise.fail(new Exception("create ssl failed, password of jks is empty."));
                    return promise.future();
                }
                JksOptions jksOptions = new JksOptions().setPath(keystore).setPassword(password);
                if (Optional.ofNullable(sslConfig.getTrust()).orElse(false)) {
                    options.setTrustStoreOptions(jksOptions);
                } else {
                    options.setKeyStoreOptions(jksOptions);
                }
                hasKey = true;
            }
            String cert = Optional.ofNullable(sslConfig.getCert()).orElse("").trim();
            if (cert.length() > 0) {
                String key = Optional.ofNullable(sslConfig.getKey()).orElse("").trim();
                if (key.length() == 0) {
                    log.error("create ssl failed, key of cert is empty.");
                    promise.fail(new Exception("create ssl failed, key of cert is empty."));
                    return promise.future();
                }
                options.setKeyCertOptions(new PemKeyCertOptions().setCertPath(cert).setKeyPath(key));

                hasKey = true;
            }

            if (!hasKey) {
                log.error("create ssl failed, not defined.");
                promise.fail(new Exception("create ssl failed, not defined."));
                return promise.future();
            }

            options.removeEnabledSecureTransportProtocol("TLSv1");
            options.addEnabledSecureTransportProtocol("TLSv1.3");
            options.setSsl(true);
            SSLEngineOptions sslEngineOptions;
            if (OpenSSLEngineOptions.isAvailable()) {
                sslEngineOptions = new OpenSSLEngineOptions();
            } else {
                sslEngineOptions = new JdkSSLEngineOptions();
            }
            options.setSslEngineOptions(sslEngineOptions);

            if (Optional.ofNullable(sslConfig.getHttp2()).orElse(false)) {

                if (Optional.ofNullable(sslConfig.getHttp2UseAlpn()).orElse(false)) {
                    options.setUseAlpn(true);
                    options.setAlpnVersions(List.of(HttpVersion.HTTP_2, HttpVersion.HTTP_1_1));
                }
                int http2WindowSize = Optional.ofNullable(sslConfig.getHttp2WindowSize()).orElse(-1);
                if (http2WindowSize > 0) {
                    options.setHttp2ConnectionWindowSize(http2WindowSize);
                }
            }
        }

        vertx.createHttpServer(options).requestHandler(HttpRouterBuilder.build(vertx)).listen(r -> {
            if (r.failed()) {
                log.error("无法启动 HTTP 服务,", r.cause());
                promise.fail(r.cause());
                return;
            }
            if (log.isDebugEnabled()) {
                log.debug("启动 HTTP 服务成功, {}:{}", options.getHost(), options.getPort());
            }
            this.server = r.result();
            // todo discovery
            promise.complete();
        });

        return promise.future();
    }

    public Future<Void> close() {
        Promise<Void> promise = Promise.promise();
        this.server.close(promise);
        return promise.future();
    }

}
