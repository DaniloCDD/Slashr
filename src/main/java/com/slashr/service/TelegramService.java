package com.slashr.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Cliente da Bot API do Telegram.
 */
@Service
public class TelegramService {

    private final RestClient restClient;

    public TelegramService(RestClient.Builder builder, @Value("${telegram.bot.token}") String token) {
        var httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        var factory = new JdkClientHttpRequestFactory(httpClient);
        // 40s = 30s do long polling (getUpdates) + 10s de folga. Um valor menor derrubaria a conexão antes da resposta.
        factory.setReadTimeout(Duration.ofSeconds(40));
        this.restClient = builder.baseUrl("https://api.telegram.org/bot" + token)
                .requestFactory(factory).build();
    }

    /** Long polling: o Telegram segura a resposta até 30s esperando novas mensagens. */
    public JsonNode getUpdates(long offset) {
        return restClient.get()
                .uri("/getUpdates?offset={offset}&timeout={timeout}", offset, 30)
                .retrieve()
                .body(JsonNode.class);
    }


    public long sendMessage(String chatId, String text, Object replyMarkup) {
        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("text", text);
        if (replyMarkup != null) body.put("reply_markup", replyMarkup);
        JsonNode resposta = restClient.post().uri("/sendMessage")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body).retrieve().body(JsonNode.class);
        return resposta == null ? 0 : resposta.path("result").path("message_id").asLong();
    }

    public void answerCallbackQuery(String callbackId, String text) {
        Map<String, Object> body = new HashMap<>();
        body.put("callback_query_id", callbackId);
        if (text != null) body.put("text", text);
        restClient.post().uri("/answerCallbackQuery")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body).retrieve().toBodilessEntity();
    }

    public void editMessageText(String chatId, long messageId, String text) {
        restClient.post().uri("/editMessageText")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("chat_id", chatId, "message_id", messageId, "text", text))
                .retrieve().toBodilessEntity();
    }

    // Desativa a prévia de links para a mensagem de promoção não ficar poluída.
    public void sendHtml(String chatId, String html) {
        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("text", html);
        body.put("parse_mode", "HTML");
        body.put("link_preview_options", Map.of("is_disabled", true));
        restClient.post().uri("/sendMessage")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body).retrieve().toBodilessEntity();
    }
}
