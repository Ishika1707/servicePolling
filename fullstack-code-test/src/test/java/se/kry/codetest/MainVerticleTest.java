package se.kry.codetest;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(VertxExtension.class)
public class MainVerticleTest implements WithDBTest {

    @BeforeEach
    void deploy_verticle(Vertx vertx, VertxTestContext testContext) throws Exception {
        truncateServiceTable(vertx);
        DeploymentOptions options = new DeploymentOptions()
                .setConfig(new JsonObject()
                        .put("db.path", "test")
                );

        vertx.deployVerticle(new MainVerticle(), options, testContext.succeeding(id -> testContext.completeNow()));
    }


    @Test
    void start_http_server_and_get_services(Vertx vertx, VertxTestContext testContext) {
        JsonObject obj = new JsonObject().put("name", "google").put("url", "https://www.google.com/");

        WebClient.create(vertx)
                .post(8080, "::1", "/service")
                .sendJsonObject(obj, response -> testContext.verify(() -> {
                }));
        WebClient.create(vertx)
                .get(8080, "::1", "/service")
                .send(response -> testContext.verify(() -> {
                    assertEquals(200, response.result().statusCode());
                    JsonArray body = response.result().bodyAsJsonArray();
                    assertEquals(1, body.size());
                    testContext.completeNow();
                }));
    }


    @Test
    void start_http_server_and_add_service(Vertx vertx, VertxTestContext testContext) {
        JsonObject body = new JsonObject().put("name", "google").put("url", "https://www.google.com/");

        WebClient.create(vertx)
                .post(8080, "::1", "/service")
                .sendJsonObject(body, response -> testContext.verify(() -> {
                    assertEquals(200, response.result().statusCode());
                }));
        WebClient.create(vertx)
                .get(8080, "::1", "/service")
                .send(response -> testContext.verify(() -> {
                    assertEquals(200, response.result().statusCode());
                    JsonArray rbody = response.result().bodyAsJsonArray();
                    assertEquals(1, rbody.size());
                    JsonObject addedService = rbody.getJsonObject(0);
                    assertEquals("google", addedService.getString("name"));
                    assertEquals("https://www.google.com/", addedService.getString("url"));
                    testContext.completeNow();
                }));
    }



    @Test
    void start_http_server_and_add_service_no_url_in_body(Vertx vertx, VertxTestContext testContext) {
        JsonObject body = new JsonObject().put("name", "google");
        WebClient.create(vertx)
                .post(8080, "::1", "/service")
                .sendJsonObject(body, response -> testContext.verify(() -> {
                    assertEquals(400, response.result().statusCode());
                    JsonObject rbody = response.result().bodyAsJsonObject();
                    assertEquals("Invalid Service url", rbody.getString("error"));
                }));

        WebClient.create(vertx)
                .get(8080, "::1", "/service")
                .send(response -> testContext.verify(() -> {
                    assertEquals(200, response.result().statusCode());
                    JsonArray rbody = response.result().bodyAsJsonArray();
                    assertEquals(0, rbody.size());
                    testContext.completeNow();
                }));
    }
}