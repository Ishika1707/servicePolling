package se.kry.codetest;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import se.kry.codetest.model.Service;
import java.time.Instant;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(VertxExtension.class)
class BackgroundPollerTest {

    private BackgroundPoller toTest;
    private DBConnector mockConnector = Mockito.mock(DBConnector.class);

    @BeforeEach
    void before(Vertx vertx) throws Exception {
        Mockito.reset(mockConnector);
        toTest = new BackgroundPoller(mockConnector, WebClient.create(vertx));

    }

    @Test()
    void pollServicesTest(Vertx vertx) {
        //prepare
        when(mockConnector.getAllServices()).thenReturn(Future.succeededFuture(Collections.singletonList(new Service("GOOGLE", "https://google.com", "UNKNOWN", Instant.now()))));

        toTest.pollServices();
        assertEquals("COMPLETED" , toTest.pollServices());
    }

}