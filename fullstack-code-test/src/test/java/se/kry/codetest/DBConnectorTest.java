package se.kry.codetest;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
public class DBConnectorTest implements WithDBTest {

    private DBConnector totest;

    @BeforeEach
    void init(Vertx vertx) throws Exception {
        truncateServiceTable(vertx);
        totest = new DBConnector(vertx);
    }

    @Test
    void getAllServices_should_return_nothing_if_db_is_empty(Vertx vertx, VertxTestContext testContext) {
        totest.query("SELECT * FROM service").setHandler(ar -> testContext.verify(() -> {
                    assertEquals(false, ar.failed());
                    assertEquals(0, ar.result().getRows().size());
                    testContext.completeNow();
                })
        );
    }

    @Test
    void getAllServices_should_return_service_if_db_is_not_empty(Vertx vertx, VertxTestContext testContext) throws Exception {
        execute("INSERT INTO service (name, url, status) VALUES('google', 'https://www.google.com', 'OK')");
        totest.query("SELECT * FROM service").setHandler(ar -> testContext.verify(() -> {

                    // assert
                    assertEquals(false, ar.failed());
                    List<JsonObject> services = ar.result().getRows();
                    assertEquals(1, services.size());
                    assertEquals("google", services.get(0).getString("name"));
                    assertEquals("https://www.google.com", services.get(0).getString("url"));
                    assertEquals("OK", services.get(0).getString("status"));
                    testContext.completeNow();
                })
        );
    }

    @Test
    void insertService_should_add_a_service_if_it_does_not_exist(Vertx vertx, VertxTestContext testContext) throws Exception {
        totest.query("INSERT INTO service(name, url, status) VALUES(?, ?, ?)", new JsonArray().add("google2").add("https://www.google.com").add("OK")).setHandler(ar -> testContext.verify(() -> {
                    // assert
                    assertEquals(false, ar.failed());
                    ResultSet updateResult = ar.result();
                    System.out.println(updateResult);
                    assertEquals(true, ar.succeeded());
                    testContext.completeNow();
                })
        );
    }


    @Test
    void deleteService_should_delete_a_service_if_it_exists(Vertx vertx, VertxTestContext testContext) throws Exception {
        execute("INSERT INTO service (name, url, status) VALUES('google3', 'https://www.google.com', 'OK')");
        totest.query("DELETE FROM service WHERE name =?;", new JsonArray().add("google3")).setHandler(ar -> testContext.verify(() -> {
                    // assert
                    assertEquals(false, ar.failed());
                    assertEquals(true, ar.succeeded());
                    testContext.completeNow();
                })
        );
    }

    @Test
    void deleteService_should_not_delete_a_service_if_it_does_not_exist(Vertx vertx, VertxTestContext testContext) throws Exception {
        execute("INSERT INTO service (name, url, status) VALUES('google2', 'https://www.google.com', 'FAIL')");
        totest.query("DELETE FROM service WHERE name LIKE (?);", new JsonArray().add("google2")).setHandler(ar -> testContext.verify(() -> {
                    // assert
                    assertEquals(false, ar.failed());
                    assertEquals(true, ar.succeeded());
                    testContext.completeNow();
                })
        );
    }

    @Test
    void updateAll_should_update_by_batch(Vertx vertx, VertxTestContext testContext) throws Exception {

        execute("INSERT INTO service (name, url, status) VALUES('google', 'https://www.google.com', 'FAIL')");
        totest.query("UPDATE service SET status=? where name like (?)", new JsonArray().add("OK").add("google")).setHandler(ar -> testContext.verify(() -> {
                    // assert
                    assertEquals(false, ar.failed());
                    assertEquals(true, ar.succeeded());
                    testContext.completeNow();
                })
        );
    }
}
