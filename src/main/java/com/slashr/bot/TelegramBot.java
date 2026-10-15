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

//Essa classe é a que vai receber as mensagens e decidir o que fazer com cada uma, utilizando o TelegramService
//os comentários abaixo foram feitos por IA
/* Como funciona: usamos "long polling". Em vez de o Telegram avisar o nosso
 * servidor quando chega mensagem (webhook), NÓS ficamos perguntando
 * "tem novidade?". O Telegram segura a pergunta por até 30s e responde assim
 * que chega algo, então não é um loop frenético.
 * Por que SmartLifecycle e não @Scheduled?
 * O polling não é uma tarefa periódica, é um processo que fica rodando o tempo inteiro.
 * SmartLifecycle eh uma interface do Spring que da dois "ganchos":
 * start() é chamado quando a aplicação termina de subir e stop() quando ela
 * vai desligar. Assim o bot tem uma thread própria, e o @Scheduled fica livre
 * para o job de preços (que roda de hora em hora).
 */
@Component
@RequiredArgsConstructor // Lombok: gera o construtor com os campos "final" (injeção de dependência)
public class TelegramBot implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(TelegramBot.class);

    // ID do único grupo em que o bot responde. Vem da variável de ambiente
    // TELEGRAM_BOT_ALLOWED_CHAT_ID (ver application.properties).
    // Vazio = o bot ignora tudo. Não é "final" de propósito: o @Value em campo
    // não é injetado pelo construtor gerado pelo Lombok.
    @Value("${telegram.bot.allowed-chat-id:}")
    private String allowedChatId;

    private final TelegramService telegramService; // fala com a API do Telegram
    private final ObjectMapper objectMapper;       // lê JSON (usado no corpo do erro 429)

    // "volatile": o valor é escrito por uma thread (a que chama stop()) e lido
    // por outra (a do loop). Sem volatile, o loop poderia nunca enxergar a mudança.
    private volatile boolean running;
    private Thread thread;

    // Offset do Telegram: o id do último update que já tratamos. Pedimos sempre
    // "lastUpdateId + 1" para não receber de novo o que já foi processado.
    // ATENÇÃO: fica só em memória. Se o bot reiniciar, volta a 0 (futuramente
    // deve ir para o banco).
    private long lastUpdateId = 0;

    // ---------------------------------------------------------------------
    // Ciclo de vida (chamado automaticamente pelo Spring)
    // ---------------------------------------------------------------------

    @Override
    public void start() {
        running = true;
        thread = new Thread(this::loop, "telegram-polling"); // nome aparece em logs/dumps
        thread.setDaemon(true); // não impede a JVM de encerrar
        thread.start();
    }

    @Override
    public void stop() {
        running = false;
        // Acorda a thread se ela estiver em Thread.sleep. Se estiver no meio da
        // chamada HTTP, ela só termina quando a resposta chegar ou o timeout vencer.
        thread.interrupt();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    // ---------------------------------------------------------------------
    // Loop principal de polling
    // ---------------------------------------------------------------------

    private void loop() {
        // Contador de falhas seguidas: alimenta o backoff (espera crescente).
        // Zera sempre que uma chamada dá certo.
        int falhas = 0;
        while (running) {
            try {
                // Bloqueia até ~30s esperando novidades (long polling)
                JsonNode root = telegramService.getUpdates(lastUpdateId + 1);
                if (root == null || !root.path("ok").asBoolean()) {
                    log.warn("Resposta inválida do Telegram");
                    pausa(backoff(++falhas));
                    continue;
                }
                falhas = 0; // chamada OK: reinicia o backoff
                for (JsonNode update : root.path("result")) {
                    processUpdate(update);
                }
            } catch (RestClientResponseException e) {
                // O Telegram respondeu, mas com erro HTTP (4xx/5xx)
                if (e.getStatusCode().value() == 429) {
                    // Too Many Requests: o próprio Telegram diz quantos segundos esperar.
                    // Não entra no backoff, porque o tempo é ditado pelo servidor.
                    long segundos = retryAfter(e);
                    log.warn("Telegram pediu espera de {}s (429)", segundos);
                    pausa((segundos + 1) * 1000); // +1s de folga
                } else if (e.getStatusCode().value() == 409) {
                    // Conflict: só pode haver UM getUpdates por token. Acontece se
                    // duas instâncias (ex.: PC local + VM) usam o mesmo bot, ou se
                    // há um webhook registrado no token.
                    log.error("409 Conflict: outra instância está usando este token, ou há webhook configurado");
                    pausa(30_000);
                } else {
                    log.error("Telegram respondeu {}", e.getStatusCode());
                    pausa(backoff(++falhas));
                }
            } catch (RestClientException e) {
                // Não houve resposta: queda de rede, DNS, timeout...
                // Logamos só o nome da classe, nunca e.getMessage(): a mensagem
                // pode conter a URL da API, que inclui o TOKEN do bot.
                log.error("Falha de rede ({})", e.getClass().getSimpleName());
                pausa(backoff(++falhas));
            } catch (Exception e) {
                // Rede de segurança: nenhuma exceção pode matar a thread do bot.
                log.error("Erro inesperado no loop ({})", e.getClass().getSimpleName());
                pausa(backoff(++falhas));
            }
        }
    }

    /** Espera crescente entre tentativas: 1s, 2s, 4s, 8s, 16s, 32s e depois 60s (teto). */
    private long backoff(int falhas) {
        return Math.min(1000L << Math.min(falhas - 1, 6), 60_000L);
    }

    /** Lê "parameters.retry_after" do corpo do 429. Se não conseguir, assume 5s. */
    private long retryAfter(RestClientResponseException e) {
        try {
            JsonNode body = objectMapper.readTree(e.getResponseBodyAsString());
            return body.path("parameters").path("retry_after").asLong(5);
        } catch (Exception ex) {
            return 5;
        }
    }

    // ---------------------------------------------------------------------
    // Tratamento de cada update
    // ---------------------------------------------------------------------

    /**
     * Trata UM update isolado. Erros da nossa lógica ficam presos aqui e não
     * chegam ao loop (assim não são confundidos com falha de rede).
     */
    private void processUpdate(JsonNode update) {
        long id = update.path("update_id").asLong();
        try {
            if (!chatPermitido(update)) {
                return; // outro grupo/conversa: ignora (o finally ainda roda)
            }
            handleUpdate(update);
        } catch (Exception e) {
            log.error("Falha ao tratar update {} ({})", id, e.getClass().getSimpleName());
        } finally {
            // Avança o offset SEMPRE, com sucesso ou erro. Se não avançasse num erro,
            // o mesmo update voltaria para sempre e travaria o bot.
            // Consequência: um update que falha é perdido, então os comandos
            // (como o futuro /add) precisam ser idempotentes.
            lastUpdateId = id;
        }
    }

    /** Pausa que respeita interrupção (o stop() usa interrupt para acordar a thread). */
    private void pausa(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // restaura o sinal em vez de engoli-lo
        }
    }

    /**
     * Só deixa passar updates do grupo configurado. Cobre mensagens normais e
     * cliques em botão (callback_query), que trazem o chat em lugares diferentes.
     */
    private boolean chatPermitido(JsonNode update) {
        JsonNode msg = update.has("callback_query") ? update.path("callback_query").path("message")
                : update.path("message");
        String chatId = msg.path("chat").path("id").asText();
        return !allowedChatId.isBlank() && allowedChatId.equals(chatId);
    }

    // ---------------------------------------------------------------------
    // Despacho dos comandos
    // ---------------------------------------------------------------------

    private void handleUpdate(JsonNode update) {
        // Clique em botão inline: chega como "callback_query", não como "message"
        if (update.has("callback_query")) {
            handleCallback(update.path("callback_query"));
            return;
        }
        JsonNode message = update.path("message");
        // Ignora o que não é texto (foto, sticker, entrada de membro etc.)
        if (message.isMissingNode() || !message.has("text")) {
            return;
        }
        String chatId = message.path("chat").path("id").asText();
        String text = message.path("text").asText().trim();
        log.info("Mensagem recebida de {}: {}", chatId, text);

        // Pega só a primeira palavra e remove "@NomeDoBot" (no grupo, o Telegram
        // pode mandar "/start@seubot"). Ex.: "/Add@bot Hades" -> "/add"
        String command = text.split("\\s+")[0].split("@")[0].toLowerCase();

        switch (command) {
            case "/start" -> telegramService.sendMessage(chatId, "Mensagem de teste", null); // placeholder
            default -> {
                // comando desconhecido: ignorado em silêncio, por decisão do projeto
            }
        }
    }

    /**
     * Trata o clique num botão inline (a escolha entre jogos parecidos no /add).
     * O formato do callback_data é "add:<idDoJogo>:<idDoUsuárioQuePediu>".
     * (Limite do Telegram: 64 bytes, por isso vai só o ID, não o título.)
     */
    private void handleCallback(JsonNode cb) {
        String callbackId = cb.path("id").asText();
        String chatId = cb.path("message").path("chat").path("id").asText();
        long messageId = cb.path("message").path("message_id").asLong();
        long clickerId = cb.path("from").path("id").asLong(); // quem clicou
        String data = cb.path("data").asText();

        // O callback_data vem do cliente: valide sempre, não confie.
        String[] p = data.split(":");
        if (p.length != 3 || !p[0].equals("add")) {
            telegramService.answerCallbackQuery(callbackId, null);
            return;
        }
        // No grupo qualquer um pode clicar: só quem deu o /add pode escolher.
        if (!String.valueOf(clickerId).equals(p[2])) {
            telegramService.answerCallbackQuery(callbackId, "Só quem pediu pode escolher.");
            return;
        }
        // Sem essa resposta, o botão fica "girando" na tela do usuário.
        telegramService.answerCallbackQuery(callbackId, null);

        //  validar que p[1] é um id bem formado e GRAVAR o alerta no banco
        // (antes de editar a mensagem). Hoje é só um esqueleto.
        String itadId = p[1];
        telegramService.editMessageText(chatId, messageId, "Jogo adicionado.");
    }
}