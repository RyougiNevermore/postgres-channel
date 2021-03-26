package org.pharosnet.postgres.channel.service;

import io.vertx.core.Vertx;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TransactionServiceImpl implements TransactionService {

    public TransactionServiceImpl(Vertx vertx) {
        this.vertx = vertx;
    }

    private final Vertx vertx;

}
