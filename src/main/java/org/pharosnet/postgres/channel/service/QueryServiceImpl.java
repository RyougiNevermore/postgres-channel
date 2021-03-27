package org.pharosnet.postgres.channel.service;

import io.vertx.core.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.serviceproxy.ServiceException;
import io.vertx.sqlclient.*;
import lombok.extern.slf4j.Slf4j;
import org.pharosnet.postgres.channel.context.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
public class QueryServiceImpl extends AbstractServiceImpl implements QueryService {

    public QueryServiceImpl(Vertx vertx) {
        super(vertx);
    }

    @Override
    public void query(Context context, QueryForm form, Handler<AsyncResult<JsonArray>> handler) {
        String sql = Optional.ofNullable(form.getQuery()).orElse("").trim();
        if (sql.isBlank()) {
            handler.handle(ServiceException.fail(400, "query is empty"));
            return;
        }
        this.isLocal(context)
                .compose(local -> {
                    if (local) {
                        return this.getTransaction(context)
                                .compose(cachedTransaction -> {
                                    Promise<JsonArray> resultPromise = Promise.promise();
                                    if (cachedTransaction.isEmpty()) {
                                        PgPool pool;
                                        if (Optional.ofNullable(form.getSlaverMode()).orElse(false)) {
                                            pool = this.databases().getSlaver();
                                            if (pool == null) {
                                                resultPromise.fail("no slaver mode");
                                                return resultPromise.future();
                                            }
                                        } else {
                                            pool = this.databases().getNode();
                                        }
                                        PreparedQuery<RowSet<Row>> preparedQuery = pool.preparedQuery(sql);
                                        boolean batch = Optional.ofNullable(form.getBatch()).orElse(false);
                                        this.doQuery(preparedQuery, form.getArgs(), batch)
                                                .onFailure(e -> {
                                                    log.error("query in local failed, \nsql = {}\n arg = {}", sql, form.getArgs().encodePrettily(), e);
                                                    resultPromise.fail(e);
                                                })
                                                .onSuccess(rows -> {
                                                    JsonArray array = new JsonArray();
                                                    for (Row row : rows) {
                                                        array.add(row.toJson());
                                                    }
                                                    if (log.isDebugEnabled()) {
                                                        log.error("query in local succeed, \nsql = {} \narg = {} \nresults = {}", sql, form.getArgs().encodePrettily(), array.encodePrettily());
                                                    }
                                                    resultPromise.complete(array);
                                                });
                                    } else {
                                        // tx query
                                        SqlConnection connection = cachedTransaction.get().getConnection();
                                        PreparedQuery<RowSet<Row>> preparedQuery = connection.preparedQuery(sql);
                                        boolean batch = Optional.ofNullable(form.getBatch()).orElse(false);
                                        this.doQuery(preparedQuery, form.getArgs(), batch)
                                                .onFailure(e -> {
                                                    log.error("query in local transaction failed, \nsql = {}\n arg = {}", sql, form.getArgs().encodePrettily(), e);
                                                    resultPromise.fail(e);
                                                })
                                                .onSuccess(rows -> {
                                                    JsonArray array = new JsonArray();
                                                    for (Row row : rows) {
                                                        array.add(row.toJson());
                                                    }
                                                    if (log.isDebugEnabled()) {
                                                        log.error("query in local transaction succeed, \nsql = {} \narg = {} \nresults = {}", sql, form.getArgs().encodePrettily(), array.encodePrettily());
                                                    }
                                                    resultPromise.complete(array);
                                                });
                                    }
                                    return resultPromise.future();
                                });
                    } else {
                        // remote invoke
                        Promise<JsonArray> resultPromise = Promise.promise();
                        this.remoteInvoke(QueryService.HTTP_PATH, context, form.toJson())
                                .onFailure(e -> {
                                    log.error("query in remote failed, \nsql = {}\n arg = {}", sql, form.getArgs().encodePrettily(), e);
                                    resultPromise.fail(e);
                                })
                                .onSuccess(result -> {
                                    if (log.isDebugEnabled()) {
                                        log.error("query in remote succeed, \nsql = {} \narg = {} \nresults = {}", sql, form.getArgs().encodePrettily(), result.encodePrettily());
                                    }
                                    resultPromise.complete(result);
                                });
                        return resultPromise.future();
                    }
                })
                .onFailure(e -> {
                    handler.handle(ServiceException.fail(555, "query failed", new JsonObject().put("cause", e)));
                })
                .onSuccess(result -> {
                    handler.handle(Future.succeededFuture(result));
                });

    }

    private Future<RowSet<Row>> doQuery(PreparedQuery<RowSet<Row>> preparedQuery, JsonArray args, boolean batch) {
        if (args != null && !args.isEmpty()) {
            if (batch) {
                List<Tuple> tuples = new ArrayList<>();
                for (int i = 0; i < args.size(); i++) {
                    JsonArray line = args.getJsonArray(i);
                    tuples.add(Tuple.from(line.getList()));
                }
                return preparedQuery.executeBatch(tuples);
            } else {
                Tuple tuple = Tuple.from(args.getList());
                return preparedQuery.execute(tuple);
            }
        } else {
            return preparedQuery.execute();
        }
    }

}
