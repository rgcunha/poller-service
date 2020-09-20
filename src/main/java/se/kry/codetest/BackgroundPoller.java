package se.kry.codetest;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

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

  private Future<Status> poll(String requestUrl) {
    URL url;
    try {
      url = new URL(requestUrl);
    } catch(MalformedURLException e) {
      return Future.failedFuture(e);
    }

    Future<Status> serviceStatusFuture = Future.future();

    webClient
      .get(url.getHost(), url.getPath())
      .timeout(5000)
      .send(asyncResult -> {
        if (asyncResult.succeeded()) {
          Integer statusCode = asyncResult.result().statusCode();
          String result = String.format("Status: %s | %s", requestUrl, statusCode);
          System.out.println(result);
          if (statusCode == 200) {
            serviceStatusFuture.complete(Status.OK);
          } else {
            serviceStatusFuture.complete(Status.FAIL);
          }
        } else {
          String result = String.format("Status: %s | %s", requestUrl, asyncResult.cause().getMessage());
          System.out.println(result);
          serviceStatusFuture.complete(Status.UNKNOWN);
        }
      });

    return serviceStatusFuture;
  }

  public void pollServices(HashMap<Integer, String> services) {
    System.out.println("Polling...");

    services.forEach((id, url) -> {
      this.poll(url).setHandler(ar -> {
        if (ar.succeeded()) {
          JsonObject event = new JsonObject();
          event.put("id", id);
          event.put("status", ar.result().toString());
          eb.publish("STATUS_UPDATED", event.toString());
        }
      });
    });
  }
}