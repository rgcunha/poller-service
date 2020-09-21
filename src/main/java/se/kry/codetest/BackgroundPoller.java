package se.kry.codetest;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;

import java.net.URL;
import java.util.List;

enum Status {
  OK, FAIL, UNKNOWN;
}

public class BackgroundPoller {
  private WebClient webClient;
  private EventBus eb;

  BackgroundPoller(Vertx vertx) {
    webClient = WebClient.create(vertx);
    eb = vertx.eventBus();
  }

  private Future<Status> poll(URL url) {
    Future<Status> serviceStatusFuture = Future.future();

    webClient
      .get(url.getHost(), url.getPath())
      .timeout(5000)
      .send(asyncResult -> {
        if (asyncResult.succeeded()) {
          Integer statusCode = asyncResult.result().statusCode();
          String result = String.format("Status: %s | %s", url.toString(), statusCode);
          System.out.println(result);
          if (statusCode == 200) {
            serviceStatusFuture.complete(Status.OK);
          } else {
            serviceStatusFuture.complete(Status.FAIL);
          }
        } else {
          String result = String.format("Status: %s | %s", url.toString(), asyncResult.cause().getMessage());
          System.out.println(result);
          serviceStatusFuture.complete(Status.UNKNOWN);
        }
      });

    return serviceStatusFuture;
  }

  public void pollServices(List<Service> services) {
    System.out.println("Polling...");

    services.forEach(service -> {
      this.poll(service.getUrl()).setHandler(ar -> {
        if (ar.succeeded()) {
          JsonObject event = new JsonObject();
          event.put("id", service.getId());
          event.put("status", ar.result().toString());
          eb.publish("STATUS_UPDATED", event.toString());
        }
      });
    });
  }
}