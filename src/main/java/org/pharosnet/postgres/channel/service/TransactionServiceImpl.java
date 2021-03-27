package org.pharosnet.postgres.channel.service;

import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.pharosnet.postgres.channel.context.Context;
import org.pharosnet.postgres.channel.database.CachedTransaction;

import java.util.Optional;
import java.util.UUID;

@Slf4j
public class TransactionServiceImpl extends AbstractServiceImpl implements TransactionService {

    public TransactionServiceImpl(Vertx vertx) {
        super(vertx);
    }

    @Override
    public void execute(Context context, TransactionForm form, Handler<AsyncResult<TransactionResult>> handler) {
        String command = Optional.ofNullable(form.getCommand()).orElse("").trim().toUpperCase();
        switch (command) {
            case "BEGIN":
                this.begin(handler);
                break;
            case "COMMIT":
                this.commit(context, form, handler);
                break;
            case "ROLLBACK":
                this.rollback(context, form, handler);
                break;
            default:
                handler.handle(ServiceException.fail(500, "command is invalid"));
                break;
        }
    }

    private void begin(Handler<AsyncResult<TransactionResult>> handler) {
        String id = UUID.randomUUID().toString();
        this.databases().getNode()
                .getConnection()
                .compose(connection -> {
                    Promise<CachedTransaction> cachedTransactionPromise = Promise.promise();
                    connection.begin()
                            .onSuccess(transaction -> {
                                cachedTransactionPromise.complete(new CachedTransaction(connection, transaction));
                            })
                            .onFailure(e -> {
                                log.error("transaction begin failed", e);
                                cachedTransactionPromise.fail(e);
                            });
                    return cachedTransactionPromise.future();
                })
                .compose(ct -> this.putTransaction(id, ct))
                .onSuccess(r -> {
                    if (log.isDebugEnabled()) {
                        log.debug("transaction begin succeed, id = {}, host = {}", id, this.getHostId());
                    }
                    handler.handle(Future.succeededFuture(new TransactionResult(id)));
                })
                .onFailure(e -> {
                    log.error("transaction begin failed", e);
                    handler.handle(ServiceException.fail(500, "transaction begin failed", new JsonObject().put("cause", e)));
                });

    }

    private void commit(Context context, TransactionForm form, Handler<AsyncResult<TransactionResult>> handler) {
        String id = Optional.ofNullable(context.getId()).orElse("").trim();
        if (id.isBlank()) {
            handler.handle(ServiceException.fail(400, "x-transaction-id header is empty"));
            return;
        }
        this.isLocal(context)
                .compose(local -> {
                    Promise<Void> resultPromise = Promise.promise();
                    if (local) {
                        this.getTransaction(context)
                                .compose(cachedTransaction -> {
                                    if (cachedTransaction.isEmpty()) {
                                        return Future.failedFuture(String.format("transaction %s is lost", id));
                                    }
                                    return cachedTransaction.get().getTransaction().commit()
                                            .eventually(v -> cachedTransaction.get().getConnection().close());

                                })
                                .compose(r -> this.releaseTransaction(id))
                                .onSuccess(r -> {
                                    resultPromise.complete();
                                })
                                .onFailure(e -> {
                                    log.error("transaction commit failed, id = {}, host = {}", id, this.getHostId(), e);
                                    resultPromise.fail(e);
                                });
                    } else {
                        this.remoteInvoke(TransactionService.HTTP_PATH, context, form.toJson())
                                .onFailure(e -> {
                                    log.error("transaction commit in remote failed, id = {}", id, e);
                                    resultPromise.fail(e);
                                })
                                .onSuccess(result -> {
                                    if (log.isDebugEnabled()) {
                                        log.error("transaction commit in remote succeed, id = {}", id);
                                    }
                                    resultPromise.complete();
                                });
                    }
                    return resultPromise.future();
                })
                .onSuccess(r -> {
                    if (log.isDebugEnabled()) {
                        log.debug("transaction commit succeed, id = {}, host = {}", id, this.getHostId());
                    }
                    handler.handle(Future.succeededFuture(new TransactionResult(id)));
                })
                .onFailure(e -> {
                    log.error("transaction commit failed", e);
                    handler.handle(ServiceException.fail(500, "transaction commit failed", new JsonObject().put("cause", e)));
                });
    }

    private void rollback(Context context, TransactionForm form, Handler<AsyncResult<TransactionResult>> handler) {
        String id = Optional.ofNullable(context.getId()).orElse("").trim();
        if (id.isBlank()) {
            handler.handle(ServiceException.fail(400, "x-transaction-id header is empty"));
            return;
        }
        this.isLocal(context)
                .compose(local -> {
                    Promise<Void> resultPromise = Promise.promise();
                    if (local) {
                        this.getTransaction(context)
                                .compose(cachedTransaction -> {
                                    if (cachedTransaction.isEmpty()) {
                                        return Future.failedFuture(String.format("transaction %s is lost", id));
                                    }
                                    return cachedTransaction.get().getTransaction().rollback()
                                            .eventually(v -> cachedTransaction.get().getConnection().close());

                                })
                                .compose(r -> this.releaseTransaction(id))
                                .onSuccess(r -> {
                                    resultPromise.complete();
                                })
                                .onFailure(e -> {
                                    log.error("transaction rollback failed, id = {}, host = {}", id, this.getHostId(), e);
                                    resultPromise.fail(e);
                                });
                    } else {
                        this.remoteInvoke(TransactionService.HTTP_PATH, context, form.toJson())
                                .onFailure(e -> {
                                    log.error("transaction rollback in remote failed, id = {}", id, e);
                                    resultPromise.fail(e);
                                })
                                .onSuccess(result -> {
                                    if (log.isDebugEnabled()) {
                                        log.error("transaction rollback in remote succeed, id = {}", id);
                                    }
                                    resultPromise.complete();
                                });
                    }
                    return resultPromise.future();
                })
                .onSuccess(r -> {
                    if (log.isDebugEnabled()) {
                        log.debug("transaction rollback succeed, id = {}, host = {}", id, this.getHostId());
                    }
                    handler.handle(Future.succeededFuture(new TransactionResult(id)));
                })
                .onFailure(e -> {
                    log.error("transaction rollback failed", e);
                    handler.handle(ServiceException.fail(500, "transaction rollback failed", new JsonObject().put("cause", e)));
                });
    }

}
