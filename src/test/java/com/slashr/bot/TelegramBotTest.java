package com.slashr.bot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.slashr.collector.GameMatch;
import com.slashr.collector.ItadException;
import com.slashr.collector.PriceCollector;
import com.slashr.service.AlertaService;
import com.slashr.service.TelegramService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TelegramBotTest {

    private static final long CHAT_ID = -100123L;
    private static final long USER_ID = 42L;
    private static final long MESSAGE_ID = 55L;
    private static final String ID = "018d937f-1b2c-7a3d-8e4f-5a6b7c8d9e0f";

    private final ObjectMapper mapper = new ObjectMapper();
    private TelegramService telegram;
    private PriceCollector collector;
    private CacheBuscas cache;
    private AlertaService alertas;
    private TelegramBot bot;

    @BeforeEach
    void setUp() {
        telegram = mock(TelegramService.class);
        collector = mock(PriceCollector.class);
        cache = mock(CacheBuscas.class);
        alertas = mock(AlertaService.class);
        bot = new TelegramBot(telegram, mapper, collector, cache, alertas);
        ReflectionTestUtils.setField(bot, "allowedChatId", String.valueOf(CHAT_ID));
        when(telegram.sendMessage(anyString(), anyString(), any())).thenReturn(MESSAGE_ID);
    }

    @Test
    void ignoraMensagemDeChatNaoPermitido() throws Exception {
        process("""
                {"update_id":1,"message":{"chat":{"id":999},"text":"/start","from":{"id":42}}}
                """);
        verifyNoInteractions(telegram, collector, cache, alertas);
    }

    @Test
    void startEnviaMensagemDeBoasVindasEmHtml() throws Exception {
        process(message("/start"));
        verify(telegram).sendHtml(eq(String.valueOf(CHAT_ID)), contains("Slashr"));
    }

    @Test
    void addSemNomeMostraInstrucaoDeUso() throws Exception {
        process(message("/add   "));
        verify(telegram).sendMessage(String.valueOf(CHAT_ID), "Use: /add nome do jogo", null);
        verifyNoInteractions(collector);
    }

    @Test
    void addTrataFalhaDoItad() throws Exception {
        when(collector.busca("Hades")).thenThrow(new ItadException(429, "limite"));

        process(message("/add Hades"));

        verify(telegram).sendMessage(eq(String.valueOf(CHAT_ID)), contains("ITAD não respondeu"), isNull());
    }

    @Test
    void addInformaQuandoNenhumJogoFoiEncontrado() throws Exception {
        when(collector.busca("jogo inexistente")).thenReturn(List.of());

        process(message("/add jogo inexistente"));

        verify(telegram).sendMessage(String.valueOf(CHAT_ID), "Não encontrei nenhum jogo com esse nome.", null);
    }

    @Test
    void addMostraEscolhasEArmazenaBuscaNoCache() throws Exception {
        List<GameMatch> jogos = List.of(new GameMatch(ID, "Hades"));
        when(collector.busca("Hades")).thenReturn(jogos);

        process(message("/add Hades"));

        verify(telegram).sendMessage(eq(String.valueOf(CHAT_ID)), eq("Qual destes? (só quem pediu pode escolher)"), any());
        verify(cache).guarda(CHAT_ID, MESSAGE_ID, jogos);
    }

    @Test
    void listInformaQuandoPessoaAindaNaoAcompanhaJogos() throws Exception {
        when(alertas.alertasDaPessoa(CHAT_ID, USER_ID)).thenReturn(List.of());

        process(message("/list"));

        verify(telegram).sendMessage(eq(String.valueOf(CHAT_ID)), contains("Você ainda não acompanha nenhum jogo"), isNull());
    }

    @Test
    void listEnumeraJogosAcompanhados() throws Exception {
        when(alertas.alertasDaPessoa(CHAT_ID, USER_ID)).thenReturn(List.of(
                new GameMatch(ID, "Hades"), new GameMatch("118d937f-1b2c-7a3d-8e4f-5a6b7c8d9e0f", "Celeste")));

        process(message("/list"));

        verify(telegram).sendMessage(eq(String.valueOf(CHAT_ID)), argThat(text ->
                text.contains("1. Hades") && text.contains("2. Celeste")), isNull());
    }

    @Test
    void removeInformaQuandoNaoHaJogosAcompanhados() throws Exception {
        when(alertas.alertasDaPessoa(CHAT_ID, USER_ID)).thenReturn(List.of());

        process(message("/remove"));

        verify(telegram).sendMessage(String.valueOf(CHAT_ID), "Você não acompanha nenhum jogo.", null);
    }

    @Test
    void callbackAddRecusaCliqueDeOutroUsuario() throws Exception {
        process(callback("add:" + ID + ":" + USER_ID, USER_ID + 1));

        verify(telegram).answerCallbackQuery("cb-1", "Só quem pediu pode escolher.");
        verify(alertas, never()).adiciona(anyLong(), anyLong(), anyString(), any(), any(GameMatch.class));
    }

    @Test
    void callbackAddInformaQuandoBuscaExpirou() throws Exception {
        when(cache.acha(CHAT_ID, MESSAGE_ID, ID)).thenReturn(Optional.empty());

        process(callback("add:" + ID + ":" + USER_ID, USER_ID));

        verify(telegram).editMessageText(String.valueOf(CHAT_ID), MESSAGE_ID, "Essa busca expirou. Use /add de novo.");
        verify(alertas, never()).adiciona(anyLong(), anyLong(), anyString(), any(), any(GameMatch.class));
    }

    @Test
    void callbackAddSalvaAlertaEAtualizaMensagem() throws Exception {
        GameMatch jogo = new GameMatch(ID, "Hades");
        when(cache.acha(CHAT_ID, MESSAGE_ID, ID)).thenReturn(Optional.of(jogo));
        when(alertas.adiciona(CHAT_ID, USER_ID, "Ana", "ana", jogo)).thenReturn(AlertaService.Resultado.ADICIONADO);

        process(callback("add:" + ID + ":" + USER_ID, USER_ID, "ana"));

        verify(alertas).adiciona(CHAT_ID, USER_ID, "Ana", "ana", jogo);
        verify(telegram).editMessageText(String.valueOf(CHAT_ID), MESSAGE_ID, "Acompanhando: Hades");
    }

    @Test
    void callbackRemoveRemoveAlertaEAtualizaMensagem() throws Exception {
        when(alertas.tituloDoJogo(ID)).thenReturn(Optional.of("Hades"));
        when(alertas.remove(CHAT_ID, USER_ID, ID)).thenReturn(true);

        process(callback("rem:" + ID + ":" + USER_ID, USER_ID));

        verify(alertas).remove(CHAT_ID, USER_ID, ID);
        verify(telegram).editMessageText(String.valueOf(CHAT_ID), MESSAGE_ID, "Deixei de acompanhar Hades.");
    }

    @Test
    void callbackCancelarLimpaCacheEEditaMensagem() throws Exception {
        process(callback(CallbackCancelar.formata(USER_ID), USER_ID));

        verify(cache).remove(CHAT_ID, MESSAGE_ID);
        verify(telegram).editMessageText(String.valueOf(CHAT_ID), MESSAGE_ID, "Cancelado.");
    }

    private void process(String source) throws Exception {
        JsonNode update = mapper.readTree(source);
        ReflectionTestUtils.invokeMethod(bot, "processUpdate", update);
    }

    private String message(String text) {
        return """
                {"update_id":1,"message":{"chat":{"id":-100123},"text":"%s","from":{"id":42,"first_name":"Ana","username":"ana"}}}
                """.formatted(text);
    }

    private String callback(String data, long clickerId) {
        return callback(data, clickerId, "ana");
    }

    private String callback(String data, long clickerId, String username) {
        return """
                {"update_id":2,"callback_query":{"id":"cb-1","data":"%s","from":{"id":%d,"first_name":"Ana","username":"%s"},"message":{"message_id":55,"chat":{"id":-100123}}}}
                """.formatted(data, clickerId, username);
    }
}
