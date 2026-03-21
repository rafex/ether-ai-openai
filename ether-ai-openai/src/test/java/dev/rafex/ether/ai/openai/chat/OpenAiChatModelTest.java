package dev.rafex.ether.ai.openai.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import dev.rafex.ether.ai.core.chat.AiChatRequest;
import dev.rafex.ether.ai.core.message.AiMessage;
import dev.rafex.ether.ai.openai.config.OpenAiConfig;
import dev.rafex.ether.json.JsonUtils;

class OpenAiChatModelTest {

    private HttpServer server;
    private final AtomicReference<String> authorization = new AtomicReference<>();
    private final AtomicReference<String> requestBody = new AtomicReference<>();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.setExecutor(Executors.newSingleThreadExecutor());
        server.createContext("/v1/chat/completions", this::handleChat);
        server.start();
    }

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldCallOpenAiCompatibleChatCompletionEndpoint() throws Exception {
        final var config = new OpenAiConfig("test-key",
                URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/v1"), null, "org-test", "proj-test",
                null);
        final var model = new OpenAiChatModel(config);
        final var response = model
                .generate(new AiChatRequest("gpt-4.1-mini", java.util.List.of(AiMessage.user("ping")), 0.2d, 128));

        assertEquals("pong", response.text());
        assertEquals("stop", response.finishReason());
        assertEquals(15, response.usage().totalTokens());
        assertEquals("Bearer test-key", authorization.get());
        assertTrue(requestBody.get().contains("\"model\":\"gpt-4.1-mini\""));
        assertTrue(requestBody.get().contains("\"content\":\"ping\""));
    }

    private void handleChat(final HttpExchange exchange) throws IOException {
        authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
        requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));

        final var payload = java.util.Map.of("id", "chatcmpl-test", "model", "gpt-4.1-mini", "choices",
                java.util.List.of(java.util.Map.of("index", 0, "message",
                        java.util.Map.of("role", "assistant", "content", "pong"), "finish_reason", "stop")),
                "usage", java.util.Map.of("prompt_tokens", 12, "completion_tokens", 3, "total_tokens", 15));

        final byte[] response = JsonUtils.toJsonBytes(payload);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(response);
        }
    }
}
