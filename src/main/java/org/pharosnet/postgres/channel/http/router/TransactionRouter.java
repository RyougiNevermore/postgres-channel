package org.pharosnet.postgres.channel.http.router;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;

public class TransactionRouter {

    public TransactionRouter(Vertx vertx) {
        this.vertx = vertx;
    }

    private final Vertx vertx;

    public void build(Router router) {

    }

}
