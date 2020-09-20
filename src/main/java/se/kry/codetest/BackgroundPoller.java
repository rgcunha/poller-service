package se.kry.codetest;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

public class BackgroundPoller {
  private WebClient webClient;

  BackgroundPoller(Vertx vertx) {
    webClient = WebClient.create(vertx);
  }

  private void poll(String requestUrl) {
    URL url;
    try {
      url = new URL(requestUrl);
    } catch(MalformedURLException e) {
      System.out.println("Malformed url: " + requestUrl);
      return;
    }

    webClient
      .get(url.getHost(), url.getPath())
      .timeout(5000)
      .send(asyncResult -> {
        if (asyncResult.succeeded()) {
          String result = String.format("Status: %s | %s", requestUrl, asyncResult.result().statusCode());
          System.out.println(result);
        } else {
          String result = String.format("Status: %s | %s", requestUrl, asyncResult.cause().getMessage());
          System.out.println(result);
        }
      });
  }

  public Future<String> pollServices(Map<Integer, String> services) {
    services.forEach((id, url) -> this.poll(url));
    return Future.succeededFuture("Polling finished");
  }
}