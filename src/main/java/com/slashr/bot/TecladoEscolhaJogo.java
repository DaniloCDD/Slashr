package com.slashr.bot;

import com.slashr.collector.GameMatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Monta o teclado de botões (inline keyboard) da escolha de jogo no /add.
 * Só constrói os dados; quem envia é o TelegramService.
 */
public final class TecladoEscolhaJogo {

    // Botões muito longos ficam cortados na tela do Telegram de qualquer jeito.
    private static final int MAX_TEXTO = 60;

    private TecladoEscolhaJogo() {}


    /**
     * O Telegram espera uma lista de LINHAS, e cada linha é uma lista de botões.
     * Usamos um botão por linha porque títulos de jogo são longos.
     * O callback_data leva só os ids (limite de 64 bytes), não o título.
     */
    public static Map<String, Object> monta(List<GameMatch> jogos, long userId) {
        List<List<Map<String, Object>>> linhas = new ArrayList<>();
        for (GameMatch jogo : jogos) {
            linhas.add(List.of(Map.of(
                    "text", cortar(jogo.nomeJogo()),
                    "callback_data", CallbackAdd.formata(jogo.id(), userId))));
        }
        return Map.of("inline_keyboard", linhas);
    }

    private static String cortar(String titulo) {
        return titulo.length() <= MAX_TEXTO ? titulo : titulo.substring(0, MAX_TEXTO - 1) + "…";
    }
}
