package com.slashr.service;

import com.slashr.collector.ItadException;
import com.slashr.collector.Offer;
import com.slashr.collector.PriceCollector;
import com.slashr.model.PriceAlert;
import com.slashr.repository.PriceAlertRepository;
import com.slashr.service.DecisaoAlerta.Acao;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Uma rodada de verificação de preços, chamada de hora em hora pelo PrecoScheduler.
 * Garantias:
 *  - jogo ausente na resposta do ITAD = sem dados: o alerta não é alterado;
 *  - falha do ITAD (inclusive 429) pula a rodada inteira;
 *  - o estado do alerta só é gravado depois que o Telegram aceita a mensagem.
 * Não é @Transactional de propósito, para não segurar conexão do banco durante chamadas HTTP.
 */
@Service
@RequiredArgsConstructor
public class VerificadorPrecos {

    private static final Logger log = LoggerFactory.getLogger(VerificadorPrecos.class);

    // O Telegram limita ~20 mensagens/minuto por grupo: 1 a cada 3s fica abaixo.
    private static final long PAUSA_ENTRE_ENVIOS_MS = 3_000;

    private final PriceAlertRepository alertas;
    private final PriceCollector coletor;
    private final TelegramService telegram;

    public void verifica() {
        List<PriceAlert> todos = alertas.findAllComJogo();
        if (todos.isEmpty()) {
            return;
        }

        Map<String, List<PriceAlert>> porJogo = todos.stream()
                .collect(Collectors.groupingBy(a -> a.getGame().getItadId(), LinkedHashMap::new, Collectors.toList()));

        // 1) preços atuais, em lote
        Map<String, Offer> ofertas;
        try {
            ofertas = coletor.precos(new ArrayList<>(porJogo.keySet())).stream()
                    .collect(Collectors.toMap(Offer::gameId, Function.identity(), (a, b) -> a));
        } catch (ItadException e) {
            log.warn("ITAD falhou (status {}); pulando a rodada", e.status());
            return;
        }

        /// 2) decide o que fazer com cada alerta
        List<PriceAlert> encerrar = new ArrayList<>();
        Map<String, List<PriceAlert>> aNotificar = new LinkedHashMap<>();

        for (var entrada : porJogo.entrySet()) {
            Offer oferta = ofertas.get(entrada.getKey());
            if (oferta == null) {
                continue; // sem dados não é "sem promoção",então mantém o alerta
            }
            int cents = emCentavos(oferta.preco());
            for (PriceAlert a : entrada.getValue()) {
                Acao acao = DecisaoAlerta.decide(a.isOnSale(), a.getLastNotifiedPriceCents(), oferta.desconto(), cents);
                switch (acao) {
                    case ENCERRAR_PROMOCAO -> {
                        // zera o estado para a próxima promoção voltar a ser avisada
                        a.setOnSale(false);
                        a.setLastNotifiedPriceCents(null);
                        encerrar.add(a);
                    }
                    case NOTIFICAR -> aNotificar.computeIfAbsent(entrada.getKey(), k -> new ArrayList<>()).add(a);
                    case NADA -> { }
                }
            }
        }
        if (!encerrar.isEmpty()) {
            alertas.saveAll(encerrar);
        }
        if (aNotificar.isEmpty()) {
            return;
        }

        // 3) menor histórico só dos jogos que vamos avisar
        Map<String, BigDecimal> historico;
        try {
            historico = coletor.precoHistorico(new ArrayList<>(aNotificar.keySet()));
        } catch (ItadException e) {
            log.warn("ITAD falhou ao buscar histórico (status {}); pulando a rodada", e.status());
            return;
        }

        // 4) uma mensagem por jogo e chat, mencionando todos os interessados
        for (var entrada : aNotificar.entrySet()) {
            String itadId = entrada.getKey();
            Offer oferta = ofertas.get(itadId);
            Map<String, List<PriceAlert>> porChat = entrada.getValue().stream()
                    .collect(Collectors.groupingBy(a -> String.valueOf(a.getChatId()), LinkedHashMap::new, Collectors.toList()));

            for (var chat : porChat.entrySet()) {
                List<PriceAlert> destinatarios = chat.getValue();
                String titulo = destinatarios.get(0).getGame().getTitle();
                List<MensagemPromocao.Mencao> mencoes = destinatarios.stream()
                        .map(a -> new MensagemPromocao.Mencao(a.getTelegramUserId(), a.getFirstName(), a.getUsername()))
                        .toList();
                String html = MensagemPromocao.formata(oferta.comNome(titulo), historico.get(itadId), mencoes);

                try {
                    telegram.sendHtml(chat.getKey(), html);
                } catch (RestClientResponseException e) {
                    if (e.getStatusCode().value() == 429) {
                        // limite do Telegram: o restante sai na próxima rodada
                        log.warn("Telegram 429 ao enviar; o resto fica para a próxima rodada");
                        return;
                    }
                    log.error("Telegram respondeu {} ao enviar promoção", e.getStatusCode());
                    continue; // não grava estado: tenta de novo na próxima hora
                } catch (RestClientException e) {
                    // nunca logar e.getMessage(): contém a URL com o token
                    log.error("Falha de rede ao enviar promoção ({})", e.getClass().getSimpleName());
                    continue;
                }

                // envio confirmado: só agora o estado é gravado
                int cents = emCentavos(oferta.preco());
                for (PriceAlert a : destinatarios) {
                    a.setOnSale(true);
                    a.setLastNotifiedPriceCents(cents);
                }
                alertas.saveAll(destinatarios);
                pausa();
            }
        }
    }

    // 19,99 vira 1999. BigDecimal evita os erros de arredondamento do double.
    static int emCentavos(BigDecimal preco) {
        return preco.movePointRight(2).setScale(0, RoundingMode.HALF_UP).intValueExact();
    }

    private void pausa() {
        try {
            Thread.sleep(PAUSA_ENTRE_ENVIOS_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}