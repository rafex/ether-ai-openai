package dev.rafex.ether.ai.openai.config;

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
