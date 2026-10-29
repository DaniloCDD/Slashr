package com.slashr.scheduler;

import com.slashr.service.VerificadorPrecos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class PrecoSchedulerTest {

    private VerificadorPrecos verificador;
    private PrecoScheduler scheduler;

    @BeforeEach
    void setUp() {
        verificador = mock(VerificadorPrecos.class);
        scheduler = new PrecoScheduler(verificador);
    }

    @Test
    void rodaAcionaVerificadorUmaVez() {
        scheduler.roda();
        verify(verificador).verifica();
    }

    @Test
    void rodaAbsorveExcecaoDoVerificadorParaNaoDerrubarAgendamento() {
        doThrow(new IllegalStateException("falha simulada")).when(verificador).verifica();

        assertDoesNotThrow(() -> scheduler.roda());
        verify(verificador).verifica();
    }
}
