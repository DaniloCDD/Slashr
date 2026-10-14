package com.slashr.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;

@Service
public class TelegramService {

    private final RestClient restClient;

    public TelegramService(RestClient.Builder builder, @Value("${telegram.bot.token}") String token) {
        var factory = new JdkClientHttpRequestFactory();
        factory.setReadTimeout(Duration.ofSeconds(40));
        this.restClient = builder.baseUrl("https://api.telegram.org/bot" + token)
                .requestFactory(factory).build();
    }

    public JsonNode getUpdates(long offset) {
        return restClient.get()
                .uri("/getUpdates?offset={offset}&timeout={timeout}", offset, 30)
                .retrieve()
                .body(JsonNode.class);
    }

    public void sendMessage(String chatId, String text) {
        restClient.post()
                .uri("/sendMessage")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("chat_id", chatId, "text", text))
                .retrieve()
                .toBodilessEntity();
    }
}