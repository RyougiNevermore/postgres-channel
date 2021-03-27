package org.pharosnet.postgres.channel.context;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.util.Optional;

public class ContextHandler implements Handler<RoutingContext> {

    @Override
    public void handle(RoutingContext routingContext) {
       String id = Optional.ofNullable(routingContext.request().getHeader("x-transaction-id")).orElse("").trim();
       Context context = new Context();
       context.setId(id);
       context.setData(new JsonObject());
       context.setAuthorizationToken((String) routingContext.data().get("Authorization"));
       routingContext.data().put(Context.context_key, context);
       routingContext.next();
    }
}
