package se.kry.codetest;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.HttpResponse;
import java.util.stream.Collectors;

import io.vertx.core.logging.Logger;

public class BackgroundPoller {private final DBConnector connector;
  private final WebClient webClient;
  private final Logger logger = LoggerFactory.getLogger(BackgroundPoller.class);


  public BackgroundPoller(DBConnector connector, WebClient webClient) {
    this.connector = connector;
    this.webClient = webClient;
  }

  public String pollServices() {
    connector.getAllServices().setHandler(
            result -> {
              if (result.failed()) {
                logger.warn("Impossible to retrieve services from db");
              } else {
                CompositeFuture.join(result.result().stream().map(
                        service -> headService(service.getUrl())
                                .map(serviceStatus -> new JsonArray().add(serviceStatus))
                ).collect(Collectors.toList()))
                        .compose(
                                allEnded -> connector.updateAll(allEnded.result().list())
                        );
              }
            }
    );
    return "COMPLETED";
  }

  private Future<String> headService(final String url) {
    Future<String> statusFuture = Future.future();
    webClient
            .headAbs(url)
            .timeout(5000)
            .send(ar -> {
              if (ar.succeeded()) {
                final HttpResponse<Buffer> response = ar.result();
                final int status = response.statusCode();
                logger.debug("Received response for " + url + " with status code " + status);
                if (status < 400) {
                  statusFuture.complete("OK");
                } else {
                  statusFuture.complete("FAIL");
                }
              } else {
                logger.warn("Received no response for "  + url + " : status will be set to FAIL", ar.cause());
                statusFuture.complete("FAIL");
              }
            });

    return statusFuture;
  }
}
