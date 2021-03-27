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
import org.pharosnet.postgres.channel.service.QueryForm;
import org.pharosnet.postgres.channel.service.QueryService;

public class QueryRouter {

    public QueryRouter(Vertx vertx) {
        this.vertx = vertx;
        this.service = QueryService.proxy(vertx);
    }

    private final Vertx vertx;
    private final QueryService service;

    public void build(Router router) {
        router.post(QueryService.HTTP_PATH)
                .handler(AuthorizationHandler.create(vertx))
                .handler(ResponseTimeHandler.create())
                .handler(Context.create())
                .handler(BodyHandler.create().setBodyLimit(4194304))
                .handler(this::handle);
    }

    private void handle(RoutingContext routingContext) {
        Context context = Context.get(routingContext);
        QueryForm form = new QueryForm(routingContext.getBodyAsJson());
        Promise<JsonArray> promise = Promise.promise();
        this.service.query(context, form, promise);
        promise.future()
                .onSuccess(r -> {
                    routingContext.response()
                            .setStatusCode(200)
                            .setChunked(true)
                            .putHeader("x-transaction-id", context.getId())
                            .end(r.encode(), "UTF-8");
                })
                .onFailure(e -> {
                    routingContext.fail(505, e);
                });
    }

}
