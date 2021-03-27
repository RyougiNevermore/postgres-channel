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
public interface TransactionService {


    String HTTP_PATH = "/transaction";

    String SERVICE_ADDRESS = "postgres-channel/transaction";

    static MessageConsumer<JsonObject> register(Vertx vertx) {
        return new ServiceBinder(vertx).setAddress(SERVICE_ADDRESS).register(TransactionService.class, new TransactionServiceImpl(vertx));
    }

    static TransactionService proxy(Vertx vertx) {
        return new TransactionServiceVertxEBProxy(vertx, SERVICE_ADDRESS);
    }

    void execute(Context context, TransactionForm form, Handler<AsyncResult<TransactionResult>> handler);

}
