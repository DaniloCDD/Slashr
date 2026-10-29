package com.slashr.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CalculadoraVereditoTest {

    private static Optional<Veredito> v(String preco, String menor) {
        return CalculadoraVeredito.calcula(new BigDecimal(preco), new BigDecimal(menor));
    }

    @Test void noMenorHistoricoECompre() { assertEquals(Optional.of(Veredito.COMPRE), v("19.99", "19.99")); }
    @Test void escalaDiferenteNaoImporta() { assertEquals(Optional.of(Veredito.COMPRE), v("20", "20.00")); }
    @Test void limiteExatoDeCompreEntra() { assertEquals(Optional.of(Veredito.COMPRE), v("21.00", "20.00")); }
    @Test void logoAcimaDoLimiteCompreERazoavel() { assertEquals(Optional.of(Veredito.RAZOAVEL), v("21.01", "20.00")); }
    @Test void limiteExatoDeRazoavelEntra() { assertEquals(Optional.of(Veredito.RAZOAVEL), v("25.00", "20.00")); }
    @Test void acimaDeRazoavelEEspere() { assertEquals(Optional.of(Veredito.ESPERE), v("25.01", "20.00")); }
    @Test void limiteNaoArredondaParaCentavos() {
        assertEquals(Optional.of(Veredito.COMPRE), v("20.98", "19.99"));    // 20,9895
        assertEquals(Optional.of(Veredito.RAZOAVEL), v("20.99", "19.99"));
    }
    @Test void historicoZeroSoPrecoZeroECompre() {
        assertEquals(Optional.of(Veredito.COMPRE), v("0", "0"));
        assertEquals(Optional.of(Veredito.ESPERE), v("0.01", "0"));
    }
    @Test void semDadoSemVeredito() {
        assertEquals(Optional.empty(), CalculadoraVeredito.calcula(new BigDecimal("10"), null));
        assertEquals(Optional.empty(), CalculadoraVeredito.calcula(null, new BigDecimal("10")));
    }
}
