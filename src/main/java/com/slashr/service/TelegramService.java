package com.slashr.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class TelegramService {

    private final RestClient restClient;

    public TelegramService(RestClient.Builder builder, @Value("${TELEGRAM_BOT_TOKEN}") String token) {
        this.restClient = builder
                .baseUrl("https://api.telegram.org/bot" + token)
                .build();
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