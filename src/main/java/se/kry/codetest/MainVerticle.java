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

import java.net.URL;
import java.util.HashMap;

public class MainVerticle extends AbstractVerticle {
  private DBConnector connector;
  private BackgroundPoller poller;
  private HashMap<Integer, String> services = new HashMap<>();


  @Override
  public void start(Future<Void> startFuture) {
    connector = new DBConnector(vertx);
    poller = new BackgroundPoller(vertx);
    Router router = Router.router(vertx);

    // setup poller
    vertx.setPeriodic(1000 * 10, timerId -> {
      String query = "SELECT id, url from service";
      Future<ResultSet> queryResultFuture = connector.query(query);

      queryResultFuture.setHandler(asyncResult -> {
          if (asyncResult.succeeded()) {
            services.clear();
            asyncResult.result().getRows().forEach(result -> {
              services.put(result.getInteger("id"), result.getString("url"));
            });
            poller.pollServices(services);
          }
        });
    });

    // setup http server
    router.route().handler(BodyHandler.create());
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
            asyncResult.result().getRows().forEach(arr::add);
              sendSuccessResponse(req, arr.encode());
            } else {
              sendInternalError(req);
            }
          });
    });

    router.post("/service").handler(req -> {
      JsonObject jsonBody = req.getBodyAsJson();
      String url = jsonBody.getString("url");
      String name = jsonBody.getString("name");

      if(name == null || name.isEmpty()) {
        sendValidationError(req, "name");
        return;
      }

      if(!isValidUrl(url)) {
        sendValidationError(req, "url");
        return;
      }


      JsonArray params = new JsonArray().add(name).add(url).add("UNKNOWN");
      String query = "INSERT INTO service (name, url, status) VALUES(?, ?, ?);";

      Future<UpdateResult> updateResultFuture = connector.update(query, params);
      updateResultFuture.setHandler(asyncResult -> {
        if (asyncResult.succeeded()) {
          sendSuccessResponse(req);
        } else {
          sendInternalError(req);
        }
      });
    });

    router.delete("/service/:id").handler(req -> {
      String id = req.request().getParam("id");

      if(id == null || id.isEmpty()) {
        sendValidationError(req, "id");
        return;
      }

      JsonArray params = new JsonArray().add(id);
      String query = "DELETE FROM service WHERE id = ?;";

      Future<UpdateResult> updateResultFuture = connector.update(query, params);
      updateResultFuture.setHandler(asyncResult -> {
        if (asyncResult.succeeded()) {
          if (asyncResult.result().getUpdated() == 0) {
            sendNotFoundError(req);
          } else {
            sendSuccessResponse(req);
          }
        } else {
          sendInternalError(req);
        }
      });
    });
  }

  private boolean isValidUrl(String url) {
    try {
      new URL(url).toURI();
    } catch(Exception e) {
      return false;
    }
    return true;
  }

  private HttpServerResponse createPlainTextResponse(RoutingContext req) {
    return req.response().putHeader("content-type", "text/plain");
  }

  private HttpServerResponse createJsonResponse(RoutingContext req) {
    return req.response().putHeader("content-type", "application/json");
  }

  private void sendInternalError(RoutingContext req) {
    createPlainTextResponse(req).setStatusCode(500).end("FAILED");
  }

  private void sendValidationError(RoutingContext req, String paramName) {
    createPlainTextResponse(req).setStatusCode(400).end("Invalid or missing parameter: " + paramName);
  }

  private void sendNotFoundError(RoutingContext req) {
    createPlainTextResponse(req).setStatusCode(404).end("Service not found");
  }

  private void sendSuccessResponse(RoutingContext req) {
    createPlainTextResponse(req).end("OK");
  }

  private void sendSuccessResponse(RoutingContext req, String jsonPayload) {
    createJsonResponse(req).end(jsonPayload);
  }
}



