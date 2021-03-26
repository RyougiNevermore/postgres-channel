package org.pharosnet.postgres.channel.service;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import lombok.extern.slf4j.Slf4j;
import org.pharosnet.postgres.channel.context.Context;

@Slf4j
public class QueryServiceImpl extends AbstractServiceImpl implements QueryService {

    public QueryServiceImpl(Vertx vertx) {
        super(vertx);
    }


    @Override
    public void query(Context context, QueryForm form, Handler<AsyncResult<JsonArray>> handler) {
        // get context host in sharedData


        // if local do

        // if others found and dispatch
    }


}
