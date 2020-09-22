package se.kry.codetest;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;

public class ServiceRepository {
    private final DBConnector connector;

    public ServiceRepository(Vertx vertx) {
        this.connector = new DBConnector(vertx);
    }

    public Future<List<Service>> getServices() {
        String query = "SELECT * from service";
        Future<List<Service>> future = Future.future();

        connector
            .query(query)
            .setHandler(ar -> {
                if (ar.succeeded()) {
                    List<Service> services = ar
                        .result()
                        .getRows()
                        .stream()
                        .map(row -> { return io.vertx.core.json.Json.decodeValue(row.toString(), Service.class); })
                        .collect(Collectors.toList());
                    future.complete(services);
                } else {
                    future.fail(ar.cause());
                }
            });
        return future;
    }

    public Future<Void> createService(String name, String url) {
        JsonArray params = new JsonArray()
            .add(name)
            .add(url)
            .add("UNKNOWN")
            .add(Instant.now());
        String query = "INSERT INTO service (name, url, status, created_at) VALUES(?, ?, ?, ?);";
        Future<Void> future = Future.future();

        connector
            .update(query, params)
            .setHandler(ar -> {
                if (ar.succeeded()) {
                    future.complete();
                } else {
                    future.fail(ar.cause());
                }
            });
        return future;
    }

    public Future<Void> deleteService(Integer id) {
        JsonArray params = new JsonArray().add(id);
        String query = "DELETE FROM service WHERE id = ?;";
        Future<Void> future = Future.future();

        connector
            .update(query, params)
            .setHandler(ar -> {
                if (ar.result().getUpdated() > 0) {
                    future.complete();
                } else {
                    future.fail(ar.cause());
                }
            });
        return future;
    }

    public Future<Void> updateService(Integer id, String status) {
        JsonArray params = new JsonArray().add(status).add(id);
        String query = "UPDATE service SET status = ? WHERE id = ?;";
        Future<Void> future = Future.future();

        connector
            .update(query, params)
            .setHandler(ar -> {
                if (ar.succeeded()) {
                    future.complete();
                } else {
                    future.fail(ar.cause());
                }
            });
        return future;
    }
}
