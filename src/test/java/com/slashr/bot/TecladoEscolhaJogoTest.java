package com.slashr.bot;

import com.slashr.collector.GameMatch;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TecladoEscolhaJogoTest {

    private static final String ID = "018d937f-1b2c-7a3d-8e4f-5a6b7c8d9e0f";

    @SuppressWarnings("unchecked")
    private static List<List<Map<String, Object>>> linhas(Map<String, Object> teclado) {
        return (List<List<Map<String, Object>>>) teclado.get("inline_keyboard");
    }

    @Test
    void temUmBotaoPorJogoMaisOCancelarNaUltimaLinha() {
        var teclado = TecladoEscolhaJogo.monta(List.of(new GameMatch(ID, "Hades")), 42L);

        var linhas = linhas(teclado);
        assertThat(linhas).hasSize(2);
        assertThat(linhas.get(0).get(0))
                .containsEntry("text", "Hades")
                .containsEntry("callback_data", CallbackAdd.formata(ID, 42L));
        assertThat(linhas.get(1).get(0))
                .containsEntry("text", "Cancelar")
                .containsEntry("callback_data", CallbackCancelar.formata(42L));
    }

    @Test
    void cortaTituloLongoEm60Caracteres() {
        var teclado = TecladoEscolhaJogo.monta(List.of(new GameMatch(ID, "A".repeat(100))), 1L);

        assertThat(((String) linhas(teclado).get(0).get(0).get("text")).length()).isEqualTo(60);
    }

    @Test
    void montaTecladoDeRemocaoComCallbackRemoveECancelar() {
        var teclado = TecladoEscolhaJogo.montaRemocao(List.of(new GameMatch(ID, "Hades")), 42L);

        var linhas = linhas(teclado);
        assertThat(linhas).hasSize(2);
        assertThat(linhas.get(0).get(0))
                .containsEntry("text", "Hades")
                .containsEntry("callback_data", CallbackRemove.formata(ID, 42L));
        assertThat(linhas.get(1).get(0))
                .containsEntry("text", "Cancelar")
                .containsEntry("callback_data", CallbackCancelar.formata(42L));
    }

    @Test
    void tecladoDeRemocaoSemJogosMostraSomenteCancelar() {
        var teclado = TecladoEscolhaJogo.montaRemocao(List.of(), 42L);

        assertThat(linhas(teclado)).hasSize(1);
        assertThat(linhas(teclado).get(0).get(0))
                .containsEntry("text", "Cancelar")
                .containsEntry("callback_data", CallbackCancelar.formata(42L));
    }
}
