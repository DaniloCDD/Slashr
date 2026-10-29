package com.slashr.service;

import com.slashr.service.DecisaoAlerta.Acao;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DecisaoAlertaTest {

    @Test
    void primeiraPromocaoNotifica() {
        assertEquals(Acao.NOTIFICAR, DecisaoAlerta.decide(false, null, 50, 1999));
    }

    @Test
    void mesmaPromocaoNaoNotificaDeNovo() {
        assertEquals(Acao.NADA, DecisaoAlerta.decide(true, 1999, 50, 1999));
    }

    @Test
    void precoCaiuDuranteAPromocaoNotifica() {
        assertEquals(Acao.NOTIFICAR, DecisaoAlerta.decide(true, 1999, 60, 1499));
    }

    @Test
    void precoSubiuMasSegueEmPromocaoNaoNotifica() {
        assertEquals(Acao.NADA, DecisaoAlerta.decide(true, 1499, 40, 1999));
    }

    @Test
    void promocaoAcabouEncerra() {
        assertEquals(Acao.ENCERRAR_PROMOCAO, DecisaoAlerta.decide(true, 1999, 0, 3999));
    }

    @Test
    void semPromocaoESemEstadoNaoFazNada() {
        assertEquals(Acao.NADA, DecisaoAlerta.decide(false, null, 0, 3999));
    }

    @Test
    void depoisDeEncerrarANovaPromocaoNotifica() {
        assertEquals(Acao.NOTIFICAR, DecisaoAlerta.decide(false, null, 50, 1999));
    }
}
