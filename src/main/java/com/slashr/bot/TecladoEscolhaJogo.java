package com.slashr.bot;

import com.slashr.collector.GameMatch;

import java.util.function.Function;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public final class TecladoEscolhaJogo {

    private static final int MAX_TEXTO = 60;

    private TecladoEscolhaJogo() {}

    public static Map<String, Object> monta(List<GameMatch> jogos, long userId) {
        return monta(jogos, id -> CallbackAdd.formata(id, userId));
    }

    public static Map<String, Object> montaRemocao(List<GameMatch> jogos, long userId) {
        return monta(jogos, id -> CallbackRemove.formata(id, userId));
    }

    private static Map<String, Object> monta(List<GameMatch> jogos, Function<String, String> formatador) {
        List<List<Map<String, Object>>> linhas = new ArrayList<>();
        for (GameMatch jogo : jogos) {
            linhas.add(List.of(Map.of(
                    "text", cortar(jogo.nomeJogo()),
                    "callback_data", formatador.apply(jogo.id()))));
        }
        return Map.of("inline_keyboard", linhas);
    }

    private static String cortar(String titulo) {
        return titulo.length() <= MAX_TEXTO ? titulo : titulo.substring(0, MAX_TEXTO - 1) + "…";
    }

}
