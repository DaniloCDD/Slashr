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


//é a classe que fala com o bot, ele monta as funções dele e envia, o TelegramBot que decide o que vai realmente usar
//os comentarios abaixo foi feito por IA
@Service
public class TelegramService {

    // RestClient: cliente HTTP do Spring. Já vem com a URL base (que contem o
    // token) configurada, então cada metodo só informa o final do caminho.
    private final RestClient restClient;

    public TelegramService(RestClient.Builder builder, @Value("${telegram.bot.token}") String token) {
        // connectTimeout: tempo máximo para ABRIR a conexão. Sem isso, uma rede
        // travada pode deixar a thread do bot esperando para sempre, sem erro.
        var httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        var factory = new JdkClientHttpRequestFactory(httpClient);
        // readTimeout: tempo máximo esperando a RESPOSTA. Precisa ser maior que
        // os 30s do long polling (getUpdates), senão chamadas normais seriam cortadas.
        factory.setReadTimeout(Duration.ofSeconds(40));
        // O token faz parte da URL (padrão da API do Telegram). Por isso NUNCA
        // logue a URL nem e.getMessage() de erros de requisição.
        this.restClient = builder.baseUrl("https://api.telegram.org/bot" + token)
                .requestFactory(factory).build();
    }

    /**
     * Busca mensagens novas (long polling).
     * offset: só traz updates com id >= offset (assim não repete os já tratados).
     * timeout=30: o Telegram segura a requisição até 30s se não houver novidade.
     */
    public JsonNode getUpdates(long offset) {
        return restClient.get()
                .uri("/getUpdates?offset={offset}&timeout={timeout}", offset, 30)
                .retrieve()
                .body(JsonNode.class); // devolve o JSON cru, navegado com path()/get()
    }

    /**
     * Envia mensagem de texto ao chat.
     * replyMarkup: opcional (pode ser null). Usado para botões inline.
     */
    public void sendMessage(String chatId, String text, Object replyMarkup) {
        // HashMap (e não Map.of) porque Map.of não aceita valores nulos
        // e aqui o reply_markup só entra quando existe.
        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("text", text);
        if (replyMarkup != null) body.put("reply_markup", replyMarkup);
        restClient.post().uri("/sendMessage")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body).retrieve().toBodilessEntity();
    }

    /**
     * Confirma ao Telegram que recebemos o clique no botão. Sem isso, o botão
     * fica "girando" na tela do usuário. O texto (opcional) aparece como um
     * aviso rápido para quem clicou.
     */
    public void answerCallbackQuery(String callbackId, String text) {
        Map<String, Object> body = new HashMap<>();
        body.put("callback_query_id", callbackId);
        if (text != null) body.put("text", text);
        restClient.post().uri("/answerCallbackQuery")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body).retrieve().toBodilessEntity();
    }

    /**
     * Troca o texto de uma mensagem já enviada. Como não mandamos reply_markup,
     * os botões somem (evita clique duplo depois da escolha).
     */
    public void editMessageText(String chatId, long messageId, String text) {
        restClient.post().uri("/editMessageText")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("chat_id", chatId, "message_id", messageId, "text", text))
                .retrieve().toBodilessEntity();
    }

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