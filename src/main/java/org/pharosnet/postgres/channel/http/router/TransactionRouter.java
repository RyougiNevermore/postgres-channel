package org.pharosnet.postgres.channel.http.router;

import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.ResponseTimeHandler;
import org.pharosnet.postgres.channel.auth.AuthorizationHandler;
import org.pharosnet.postgres.channel.context.Context;
import org.pharosnet.postgres.channel.service.*;

public class TransactionRouter {

    public TransactionRouter(Vertx vertx) {
        this.vertx = vertx;
        this.service = TransactionService.proxy(vertx);
    }

    private final Vertx vertx;
    private final TransactionService service;

    public void build(Router router) {
        router.post(TransactionService.HTTP_PATH)
                .handler(AuthorizationHandler.create(vertx))
                .handler(ResponseTimeHandler.create())
                .handler(Context.create())
                .handler(BodyHandler.create().setBodyLimit(1024))
                .handler(this::handle);
    }

    private void handle(RoutingContext routingContext) {
        Context context = Context.get(routingContext);
        TransactionForm form = new TransactionForm(routingContext.getBodyAsJson());
        Promise<TransactionResult> promise = Promise.promise();
        this.service.execute(context, form, promise);
        promise.future()
                .onSuccess(r -> {
                    routingContext.response()
                            .setStatusCode(200)
                            .setChunked(true)
                            .end(r.toJson().encode(), "UTF-8");
                })
                .onFailure(e -> {
                    routingContext.fail(505, e);
                });
    }

}
