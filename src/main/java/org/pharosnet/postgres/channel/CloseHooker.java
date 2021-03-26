package org.pharosnet.postgres.channel;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Optional;

@Slf4j
public class CloseHooker extends Thread {

    public CloseHooker(Vertx vertx, String deploymentId) {
        this.vertx = vertx;
        this.callbackPromise = Promise.promise();
        this.deploymentId = deploymentId;
    }

    private final Vertx vertx;
    private final String deploymentId;
    private final Promise<Void> callbackPromise;

    protected Future<Void> closeCallback() {
        return this.callbackPromise.future();
    }

    private void undeploy() {

        this.vertx.undeploy(this.deploymentId)
                .onSuccess(r -> {
                    if (log.isDebugEnabled()) {
                        log.debug("undeploy {} succeed", this.deploymentId);
                    }
                    this.callbackPromise.complete();
                    this.notify();
                })
                .onFailure(e -> {
                    log.error("undeploy failed", e);
                    this.callbackPromise.fail(e);
                    this.notify();
                });

        Duration timeout = null;
        String waitTime = Optional.ofNullable(System.getenv("CLOSE_TIMEOUT")).orElse("3s");
        try {
            if (waitTime.length() > 0) {
                timeout = Duration.parse(waitTime);
            }
        } catch (Exception e) {
            log.error("parse close timeout {} failed", waitTime, e);
        }
        try {
            if (timeout != null) {
                this.wait(timeout.toMillis());
            } else {
                this.wait();
            }
        } catch (InterruptedException e) {
            log.error("wait closing failed, timeout", e);
            this.callbackPromise.fail(new Exception("wait closing failed, timeout."));
        }

    }

    @Override
    public void run() {
        if (log.isDebugEnabled()) {
            log.debug("开始卸载服务。");
        }
        try {
            this.undeploy();
        } catch (Exception e) {
            log.error("close failed", e);
            throw new RuntimeException(e);
        }
    }

}
