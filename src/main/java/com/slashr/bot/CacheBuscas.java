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


/**
 * Guarda em memória os resultados da busca do /add.
 * Evita consultar a API novamente quando o usuário escolhe o jogo.
 * Entradas expiram em 10 minutos e são limpas a cada nova gravação.
 */
@Component
public class CacheBuscas {

    private static final Duration VALIDADE = Duration.ofMinutes(10);

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

    public void remove(long chatId, long messageId) {
        entradas.remove(new Chave(chatId, messageId));
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
