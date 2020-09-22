package se.kry.codetest;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(VertxExtension.class)
public class TestMainVerticle {
  @BeforeEach
  void deploy_verticle(Vertx vertx, VertxTestContext testContext) {
    vertx.deployVerticle(new MainVerticle(), testContext.succeeding(id -> testContext.completeNow()));
  }

  @Test
  @DisplayName("Start a web server on localhost responding to GET /service on port 8080")
  @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
  void test_get_services(Vertx vertx, VertxTestContext testContext) {
    WebClient.create(vertx)
        .get(8080, "::1", "/service")
        .send(ar -> testContext.verify(() -> {
          assertEquals(200, ar.result().statusCode());
          testContext.completeNow();
        }));
  }

  @Test
  @DisplayName("Start a web server on localhost responding to POST /service on port 8080")
  @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
  void test_post_service(Vertx vertx, VertxTestContext testContext) {
    JsonObject service = new JsonObject();
    service.put("name", "testName");
    service.put("url", "https://www.test.com");

    WebClient.create(vertx)
        .post(8080, "::1", "/service")
        .as(BodyCodec.string())
        .sendJsonObject(service, ar -> testContext.verify(() -> {
            String body = ar.result().body();

            assertEquals(200, ar.result().statusCode());
            assertEquals("OK", body);
            testContext.completeNow();
        }));
  }

  @Test
  @DisplayName("Start a web server on localhost responding to DEL /service on port 8080")
  @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
  void test_delete_service(Vertx vertx, VertxTestContext testContext) {
    WebClient.create(vertx)
        .delete(8080, "::1", "/service/9999")
        .as(BodyCodec.string())
        .send(ar -> testContext.verify(() -> {
          String body = ar.result().body();

          assertEquals(404, ar.result().statusCode());
          assertEquals("Service not found", body);
          testContext.completeNow();
        }));
  }
}
