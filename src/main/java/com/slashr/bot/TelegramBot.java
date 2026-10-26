package com.slashr.bot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.slashr.service.TelegramService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import com.slashr.collector.GameMatch;
import com.slashr.collector.ItadException;
import com.slashr.collector.PriceCollector;
import com.slashr.service.AlertaService;
import java.util.List;
import java.util.Optional;


@Component
@RequiredArgsConstructor
public class TelegramBot implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(TelegramBot.class);

    @Value("${telegram.bot.allowed-chat-id:}")
    private String allowedChatId;

    private final TelegramService telegramService;
    private final ObjectMapper objectMapper;
    private final PriceCollector priceCollector;
    private final CacheBuscas cacheBuscas;
    private final AlertaService alertaService;

    private volatile boolean running;
    private Thread thread;

    private long lastUpdateId = 0;

    @Override
    public void start() {
        running = true;
        thread = new Thread(this::loop, "telegram-polling");
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public void stop() {
        running = false;
        thread.interrupt();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    private void loop() {
        int falhas = 0;
        while (running) {
            try {
                JsonNode root = telegramService.getUpdates(lastUpdateId + 1);
                if (root == null || !root.path("ok").asBoolean()) {
                    log.warn("Resposta inválida do Telegram");
                    pausa(backoff(++falhas));
                    continue;
                }
                falhas = 0;
                for (JsonNode update : root.path("result")) {
                    processUpdate(update);
                }
            } catch (RestClientResponseException e) {
                if (e.getStatusCode().value() == 429) {
                    long segundos = retryAfter(e);
                    log.warn("Telegram pediu espera de {}s (429)", segundos);
                    pausa((segundos + 1) * 1000);
                } else if (e.getStatusCode().value() == 409) {
                    log.error("409 Conflict: outra instância está usando este token, ou há webhook configurado");
                    pausa(30_000);
                } else {
                    log.error("Telegram respondeu {}", e.getStatusCode());
                    pausa(backoff(++falhas));
                }
            } catch (RestClientException e) {
                log.error("Falha de rede ({})", e.getClass().getSimpleName());
                pausa(backoff(++falhas));
            } catch (Exception e) {
                log.error("Erro inesperado no loop ({})", e.getClass().getSimpleName());
                pausa(backoff(++falhas));
            }
        }
    }

    private long backoff(int falhas) {
        return Math.min(1000L << Math.min(falhas - 1, 6), 60_000L);
    }

    private long retryAfter(RestClientResponseException e) {
        try {
            JsonNode body = objectMapper.readTree(e.getResponseBodyAsString());
            return body.path("parameters").path("retry_after").asLong(5);
        } catch (Exception ex) {
            return 5;
        }
    }

    private void processUpdate(JsonNode update) {
        long id = update.path("update_id").asLong();
        try {
            if (!chatPermitido(update)) {
                return;
            }
            handleUpdate(update);
        } catch (Exception e) {
            log.error("Falha ao tratar update {} ({})", id, e.getClass().getSimpleName());
        } finally {
            lastUpdateId = id;
        }
    }

    private void pausa(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean chatPermitido(JsonNode update) {
        JsonNode msg = update.has("callback_query") ? update.path("callback_query").path("message")
                : update.path("message");
        String chatId = msg.path("chat").path("id").asText();
        return !allowedChatId.isBlank() && allowedChatId.equals(chatId);
    }

    private void handleUpdate(JsonNode update) {
        if (update.has("callback_query")) {
            handleCallback(update.path("callback_query"));
            return;
        }
        JsonNode message = update.path("message");
        if (message.isMissingNode() || !message.has("text")) {
            return;
        }
        String chatId = message.path("chat").path("id").asText();
        String text = message.path("text").asText().trim();
        log.info("Mensagem recebida de {}: {}", chatId, text);

        String command = text.split("\\s+")[0].split("@")[0].toLowerCase();

        long userId = message.path("from").path("id").asLong();

        switch (command) {
            case "/start" -> telegramService.sendMessage(chatId, "Mensagem de teste", null);
            case "/add"   -> handleAdd(chatId, userId, text);
            default -> { }
        }
    }

    private void handleAdd(String chatId, long userId, String text) {
        String[] partes = text.split("\\s+", 2);
        if (partes.length < 2 || partes[1].isBlank()) {
            telegramService.sendMessage(chatId, "Use: /add nome do jogo", null);
            return;
        }

        List<GameMatch> jogos;
        try {
            jogos = priceCollector.busca(partes[1].trim());
        } catch (ItadException e) {
            log.warn("ITAD falhou na busca (status {})", e.status());
            telegramService.sendMessage(chatId, "O ITAD não respondeu agora. Tente de novo em instantes.", null);
            return;
        }

        if (jogos.isEmpty()) {
            telegramService.sendMessage(chatId, "Não encontrei nenhum jogo com esse nome.", null);
            return;
        }
        long messageId = telegramService.sendMessage(chatId, "Qual destes? (só quem pediu pode escolher)",
                TecladoEscolhaJogo.monta(jogos, userId));
        cacheBuscas.guarda(Long.parseLong(chatId), messageId, jogos);
    }

    private void handleCallback(JsonNode cb) {
        String callbackId = cb.path("id").asText();

        Optional<CallbackAdd> parsed = CallbackAdd.parse(cb.path("data").asText());
        if (parsed.isEmpty()) {
            telegramService.answerCallbackQuery(callbackId, null);
            return;
        }
        CallbackAdd add = parsed.get();

        JsonNode from = cb.path("from");
        long clickerId = from.path("id").asLong();
        if (clickerId != add.userId()) {
            telegramService.answerCallbackQuery(callbackId, "Só quem pediu pode escolher.");
            return;
        }

        long chatId = cb.path("message").path("chat").path("id").asLong();
        long messageId = cb.path("message").path("message_id").asLong();
        String chat = String.valueOf(chatId);

        Optional<GameMatch> jogo = cacheBuscas.acha(chatId, messageId, add.itadId());
        if (jogo.isEmpty()) {
            telegramService.answerCallbackQuery(callbackId, null);
            telegramService.editMessageText(chat, messageId, "Essa busca expirou. Use /add de novo.");
            return;
        }
        telegramService.answerCallbackQuery(callbackId, null);

        String username = from.hasNonNull("username") ? from.path("username").asText() : null;
        AlertaService.Resultado resultado = alertaService.adiciona(
                chatId, clickerId, from.path("first_name").asText(""), username, jogo.get());

        String titulo = jogo.get().nomeJogo();
        String texto = resultado == AlertaService.Resultado.ADICIONADO
                ? "Acompanhando: " + titulo
                : "Você já acompanha: " + titulo;
        telegramService.editMessageText(chat, messageId, texto);
    }
}
