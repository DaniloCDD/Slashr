package com.slashr.bot;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CallbackRemoveTest {

    private static final String ID = "018d937f-1b2c-7a3d-8e4f-5a6b7c8d9e0f";

    @Test
    void formataEParseSaoInversos() {
        String data = CallbackRemove.formata(ID, 123456789L);

        assertThat(CallbackRemove.parse(data))
                .contains(new CallbackRemove(ID, 123456789L));
    }

    @Test
    void formataNormalizaIdParaMinusculo() {
        assertThat(CallbackRemove.formata(ID.toUpperCase(), 1L))
                .isEqualTo("rem:" + ID + ":1");
    }

    @Test
    void formataRecusaIdForaDoFormatoUuid() {
        assertThatThrownBy(() -> CallbackRemove.formata("nao-e-uuid", 1L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CallbackRemove.formata(null, 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parseIgnoraDadosInvalidos() {
        assertThat(CallbackRemove.parse(null)).isEmpty();
        assertThat(CallbackRemove.parse("")).isEmpty();
        assertThat(CallbackRemove.parse("add:" + ID + ":1")).isEmpty();
        assertThat(CallbackRemove.parse("rem:" + ID)).isEmpty();
        assertThat(CallbackRemove.parse("rem:" + ID + ":1:extra")).isEmpty();
        assertThat(CallbackRemove.parse("rem:1-1-1-1-1:1")).isEmpty();
        assertThat(CallbackRemove.parse("rem:" + ID + ":abc")).isEmpty();
        assertThat(CallbackRemove.parse("rem:" + ID + ":")).isEmpty();
        assertThat(CallbackRemove.parse("rem:" + ID + ":9223372036854775808")).isEmpty();
        assertThat(CallbackRemove.parse("rem:" + ID.toUpperCase() + ":1")).isEmpty();
    }
}
