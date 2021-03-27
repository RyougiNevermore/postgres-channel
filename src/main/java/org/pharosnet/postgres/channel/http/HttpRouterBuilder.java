package org.pharosnet.postgres.channel.http;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.web.Router;
import io.vertx.serviceproxy.ServiceException;
import org.pharosnet.postgres.channel.http.router.QueryRouter;
import org.pharosnet.postgres.channel.http.router.TransactionRouter;

public class HttpRouterBuilder {

    static Router build(Vertx vertx) {
        Router router = Router.router(vertx);
        buildFailed(router);
        buildHealthCheck(vertx, router);
        buildService(vertx, router);
        return router;
    }

    static void buildService(Vertx vertx, Router router) {
        new QueryRouter(vertx).build(router);
        new TransactionRouter(vertx).build(router);
    }

    static void buildHealthCheck(Vertx vertx, Router router) {
        HealthCheckHandler healthCheckHandler = HealthCheckHandler.create(vertx);
        router.get("/health*").handler(healthCheckHandler);
    }

    static void buildFailed(Router router) {
        router.route()
                .failureHandler(ctx -> {
                    if (ctx.failed()) {
                        int errorCode = ctx.statusCode();
                        JsonObject result = new JsonObject();
                        if (ctx.failure() instanceof ServiceException) {
                            ServiceException se = (ServiceException) ctx.failure();
                            errorCode = se.failureCode();
                            result.put("message", se.getMessage());
                            if (!se.getDebugInfo().isEmpty()) {
                                result.put("cause", se.getDebugInfo());
                            }
                        } else {
                            result.put("message", ctx.failure().getMessage());
                            result.put("cause", ctx.failure());
                        }
                        if (errorCode == -1) {
                            errorCode = 500;
                        }
                        ctx.response()
                                .setStatusCode(errorCode)
                                .putHeader("Content-Type", "application/json")
                                .end(result.encode(), "UTF-8");
                    } else {
                        ctx.next();
                    }
                });
    }

}
