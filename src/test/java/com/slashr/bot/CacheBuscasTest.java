package com.slashr.bot;

import com.slashr.collector.GameMatch;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CacheBuscasTest {

    private static final String ID_A = "018d937f-1b2c-7a3d-8e4f-5a6b7c8d9e0f";
    private static final String ID_B = "018d937f-aaaa-7a3d-8e4f-5a6b7c8d9e0f";

    private static class RelogioManual extends Clock {
        private Instant agora = Instant.parse("2026-01-01T00:00:00Z");

        void avanca(Duration d) { agora = agora.plus(d); }

        @Override public Instant instant() { return agora; }
        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
    }

    private final RelogioManual relogio = new RelogioManual();
    private final CacheBuscas cache = new CacheBuscas(relogio);

    @Test
    void achaOJogoGuardadoNaMesmaMensagem() {
        cache.guarda(-100L, 7L, List.of(new GameMatch(ID_A, "Hades"), new GameMatch(ID_B, "Hades II")));

        assertThat(cache.acha(-100L, 7L, ID_B)).contains(new GameMatch(ID_B, "Hades II"));
    }

    @Test
    void idEmMaiusculoCasaComIdGuardado() {
        cache.guarda(-100L, 7L, List.of(new GameMatch(ID_A.toUpperCase(), "Hades")));

        assertThat(cache.acha(-100L, 7L, ID_A)).isPresent();
    }

    @Test
    void naoMisturaMensagensNemChats() {
        cache.guarda(-100L, 7L, List.of(new GameMatch(ID_A, "Hades")));

        assertThat(cache.acha(-100L, 8L, ID_A)).isEmpty();
        assertThat(cache.acha(-200L, 7L, ID_A)).isEmpty();
    }

    @Test
    void jogoQueNaoEstavaNaBuscaNaoEhEncontrado() {
        cache.guarda(-100L, 7L, List.of(new GameMatch(ID_A, "Hades")));

        assertThat(cache.acha(-100L, 7L, ID_B)).isEmpty();
    }

    @Test
    void expiraDepoisDe10Minutos() {
        cache.guarda(-100L, 7L, List.of(new GameMatch(ID_A, "Hades")));

        relogio.avanca(Duration.ofMinutes(10));
        assertThat(cache.acha(-100L, 7L, ID_A)).isPresent();

        relogio.avanca(Duration.ofMinutes(2));
        assertThat(cache.acha(-100L, 7L, ID_A)).isEmpty();
    }
}
