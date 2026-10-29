package com.slashr.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import static org.mockito.ArgumentMatchers.eq;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class TelegramServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private RestClient.Builder builder;
    private RestClient restClient;
    private TelegramService service;

    @BeforeEach
    void setUp() {
        builder = mock(RestClient.Builder.class);
        restClient = mock(RestClient.class, RETURNS_DEEP_STUBS);
        when(builder.baseUrl(anyString())).thenReturn(builder);
        when(builder.requestFactory(any(ClientHttpRequestFactory.class))).thenReturn(builder);
        when(builder.build()).thenReturn(restClient);
        service = new TelegramService(builder, "token-de-teste");
    }

    @Test
    void getUpdatesRetornaRespostaDoTelegram() throws Exception {
        JsonNode resposta = mapper.readTree("{\"ok\":true,\"result\":[]}");

        when(restClient.get()
                .uri(
                        eq("/getUpdates?offset={offset}&timeout={timeout}"),
                        any(Object[].class)
                )
                .retrieve()
                .body(JsonNode.class))
                .thenReturn(resposta);

        assertThat(service.getUpdates(7L)).isSameAs(resposta);
    }

    @Test
    void sendMessageRetornaIdDaMensagem() throws Exception {
        JsonNode resposta = mapper.readTree("{\"result\":{\"message_id\":123}}");
        when(restClient.post().uri("/sendMessage")
                .contentType(MediaType.APPLICATION_JSON).body(org.mockito.ArgumentMatchers.<Object>any())
                .retrieve().body(JsonNode.class)).thenReturn(resposta);

        assertThat(service.sendMessage("-100", "Olá", null)).isEqualTo(123L);
    }

    @Test
    void sendMessageRetornaZeroQuandoRespostaForNula() {
        when(restClient.post().uri("/sendMessage")
                .contentType(MediaType.APPLICATION_JSON).body(org.mockito.ArgumentMatchers.<Object>any())
                .retrieve().body(JsonNode.class)).thenReturn(null);

        assertThat(service.sendMessage("-100", "Olá", null)).isZero();
    }

    @Test
    void metodosDeEnvioUsamEndpointsEsperados() {
        service.answerCallbackQuery("callback-1", "Escolha inválida");
        service.editMessageText("-100", 45L, "Atualizada");
        service.sendHtml("-100", "<b>Promoção</b>");

        verify(restClient.post()).uri("/answerCallbackQuery");
        verify(restClient.post()).uri("/editMessageText");
        verify(restClient.post(), times(1)).uri("/sendMessage");
    }

    @Test
    void construtorConfiguraBaseUrlComTokenFornecido() {
        verify(builder).baseUrl(contains("token-de-teste"));
    }
}
