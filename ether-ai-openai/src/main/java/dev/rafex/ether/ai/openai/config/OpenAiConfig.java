package dev.rafex.ether.ai.openai.config;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

public record OpenAiConfig(String apiKey, URI baseUri, Duration timeout, String organization, String project,
        Map<String, String> defaultHeaders) {

    private static final URI DEFAULT_BASE_URI = URI.create("https://api.openai.com/v1/");
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    public OpenAiConfig {
        Objects.requireNonNull(apiKey, "apiKey");
        if (apiKey.isBlank()) {
            throw new IllegalArgumentException("apiKey must not be blank");
        }
        baseUri = normalizeBaseUri(baseUri == null ? DEFAULT_BASE_URI : baseUri);
        timeout = timeout == null ? DEFAULT_TIMEOUT : timeout;
        organization = organization == null ? "" : organization;
        project = project == null ? "" : project;
        defaultHeaders = defaultHeaders == null ? Map.of() : Map.copyOf(defaultHeaders);
    }

    public static OpenAiConfig of(final String apiKey) {
        return new OpenAiConfig(apiKey, DEFAULT_BASE_URI, DEFAULT_TIMEOUT, "", "", Map.of());
    }

    public URI chatCompletionsUri() {
        return baseUri.resolve("chat/completions");
    }

    private static URI normalizeBaseUri(final URI baseUri) {
        final String value = baseUri.toString();
        return URI.create(value.endsWith("/") ? value : value + "/");
    }
}
