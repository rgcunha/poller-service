package se.kry.codetest;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(VertxExtension.class)
public class TestBackgroundPoller {
    BackgroundPoller poller;
    EventBus eb;

    @BeforeEach
    void deploy_verticle(Vertx vertx, VertxTestContext testContext) {
        vertx.deployVerticle(new MainVerticle(), testContext.succeeding(id -> testContext.completeNow()));
        poller = new BackgroundPoller(vertx);
        eb = vertx.eventBus();
    }

    @Test
    public void test_poller_publish_events(Vertx vertx, VertxTestContext testContext) throws MalformedURLException {
        List<Service> services = new ArrayList<Service>();
        services.add(new Service(1, "KRY-SE", "UNKNOWN", new URL("https://www.kry.se"), new Date()));

        MessageConsumer<String> consumer = eb.consumer("STATUS_UPDATED");
        consumer.handler(message -> {
            testContext.verify(() -> {
                JsonObject json = new JsonObject(message.body());

                assertEquals(Integer.valueOf(1), json.getInteger("id"));
                assertEquals("OK", json.getString("status"));
            });
            testContext.completeNow();
        });

        poller.pollServices(services);
    }
}
