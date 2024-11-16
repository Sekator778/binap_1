package org.sekator;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;

/**
 * @author Sekator
 * @created Nov 16, 2024
 */
public class BinanceWebSocketClientVirtual {

    private static final String BINANCE_WS_URL = "wss://stream.binance.com:9443/ws/btcusdt@trade";
    private static final Gson gson = new Gson();
    private static final Logger logger = LoggerFactory.getLogger(BinanceWebSocketClientVirtual.class);

    public static void main(String[] args) {
        HttpClient client = HttpClient.newHttpClient();

        Runnable websocketTask = () -> {
            client.newWebSocketBuilder()
                    .buildAsync(URI.create(BINANCE_WS_URL), new WebSocketClient())
                    .join();

            // Keep the virtual thread alive
            try {
                Thread.currentThread().join();
            } catch (InterruptedException e) {
                logger.error("WebSocket connection interrupted", e);
                Thread.currentThread().interrupt();
            }
        };

        // Start the WebSocket task in a virtual thread
        Thread virtualThread = Thread.ofVirtual().start(websocketTask);

        // Optionally, join the virtual thread to prevent the main thread from exiting
        try {
            virtualThread.join();
        } catch (InterruptedException e) {
            logger.error("Main thread interrupted", e);
            Thread.currentThread().interrupt();
        }
    }

    private static class WebSocketClient implements WebSocket.Listener {

        @Override
        public void onOpen(WebSocket webSocket) {
            logger.info("WebSocket connection established");
            webSocket.request(1); // Request the first message
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            // Handle the incoming message
            String message = data.toString();
            parseAndLogPrice(message);

            webSocket.request(1); // Request the next message
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            logger.error("WebSocket error: {}", error.getMessage(), error);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            logger.info("WebSocket closed, Code: {}, Reason: {}", statusCode, reason);
            return null;
        }

        private void parseAndLogPrice(String message) {
            try {
                JsonObject jsonObject = gson.fromJson(message, JsonObject.class);
                String price = jsonObject.get("p").getAsString();
                String tradeTime = jsonObject.get("T").getAsString();

                logger.info("Price: {} at Time: {}", price, tradeTime);
            } catch (Exception e) {
                logger.error("Failed to parse message: {}", message, e);
            }
        }
    }
}
