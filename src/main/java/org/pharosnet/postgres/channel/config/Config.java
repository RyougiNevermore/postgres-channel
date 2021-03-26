package org.pharosnet.postgres.channel.config;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;

import java.net.URL;
import java.util.Optional;

@Slf4j
public class Config {

    private static final String PATH_KEY = "CONFIG_PATH";
    private static final String KIND_KEY = "CONFIG_KIND";
    private static final String FORMAT_KEY = "CONFIG_FORMAT";

    public static Future<JsonObject> read(Vertx vertx) {

        String kind = Optional.ofNullable(System.getenv(KIND_KEY)).orElse("").trim();
        if (kind.isBlank()) {
            kind = "FILE";
        }
        String format = Optional.ofNullable(System.getenv(FORMAT_KEY)).orElse("").trim().toLowerCase();
        if (format.isBlank()) {
            format = "json";
        }

        if (kind.equalsIgnoreCase("FILE")) {
            return readFromFile(vertx, format);
        }

        return Future.failedFuture("read config failed, CONFIG_KIND is invalid.");
    }

    private static Future<JsonObject> readFromFile(Vertx vertx, String format) {
        String filename = Optional.ofNullable(System.getenv(PATH_KEY)).orElse("").trim();
        if (filename.isBlank()) {
            throw new RuntimeException("System ENV named CONFIG_PATH is not found.");
        }

        URL configURL = Thread.currentThread().getContextClassLoader().getResource(filename);
        if (configURL == null) {
            log.error("read config failed, file is not found, {}", filename);
            throw new RuntimeException("read config failed, file is not found.");
        }
        Promise<JsonObject> promise = Promise.promise();
        String configFile = configURL.getFile();
        ConfigStoreOptions configStoreOptions = new ConfigStoreOptions()
                .setType("file")
                .setFormat(format)
                .setConfig(new JsonObject().put("path", configFile));
        ConfigRetriever retriever = ConfigRetriever.create(vertx, new ConfigRetrieverOptions().addStore(configStoreOptions));
        retriever.getConfig().onSuccess(r -> {
            if (log.isDebugEnabled()) {
                log.debug("config : \n {}", r.encodePrettily());
            }
            promise.complete(r);
        }).onFailure(e -> {
            log.error("read config failed, {}", configFile, e);
            promise.fail("read config failed.");
        });
        return promise.future();
    }

}
