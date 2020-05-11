package se.kry.codetest;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.api.RequestParameters;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class MainVerticle extends AbstractVerticle {

  //TODO use this
  private DBConnector connector;
  private BackgroundPoller poller;
  private static HashMap<String, String> services = new HashMap<>();
  public static final int SERVICE_POOLING_INTERVAL = 1000 * 20;
  private List<JsonObject> jsonServices = new ArrayList<>();
  private final Logger logger = LoggerFactory.getLogger(MainVerticle.class);

  @Override
  public void start(Future<Void> startFuture) {
    connector = new DBConnector(vertx);
    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());
    poller = new BackgroundPoller(connector, WebClient.create(vertx));
    getServices();
    //services.put("https://www.kry.se", "UNKNOWN");
    vertx.setPeriodic(1000 * 60, timerId -> poller.pollServices());
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
    router.get("/service").handler(req -> getServices().setHandler(done -> req.response()
              .putHeader("content-type", "application/json")
              .end(new JsonArray(jsonServices).encode()))
    );
    router.post("/service").handler(req -> {
      JsonObject jsonBody = req.getBodyAsJson();
      String checkedUrl = checkSqlInjection(jsonBody.getString("url"));
      String checkedName = checkSqlInjection(jsonBody.getString("name"));
      if (!isValidUrl(checkedUrl)) {
        System.out.println("Not a valid URL provided " + checkedUrl);
        return;
      }
      connector.query("INSERT INTO service (name, url, date) VALUES (?, ?, datetime('now', 'localtime'));",
              new JsonArray().add(checkedName).add(checkedUrl)).setHandler(done -> {
        if(done.succeeded()){
          req.response()
                  .putHeader("content-type", "text/plain")
                  .end("OK");
        } else {
          done.cause().printStackTrace();
        }
      });
    });
    router.delete("/service/:name").handler(req -> {
      RequestParameters parameters = req.get("parsedParameters");
      String serviceName = parameters.pathParameter("name").getString();
      Future<Boolean> done = Future.future();
      connector.query("DELETE FROM service WHERE name =?", new JsonArray().add(serviceName)).setHandler(res -> {
        if(res.succeeded()) {
          req.response().setStatusCode(202)
                  .putHeader("content-type", "text/plain")
                  .end("ACCEPTED");
        } else {
          done.cause().printStackTrace();
          req.response().setStatusCode(406)
                  .putHeader("content-type", "text/plain")
                  .end("NOT ACCEPTED");
        }
      });
      //services.remove(jsonBody.getString("url"));
    });
  }

  private Future<Boolean> getServices() {
    Future<Boolean> status = Future.future();
    connector.query("SELECT * FROM service;").setHandler(res -> {
      if (res.succeeded()) {
        List<JsonObject> storedServices = res.result().getRows();

        List<String> urls = jsonServices.stream()
                .map(service -> service.getString("url"))
                .collect(Collectors.toList());

        for (JsonObject service: storedServices) {
          String url = service.getString("url");
          if (!urls.contains(url)) {
            jsonServices.add(service);
          }
        }
        status.complete();
      } else {
        System.out.println("Fetching services failed: " + res.cause());
        status.failed();
      }
    });
    return status;
  }


  private String checkSqlInjection(String param) {
    return param == null ? null : param.replace(";", "");
  }

  private boolean isValidUrl(String value) {
    try {
      new URL(value).toURI();
      return true;
    } catch (Exception e) {
      return false;
    }
  }

}



