package org.pharosnet.postgres.channel.service;

import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceBinder;
import org.pharosnet.postgres.channel.context.Context;

@VertxGen
@ProxyGen
public interface QueryService {

    String SERVICE_ADDRESS = "postgres-channel/query";

    static MessageConsumer<JsonObject> register(Vertx vertx) {
        return new ServiceBinder(vertx).setAddress(SERVICE_ADDRESS).register(QueryService.class, new QueryServiceImpl(vertx));
    }

    static QueryService proxy(Vertx vertx) {
        return new QueryServiceVertxEBProxy(vertx, SERVICE_ADDRESS);
    }

    void query(Context context, QueryForm form, Handler<AsyncResult<JsonArray>> handler);

}
