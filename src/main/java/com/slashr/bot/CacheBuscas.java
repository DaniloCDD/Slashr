package com.slashr.bot;

import com.slashr.collector.GameMatch;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/*
 * Guarda em memória o resultado de cada /add, por mensagem de botões enviada.
 * O botão só leva o id do jogo (limite de 64 bytes), então o título vem daqui quando o clique volta.
 * Some se o app reiniciar e expira em 15 min; nos dois casos o clique cai em "busca expirou".
 */
@Component
public class CacheBuscas {

    private static final Duration VALIDADE = Duration.ofMinutes(15);

    private record Chave(long chatId, long messageId) {}

    private record Entrada(Map<String, GameMatch> jogosPorId, Instant expiraEm) {}

    private final Map<Chave, Entrada> entradas = new ConcurrentHashMap<>();
    private final Clock relogio;

    public CacheBuscas() {
        this(Clock.systemUTC());
    }

    CacheBuscas(Clock relogio) {
        this.relogio = relogio;
    }

    public void guarda(long chatId, long messageId, List<GameMatch> jogos) {
        Instant agora = relogio.instant();
        entradas.values().removeIf(e -> e.expiraEm().isBefore(agora));

        Map<String, GameMatch> porId = new HashMap<>();
        for (GameMatch jogo : jogos) {
            porId.put(jogo.id().toLowerCase(), jogo);
        }
        entradas.put(new Chave(chatId, messageId), new Entrada(porId, agora.plus(VALIDADE)));
    }

    public Optional<GameMatch> acha(long chatId, long messageId, String itadId) {
        Entrada entrada = entradas.get(new Chave(chatId, messageId));
        if (entrada == null || entrada.expiraEm().isBefore(relogio.instant())) {
            return Optional.empty();
        }
        return Optional.ofNullable(entrada.jogosPorId().get(itadId.toLowerCase()));
    }
}
