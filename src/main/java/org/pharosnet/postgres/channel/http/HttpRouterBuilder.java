package org.pharosnet.postgres.channel.http;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials;
import io.vertx.ext.auth.htpasswd.HtpasswdAuth;
import io.vertx.ext.auth.htpasswd.HtpasswdAuthOptions;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.web.Router;
import io.vertx.serviceproxy.ServiceException;
import org.pharosnet.postgres.channel.config.http.HttpConfig;
import org.pharosnet.postgres.channel.http.router.QueryRouter;

import java.util.Optional;

public class HttpRouterBuilder {

    static Router build(Vertx vertx) {
        Router router = Router.router(vertx);
        buildFailed(router);
        buildHealthCheck(vertx, router);
//        buildAuth(vertx, router);
        buildService(vertx, router);
        return router;
    }

    static void buildService(Vertx vertx, Router router) {
        new QueryRouter(vertx).build(router);
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

//    static void buildAuth(Vertx vertx, Router router) {
//        JsonObject config = vertx.getOrCreateContext().get("config");
//        HttpConfig httpConfig = new HttpConfig(config.getJsonObject("http"));
//        String htpasswdFile = Optional.ofNullable(httpConfig.getHtpasswdFile()).orElse("").trim();
//        if (htpasswdFile.isBlank()) {
//            return;
//        }
//        HtpasswdAuth authProvider = HtpasswdAuth
//                .create(vertx, new HtpasswdAuthOptions()
//                        .setHtpasswdFile(htpasswdFile));
//        router.route().handler(ctx -> {
//            String authorization = Optional.ofNullable(ctx.request().getHeader("Authorization")).orElse("");
//            if (authorization.length() == 0) {
//                ctx.fail(401, new Exception("Unauthorized"));
//                return;
//            }
//            if (!authorization.startsWith("Basic ")) {
//                ctx.fail(401, new Exception("Authorization is invalid"));
//                return;
//            }
//            String token = authorization.substring(6);
//            String username = token.substring(0, token.indexOf(":"));
//            String password = token.substring(token.indexOf(":") + 1);
//            authProvider.authenticate(new UsernamePasswordCredentials(username, password))
//                    .onSuccess(user -> {
//                        ctx.setUser(user);
//                        ctx.next();
//                    })
//                    .onFailure(e -> {
//                        ctx.fail(401, new Exception("Unauthorized, username or password is invalid."));
//                    });
//        });
//    }

}
