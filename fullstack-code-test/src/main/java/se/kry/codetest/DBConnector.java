package se.kry.codetest;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.SQLConnection;
import se.kry.codetest.model.Service;

import java.util.List;
import java.util.stream.Collectors;

public class DBConnector {

  private final String DB_PATH = "poller.db";
  private final SQLClient client;
  private final Logger logger = LoggerFactory.getLogger(DBConnector.class);

  public DBConnector(Vertx vertx){
    JsonObject config = new JsonObject()
        .put("url", "jdbc:sqlite:" + DB_PATH)
        .put("driver_class", "org.sqlite.JDBC")
        .put("max_pool_size", 30);

    client = JDBCClient.createShared(vertx, config);
  }

  public Future<ResultSet> query(String query) {
    return query(query, new JsonArray());
  }


  public Future<ResultSet> query(String query, JsonArray params) {
    if(query == null || query.isEmpty()) {
      return Future.failedFuture("null or empty query");
    }
    if(!query.endsWith(";")) {
      query = query + ";";
    }

    Future<ResultSet> queryResultFuture = Future.future();

    client.queryWithParams(query, params, result -> {
      if(result.failed()){
        queryResultFuture.fail(result.cause());
      } else {
        queryResultFuture.complete(result.result());
      }
    });
    return queryResultFuture;
  }

  public Future<List<Integer>> updateAll(List<JsonArray> allServicesToUpdate) {
    Future<List<Integer>> updatesResultFuture = Future.future();

    client.getConnection(conn -> {
      if (conn.failed()) {
        Throwable error = conn.cause();
        logger.error("Connection cannot be eshtablished", error);
        updatesResultFuture.fail(error);
        return;
      }

      final SQLConnection connection = conn.result();
      connection.batchWithParams("UPDATE service SET status = ? WHERE rowid = ?;", allServicesToUpdate, result -> {
        if (result.failed()) {
          Throwable error = result.cause();
          logger.error("Failed to update all services" , error);
          updatesResultFuture.fail(error);
        } else {
          updatesResultFuture.complete(result.result());
        }
        closeConnection(connection);
      });
    });
    return updatesResultFuture;
  }

  private void closeConnection(SQLConnection conn) {
    conn.close(done -> {
      if (done.failed()) {
        throw new RuntimeException(done.cause());
      }
    });
  }

  public Future<List<Service>> getAllServices() {
    return query("SELECT * FROM service;")
            .map(
                    resultSet -> resultSet.getRows(true).stream()
                            .map(this::mapService)
                            .collect(Collectors.toList())
            );
  }

  /*
   * Map a row to a service
   */
  private Service mapService(JsonObject object){
    return new Service(
            object.getString("name"),
            object.getString("url"),
            object.getString("status"),
            object.getInstant("createdAt"));
  }



}
