package com.slashr.service;

import com.slashr.collector.Offer;
import com.slashr.service.MensagemPromocao.Mencao;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MensagemPromocaoTest {

    private static Offer oferta(String nome, String url) {
        return new Offer("id-1", nome, new BigDecimal("19.99"), new BigDecimal("39.99"), "BRL", 50, url);
    }

    @Test void formataDinheiroEmReais() {
        assertEquals("R$ 1.234,50", MensagemPromocao.dinheiro(new BigDecimal("1234.50"), "BRL"));
    }

    @Test void escapaTituloEMantemUrlSemAlterar() {
        String url = "https://isthereanydeal.com/game/x/?a=1&b=2";
        String msg = MensagemPromocao.formata(oferta("Tom & Jerry <3>", url), new BigDecimal("19.99"), List.of());
        assertTrue(msg.contains("Tom &amp; Jerry &lt;3&gt;"));
        assertTrue(msg.contains("href=\"https://isthereanydeal.com/game/x/?a=1&amp;b=2\""));
        assertTrue(msg.contains("IsThereAnyDeal"));
    }

    @Test void mostraNumerosDoVeredito() {
        String msg = MensagemPromocao.formata(oferta("Hades", "u"), new BigDecimal("15.00"), List.of());
        assertTrue(msg.contains("Razoável") || msg.contains("Espere"));
        assertTrue(msg.contains("33% acima do menor histórico"));
    }

    @Test void semHistoricoNaoTemVeredito() {
        String msg = MensagemPromocao.formata(oferta("Hades", "u"), null, List.of());
        assertTrue(msg.contains("sem dados"));
        assertFalse(msg.contains("Compre") || msg.contains("Razoável") || msg.contains("Espere"));
    }

    @Test void mencaoPorUsernameOuPorId() {
        assertEquals("@fulano", MensagemPromocao.mencao(new Mencao(1, "Fulano", "fulano")));
        assertEquals("<a href=\"tg://user?id=2\">Ana &amp; Cia</a>",
                MensagemPromocao.mencao(new Mencao(2, "Ana & Cia", null)));
    }
}
