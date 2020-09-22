package se.kry.codetest;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.UpdateResult;

public class DBConnector {

  private final String DB_PATH = "poller.db";
  private final SQLClient client;

  public DBConnector(Vertx vertx){
    JsonObject config = new JsonObject()
        .put("url", "jdbc:sqlite:" + DB_PATH)
        .put("driver_class", "org.sqlite.JDBC")
        .put("max_pool_size", 30);

    client = JDBCClient.createShared(vertx, config);
  }

  public Future<ResultSet> query(String query) {
    return query(query, new JsonArray());
  }

  public Future<ResultSet> query(String query, JsonArray params) {
    if(query == null || query.isEmpty()) {
      return Future.failedFuture("Query is null or empty");
    }
    String sanitizedQuery = sanitizeQuery(query);
    Future<ResultSet> queryResultFuture = Future.future();

    client.queryWithParams(sanitizedQuery, params, result -> {
      if(result.failed()){
        queryResultFuture.fail(result.cause());
      } else {
        queryResultFuture.complete(result.result());
      }
    });
    return queryResultFuture;
  }

  public Future<UpdateResult> update(String query) {
    return update(query, new JsonArray());
  }

  public Future<UpdateResult> update(String query, JsonArray params) {
    if(query == null || query.isEmpty()) {
      return Future.failedFuture("Query is null or empty");
    }
    String sanitizedQuery = sanitizeQuery(query);
    Future<UpdateResult> updateResultFuture = Future.future();

    client.updateWithParams(sanitizedQuery, params, result -> {
      if(result.failed()){
        updateResultFuture.fail(result.cause());
      } else {
        updateResultFuture.complete(result.result());
      }
    });
    return updateResultFuture;
  }

  private String sanitizeQuery(String query) {
    return !query.endsWith(";")
      ? query + ";"
      : query;
  }
}
