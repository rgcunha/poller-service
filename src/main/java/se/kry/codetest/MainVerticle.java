package se.kry.codetest;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

import java.net.URL;
import java.util.List;

public class MainVerticle extends AbstractVerticle {
  private BackgroundPoller poller;
  private ServiceRepository repository;

  @Override
  public void start(Future<Void> startFuture) {
    poller = new BackgroundPoller(vertx);
    Router router = Router.router(vertx);
    EventBus eb = vertx.eventBus();
    repository = new ServiceRepository(vertx);

    // setup event bus
    MessageConsumer<String> consumer = eb.consumer("STATUS_UPDATED");
    consumer.handler(message -> {
      JsonObject json = new JsonObject(message.body());
      Integer id = json.getInteger("id");
      String status = json.getString("status");
      repository.updateService(id, status);
    });

    // setup poller
    vertx.setPeriodic(1000 * 10, timerId -> {
      Future<List<Service>> future = repository.getServices();
      future.setHandler(ar -> {
          if (ar.succeeded()) {
            poller.pollServices(ar.result());
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

  private void setRoutes(Router router) {
    router.route("/*").handler(StaticHandler.create());
    router.get("/health").handler(req -> {
      sendInternalError(req);
    });

    router.get("/service").handler(req -> {
      Future<List<Service>> future = repository.getServices();
      future.setHandler(ar -> {
          if (ar.succeeded()) {
            JsonArray arr = new JsonArray(ar.result());
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

      Future<Void> future = repository.createService(name, url);
      future.setHandler(ar -> {
        if (ar.succeeded()) {
          sendSuccessResponse(req);
        } else {
          sendInternalError(req);
        }
      });
    });

    router.delete("/service/:id").handler(req -> {
      String id = req.request().getParam("id");

      if(!isNumeric(id)) {
        sendValidationError(req, "id");
        return;
      }

      Future<Void> future = repository.deleteService(Integer.parseInt(id));
      future.setHandler(ar -> {
        if (ar.succeeded()) {
          sendSuccessResponse(req);
        } else {
          sendNotFoundError(req);
        }
      });
    });
  }

  private static boolean isValidUrl(String url) {
    try {
      new URL(url).toURI();
    } catch(Exception e) {
      return false;
    }
    return true;
  }

  private static boolean isNumeric(String str) {
    if (str == null) {
      return false;
    }
    try {
      Integer.parseInt(str);
    } catch (NumberFormatException e) {
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



