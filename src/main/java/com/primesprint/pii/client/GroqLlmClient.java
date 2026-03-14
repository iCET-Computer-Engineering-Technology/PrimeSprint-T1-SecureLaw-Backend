package com.primesprint.pii.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class GroqLlmClient {
    private final WebClient webClient;
    private final String model;
    private final String chatEndpoint;
    private final Duration timeout;

    public GroqLlmClient(@Value("${llm.base-url}") String baseUrl,
                         @Value("${llm.api-key}") String apiKey,
                         @Value("${llm.model}") String model,
                         @Value("${llm.chat-endpoint}") String chatEndpoint,
                         @Value("${llm.timeout-ms}") long timeoutMs) {
        this.model = model;
        this.chatEndpoint = chatEndpoint;
        this.timeout = Duration.ofMillis(timeoutMs);
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Calls Groq/OpenAI-style chat completions.
     * Returns the raw JSON response body (we parse and extract content in service).
     */
    public Mono<String> chatRaw(String systemPrompt, String text) {
        Map<String, Object> payload = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", text)
                )
        );

        return webClient.post()
                .uri(chatEndpoint)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(timeout);
    }
}

