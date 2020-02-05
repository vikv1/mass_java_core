package edu.uw.bothell.css.dsl.MASS.monitoring;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import edu.uw.bothell.css.dsl.MASS.monitoring.models.FetchPlacesResponse;
import edu.uw.bothell.css.dsl.test.IntegrationTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class MonitoringClientTest {
    private static class FetchStatus implements WebSocket.Listener {
        @Override
        public void onOpen(WebSocket webSocket) {
            System.out.println("on open");

            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            ObjectMapper mapper = new ObjectMapper();

            mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
                    .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

            // MapType mapType = mapper.getTypeFactory().constructMapType(HashMap.class, Integer.class, FetchPlacesResponse.Message.class);

            FetchPlacesResponse response = null;

            try {
                response = mapper.readValue(data.toString(), FetchPlacesResponse.class);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            System.out.println("on open");
            return null;
        }

        @Override
        public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
            System.out.println("on open");
            return null;
        }

        @Override
        public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
            System.out.println("on open");
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            System.out.println("on open");
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            System.out.println(error);
        }
    }

    @Test
    @Category(IntegrationTest.class)
    public void testFetchStatus() {
        // TODO: where can we get the monitoring port
        String resource = "ws://krb.denki.io:8080";

        CountDownLatch latch = new CountDownLatch(1);

        FetchStatus listener = new FetchStatus();


        try {
            WebSocket socket = HttpClient.newHttpClient().newWebSocketBuilder()
                    .buildAsync(URI.create(resource), listener).join();

            socket.sendText("{ \"action\": \"FETCH\", \"handle\": \"PLACES\" }", false);

            latch.await(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
