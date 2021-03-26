package org.pharosnet.postgres.channel.auth;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials;
import io.vertx.ext.auth.htpasswd.HtpasswdAuth;
import io.vertx.ext.auth.htpasswd.HtpasswdAuthOptions;
import io.vertx.ext.web.RoutingContext;
import org.pharosnet.postgres.channel.config.http.HttpConfig;

import java.util.Optional;

public class AuthorizationHandler implements Handler<RoutingContext> {

    public static AuthorizationHandler create(Vertx vertx) {
        return new AuthorizationHandler(vertx);
    }

    private AuthorizationHandler(Vertx vertx) {
        JsonObject config = vertx.getOrCreateContext().get("config");
        HttpConfig httpConfig = new HttpConfig(config.getJsonObject("http"));
        String htpasswdFile = Optional.ofNullable(httpConfig.getHtpasswdFile()).orElse("").trim();
        if (htpasswdFile.isBlank()) {
            throw new RuntimeException("htpasswdFile is missing in http config");
        }
        this.authProvider = HtpasswdAuth
                .create(vertx, new HtpasswdAuthOptions()
                        .setHtpasswdFile(htpasswdFile));
    }

    private final HtpasswdAuth authProvider;

    @Override
    public void handle(RoutingContext ctx) {
        String authorization = Optional.ofNullable(ctx.request().getHeader("Authorization")).orElse("");
        if (authorization.length() == 0) {
            ctx.fail(401, new Exception("Unauthorized"));
            return;
        }
        if (!authorization.startsWith("Basic ")) {
            ctx.fail(401, new Exception("Authorization is invalid"));
            return;
        }
        String token = authorization.substring(6);
        String username = token.substring(0, token.indexOf(":"));
        String password = token.substring(token.indexOf(":") + 1);
        authProvider.authenticate(new UsernamePasswordCredentials(username, password))
                .onSuccess(user -> {
                    ctx.setUser(user);
                    ctx.data().put("Authorization", authorization);
                    ctx.next();
                })
                .onFailure(e -> {
                    ctx.fail(401, new Exception("Unauthorized, username or password is invalid."));
                });
    }
}
