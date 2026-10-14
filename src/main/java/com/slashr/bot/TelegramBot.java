package com.slashr.bot;

import com.fasterxml.jackson.databind.JsonNode;
import com.slashr.service.TelegramService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class TelegramBot {

    private static final Logger log = LoggerFactory.getLogger(TelegramBot.class);

    private final TelegramService telegramService;
    private long lastUpdateId = 0;

    @Scheduled(fixedDelay = 1000)
    public void listenForUpdates() {
        try {
            JsonNode root = telegramService.getUpdates(lastUpdateId + 1);

            if (root == null || !root.path("ok").asBoolean()) {
                log.warn("Resposta inválida do Telegram: {}", root);
                return;
            }

            for (JsonNode update : root.path("result")) {
                lastUpdateId = update.path("update_id").asLong();
                handleUpdate(update);
            }
        } catch (Exception e) {
            log.error("Erro ao buscar atualizações do Telegram", e);
        }
    }

    private void handleUpdate(JsonNode update) {
        JsonNode message = update.path("message");
        if (message.isMissingNode() || !message.has("text")) {
            return;
        }

        String chatId = message.path("chat").path("id").asText();
        String text = message.path("text").asText().trim();
        log.info("Mensagem recebida de {}: {}", chatId, text);

        String command = text.split("\\s+")[0].split("@")[0].toLowerCase();

        switch (command) {
            case "/start" -> telegramService.sendMessage(chatId,
                    "Mensagem de teste");
            default -> {}
        }
    }
}