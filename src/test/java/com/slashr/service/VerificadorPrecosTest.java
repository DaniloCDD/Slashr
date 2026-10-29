package com.slashr.service;

import com.slashr.collector.ItadException;
import com.slashr.collector.Offer;
import com.slashr.collector.PriceCollector;
import com.slashr.model.Game;
import com.slashr.model.PriceAlert;
import com.slashr.repository.PriceAlertRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificadorPrecosTest {

    private static final String ITAD_ID = "018d937f-1b2c-7a3d-8e4f-5a6b7c8d9e0f";
    private static final long CHAT_ID = -100123L;

    @Mock
    private PriceAlertRepository alertas;

    @Mock
    private PriceCollector coletor;

    @Mock
    private TelegramService telegram;

    @InjectMocks
    private VerificadorPrecos verificador;

    @Test
    void naoFazConsultasQuandoNaoExistemAlertas() {
        when(alertas.findAllComJogo()).thenReturn(List.of());

        verificador.verifica();

        verifyNoInteractions(coletor, telegram);
        verify(alertas, never()).saveAll(anyList());
    }

    @Test
    void interrompeRodadaQuandoItadFalhaAoBuscarPrecos() {
        PriceAlert alerta = alerta(ITAD_ID, false, null);
        when(alertas.findAllComJogo()).thenReturn(List.of(alerta));
        when(coletor.precos(anyList())).thenThrow(new ItadException(429, "limite excedido"));

        assertThatCode(() -> verificador.verifica()).doesNotThrowAnyException();

        verify(coletor, never()).precoHistorico(anyList());
        verify(alertas, never()).saveAll(anyList());
        verifyNoInteractions(telegram);
    }

    @Test
    void mantemAlertaIntactoQuandoJogoNaoVemNaRespostaDoItad() {
        PriceAlert alerta = alerta(ITAD_ID, false, null);
        when(alertas.findAllComJogo()).thenReturn(List.of(alerta));
        when(coletor.precos(anyList())).thenReturn(List.of());

        verificador.verifica();

        assertThat(alerta.isOnSale()).isFalse();
        assertThat(alerta.getLastNotifiedPriceCents()).isNull();
        verify(coletor, never()).precoHistorico(anyList());
        verify(alertas, never()).saveAll(anyList());
        verifyNoInteractions(telegram);
    }

    @Test
    void encerraPromocaoEZeraUltimoPrecoQuandoDescontoAcaba() {
        PriceAlert alerta = alerta(ITAD_ID, true, 1999);
        when(alertas.findAllComJogo()).thenReturn(List.of(alerta));
        when(coletor.precos(anyList())).thenReturn(List.of(oferta(ITAD_ID, 0)));

        verificador.verifica();

        assertThat(alerta.isOnSale()).isFalse();
        assertThat(alerta.getLastNotifiedPriceCents()).isNull();
        verify(alertas).saveAll(List.of(alerta));
        verify(coletor, never()).precoHistorico(anyList());
        verifyNoInteractions(telegram);
    }

    @Test
    void naoNotificaNemGravaEstadoSeHistoricoDoItadFalhar() {
        PriceAlert alerta = alerta(ITAD_ID, false, null);
        when(alertas.findAllComJogo()).thenReturn(List.of(alerta));
        when(coletor.precos(anyList())).thenReturn(List.of(oferta(ITAD_ID, 60)));
        when(coletor.precoHistorico(anyList())).thenThrow(new ItadException(500, "falha no histórico"));

        verificador.verifica();

        assertThat(alerta.isOnSale()).isFalse();
        assertThat(alerta.getLastNotifiedPriceCents()).isNull();
        verify(alertas, never()).saveAll(anyList());
        verifyNoInteractions(telegram);
    }

    @Test
    void naoGravaEstadoQuandoTelegramResponde429() {
        PriceAlert alerta = alerta(ITAD_ID, false, null);
        when(alertas.findAllComJogo()).thenReturn(List.of(alerta));
        when(coletor.precos(anyList())).thenReturn(List.of(oferta(ITAD_ID, 60)));
        when(coletor.precoHistorico(anyList())).thenReturn(Map.of(ITAD_ID, new BigDecimal("10.00")));
        doThrow(new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS))
                .when(telegram).sendHtml(anyString(), anyString());

        verificador.verifica();

        assertThat(alerta.isOnSale()).isFalse();
        assertThat(alerta.getLastNotifiedPriceCents()).isNull();
        verify(telegram).sendHtml(eq(Long.toString(CHAT_ID)), anyString());
        verify(alertas, never()).saveAll(anyList());
    }

    @Test
    void gravaEstadoSomenteDepoisQueTelegramAceitaMensagem() {
        PriceAlert alerta = alerta(ITAD_ID, false, null);
        when(alertas.findAllComJogo()).thenReturn(List.of(alerta));
        when(coletor.precos(anyList())).thenReturn(List.of(oferta(ITAD_ID, 60)));
        when(coletor.precoHistorico(anyList())).thenReturn(Map.of(ITAD_ID, new BigDecimal("10.00")));

        verificador.verifica();

        InOrder ordem = inOrder(telegram, alertas);
        ordem.verify(telegram).sendHtml(eq(Long.toString(CHAT_ID)), anyString());
        ordem.verify(alertas).saveAll(anyList());
        assertThat(alerta.isOnSale()).isTrue();
        assertThat(alerta.getLastNotifiedPriceCents()).isEqualTo(1999);
    }

    @Test
    void convertePrecoParaCentavosArredondandoMeioParaCima() {
        assertThat(VerificadorPrecos.emCentavos(new BigDecimal("19.99"))).isEqualTo(1999);
        assertThat(VerificadorPrecos.emCentavos(new BigDecimal("19.995"))).isEqualTo(2000);
    }

    private static PriceAlert alerta(String itadId, boolean emPromocao, Integer ultimoPreco) {
        Game jogo = new Game();
        jogo.setItadId(itadId);
        jogo.setTitle("Hades");

        PriceAlert alerta = new PriceAlert();
        alerta.setChatId(CHAT_ID);
        alerta.setTelegramUserId(42L);
        alerta.setFirstName("Ana");
        alerta.setUsername("ana");
        alerta.setGame(jogo);
        alerta.setOnSale(emPromocao);
        alerta.setLastNotifiedPriceCents(ultimoPreco);
        return alerta;
    }

    private static Offer oferta(String itadId, int desconto) {
        return new Offer(
                itadId,
                null,
                new BigDecimal("19.99"),
                new BigDecimal("49.99"),
                "BRL",
                desconto,
                "https://example.com/game"
        );
    }
}
