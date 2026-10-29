package com.slashr.bot;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CallbackAddTest {

    private static final String ID = "018d937f-1b2c-7a3d-8e4f-5a6b7c8d9e0f";

    @Test
    void formataEParseSaoInversos() {
        String data = CallbackAdd.formata(ID, 123456789L);

        assertThat(CallbackAdd.parse(data)).contains(new CallbackAdd(ID, 123456789L));
    }

    @Test
    void formataNormalizaIdParaMinusculo() {
        String data = CallbackAdd.formata(ID.toUpperCase(), 1L);

        assertThat(data).isEqualTo("add:" + ID + ":1");
    }

    @Test
    void formataRecusaIdForaDoFormatoUuid() {
        assertThatThrownBy(() -> CallbackAdd.formata("nao-e-uuid", 1L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CallbackAdd.formata(null, 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parseIgnoraLixo() {
        assertThat(CallbackAdd.parse(null)).isEmpty();
        assertThat(CallbackAdd.parse("")).isEmpty();
        assertThat(CallbackAdd.parse(CallbackRemove.PREFIXO + ID + ":1")).isEmpty();      // prefixo de outro comando
        assertThat(CallbackAdd.parse(CallbackAdd.PREFIXO + ID)).isEmpty();                // faltou o usuário
        assertThat(CallbackAdd.parse(CallbackAdd.PREFIXO + ID + ":1:extra")).isEmpty();   // sobrou parte
        assertThat(CallbackAdd.parse(CallbackAdd.PREFIXO + ":1-1-1-1-1:1")).isEmpty();          // UUID "estranho" que o fromString aceitaria
        assertThat(CallbackAdd.parse(CallbackAdd.PREFIXO + ID + ":abc")).isEmpty();       // usuário não numérico
        assertThat(CallbackAdd.parse(CallbackAdd.PREFIXO + ID + ":")).isEmpty();          // usuário vazio
    }

    @Test
    void piorCasoCabeNoLimiteDe64BytesDoTelegram() {
        String data = CallbackAdd.formata(ID, Long.MAX_VALUE);

        assertThat(data.getBytes(StandardCharsets.UTF_8).length).isLessThanOrEqualTo(64);
    }
}
