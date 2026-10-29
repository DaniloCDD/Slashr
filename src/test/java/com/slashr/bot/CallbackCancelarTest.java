package com.slashr.bot;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CallbackCancelarTest {

    @Test
    void formataEParseSaoInversos() {
        assertThat(CallbackCancelar.parse(CallbackCancelar.formata(123456789L)))
                .contains(new CallbackCancelar(123456789L));
    }

    @Test
    void parseIgnoraLixo() {
        assertThat(CallbackCancelar.parse(null)).isEmpty();
        assertThat(CallbackCancelar.parse("")).isEmpty();
        assertThat(CallbackCancelar.parse("add:018d937f-1b2c-7a3d-8e4f-5a6b7c8d9e0f:1")).isEmpty();
        assertThat(CallbackCancelar.parse(CallbackCancelar.PREFIXO)).isEmpty();
        assertThat(CallbackCancelar.parse(CallbackCancelar.PREFIXO + ":1:extra")).isEmpty();
        assertThat(CallbackCancelar.parse(CallbackCancelar.PREFIXO + ":abc")).isEmpty();
        assertThat(CallbackCancelar.parse(CallbackCancelar.PREFIXO + ":")).isEmpty();
    }
}
