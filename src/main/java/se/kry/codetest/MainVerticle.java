package se.kry.codetest;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

import java.util.HashMap;

public class MainVerticle extends AbstractVerticle {

  private HashMap<String, String> services = new HashMap<>();
  private DBConnector connector;
  private BackgroundPoller poller = new BackgroundPoller(vertx);

  @Override
  public void start(Future<Void> startFuture) {
    connector = new DBConnector(vertx);
    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());
    vertx.setPeriodic(1000 * 60, timerId -> poller.pollServices(services));
    setRoutes(router);
    vertx
        .createHttpServer()
        .requestHandler(router)
        .listen(8080, result -> {
          if (result.succeeded()) {
            System.out.println("KRY code test service started");
            startFuture.complete();
          } else {
            startFuture.fail(result.cause());
          }
        });
  }

  private void setRoutes(Router router){
    router.route("/*").handler(StaticHandler.create());
    router.get("/service").handler(req -> {
      String query = "SELECT * from service";

      Future<ResultSet> queryResultFuture = connector.query(query);
      queryResultFuture.setHandler(asyncResult -> {
          if (asyncResult.succeeded()) {
            JsonArray arr = new JsonArray();
            asyncResult.result().getRows().forEach(service -> {
              JsonObject jsonService = new JsonObject()
                .put("id", service.getInteger("id"))
                .put("name", service.getString("url"))
                .put("status", service.getString("status"));
              arr.add(jsonService);
            });
              sendSuccessResponse(req, arr.encode());
            } else {
              sendErrorResponse(req);
            }
          });
    });

    router.post("/service").handler(req -> {
      JsonObject jsonBody = req.getBodyAsJson();
      String url = jsonBody.getString("url");
      JsonArray params = new JsonArray().add(url).add("UNKNOWN");
      String query = "INSERT INTO service (url, status) VALUES(?, ?);";

      Future<UpdateResult> updateResultFuture = connector.update(query, params);
      updateResultFuture.setHandler(asyncResult -> {
        if (asyncResult.succeeded()) {
          sendSuccessResponse(req);
        } else {
          sendErrorResponse(req);
        }
      });
    });

    router.delete("/service/:id").handler(req -> {
      String id = req.request().getParam("id");
      JsonArray params = new JsonArray().add(id);
      String query = "DELETE FROM service WHERE id = ?;";

      Future<UpdateResult> updateResultFuture = connector.update(query, params);
      updateResultFuture.setHandler(asyncResult -> {
        if (asyncResult.succeeded()) {
          sendSuccessResponse(req);
        } else {
          sendErrorResponse(req);
        }
      });
    });
  }

  private HttpServerResponse createPlainTextResponse(RoutingContext req) {
    return req.response().putHeader("content-type", "text/plain");
  }

  private HttpServerResponse createJsonResponse(RoutingContext req) {
    return req.response().putHeader("content-type", "application/json");
  }

  private void sendErrorResponse(RoutingContext req) {
    createPlainTextResponse(req).end("FAILED");
  }

  private void sendSuccessResponse(RoutingContext req) {
    createPlainTextResponse(req).end("OK");
  }

  private void sendSuccessResponse(RoutingContext req, String jsonPayload) {
    createJsonResponse(req).end(jsonPayload);
  }
}



