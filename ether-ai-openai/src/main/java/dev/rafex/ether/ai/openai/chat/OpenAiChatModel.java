package dev.rafex.ether.ai.openai.chat;

/*-
 * #%L
 * ether-ai-openai
 * %%
 * Copyright (C) 2025 - 2026 Raúl Eduardo González Argote
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;

import dev.rafex.ether.ai.core.chat.AiChatModel;
import dev.rafex.ether.ai.core.chat.AiChatRequest;
import dev.rafex.ether.ai.core.chat.AiChatResponse;
import dev.rafex.ether.ai.core.error.AiHttpException;
import dev.rafex.ether.ai.core.message.AiMessage;
import dev.rafex.ether.ai.core.message.AiMessageRole;
import dev.rafex.ether.ai.core.usage.AiUsage;
import dev.rafex.ether.ai.openai.config.OpenAiConfig;
import dev.rafex.ether.json.JsonCodec;
import dev.rafex.ether.json.JsonUtils;

public final class OpenAiChatModel implements AiChatModel {

    private final OpenAiConfig config;
    private final HttpClient httpClient;
    private final JsonCodec jsonCodec;

    public OpenAiChatModel(final OpenAiConfig config) {
        this(config, HttpClient.newBuilder().connectTimeout(config.timeout()).build(), JsonUtils.codec());
    }

    public OpenAiChatModel(final OpenAiConfig config, final HttpClient httpClient, final JsonCodec jsonCodec) {
        this.config = Objects.requireNonNull(config, "config");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.jsonCodec = jsonCodec == null ? JsonUtils.codec() : jsonCodec;
    }

    @Override
    public AiChatResponse generate(final AiChatRequest request) throws IOException, InterruptedException {
        final byte[] payload = jsonCodec.toJsonBytes(toPayload(request));
        final var builder = HttpRequest.newBuilder(config.chatCompletionsUri()).timeout(config.timeout())
                .header("Authorization", "Bearer " + config.apiKey()).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(payload));

        if (!config.organization().isBlank()) {
            builder.header("OpenAI-Organization", config.organization());
        }
        if (!config.project().isBlank()) {
            builder.header("OpenAI-Project", config.project());
        }
        config.defaultHeaders().forEach(builder::header);

        final var response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new AiHttpException("OpenAI request failed with HTTP " + response.statusCode(), response.statusCode(),
                    new String(response.body(), StandardCharsets.UTF_8));
        }

        final JsonNode root = jsonCodec.readTree(response.body());
        final JsonNode choice = root.path("choices").path(0);
        final JsonNode messageNode = choice.path("message");
        final var message = new AiMessage(AiMessageRole.fromWireValue(text(messageNode, "role")),
                text(messageNode, "content"));
        return new AiChatResponse(text(root, "id"), text(root, "model"), message, text(choice, "finish_reason"),
                usage(root.path("usage")));
    }

    private static Map<String, Object> toPayload(final AiChatRequest request) {
        final var payload = new LinkedHashMap<String, Object>();
        payload.put("model", request.model());
        payload.put("messages", request.messages().stream()
                .map(message -> Map.of("role", message.role().wireValue(), "content", message.content())).toList());
        if (request.temperature() != null) {
            payload.put("temperature", request.temperature());
        }
        if (request.maxOutputTokens() != null) {
            payload.put("max_completion_tokens", request.maxOutputTokens());
        }
        return payload;
    }

    private static String text(final JsonNode node, final String fieldName) throws IOException {
        final JsonNode field = node.path(fieldName);
        if (field.isMissingNode() || field.isNull()) {
            throw new IOException("Missing JSON field: " + fieldName);
        }
        return field.asText();
    }

    private static AiUsage usage(final JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return AiUsage.empty();
        }
        return new AiUsage(node.path("prompt_tokens").asInt(0), node.path("completion_tokens").asInt(0),
                node.path("total_tokens").asInt(0));
    }
}
