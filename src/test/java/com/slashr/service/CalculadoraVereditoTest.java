package com.slashr.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CalculadoraVereditoTest {

    private static Optional<Veredito> v(String preco, String menor, String normal) {
        return CalculadoraVeredito.calcula(new BigDecimal(preco), new BigDecimal(menor), new BigDecimal(normal));
    }

    @Test void noMenorHistoricoECompre() { assertEquals(Optional.of(Veredito.COMPRE), v("19.99", "19.99", "39.99")); }
    @Test void escalaDiferenteNaoImporta() { assertEquals(Optional.of(Veredito.COMPRE), v("20", "20.00", "40")); }

    // faixa = 120 - 20 = 100; limites em 30,00 (10%) e 50,00 (30%)
    @Test void limiteExatoDeCompreEntra() { assertEquals(Optional.of(Veredito.COMPRE), v("30.00", "20.00", "120.00")); }
    @Test void logoAcimaDoLimiteCompreERazoavel() { assertEquals(Optional.of(Veredito.RAZOAVEL), v("30.01", "20.00", "120.00")); }
    @Test void limiteExatoDeRazoavelEntra() { assertEquals(Optional.of(Veredito.RAZOAVEL), v("50.00", "20.00", "120.00")); }
    @Test void acimaDeRazoavelEEspere() { assertEquals(Optional.of(Veredito.ESPERE), v("50.01", "20.00", "120.00")); }

    @Test void minimoMuitoBaixoNaoPenalizaPromocaoBoa() {
        // Borderlands 3: cheio 120,00, mínimo 5,99, atual 11,99 (~5% da faixa)
        assertEquals(Optional.of(Veredito.COMPRE), v("11.99", "5.99", "120.00"));
    }
    @Test void limiteNaoArredondaParaCentavos() {
        // faixa = 19,99; 10% = 1,999
        assertEquals(Optional.of(Veredito.COMPRE), v("21.99", "20.00", "39.99"));
        assertEquals(Optional.of(Veredito.RAZOAVEL), v("22.00", "20.00", "39.99"));
    }
    @Test void historicoZeroUsaAFaixaDoPrecoCheio() {
        assertEquals(Optional.of(Veredito.COMPRE), v("0", "0", "50"));
        assertEquals(Optional.of(Veredito.COMPRE), v("5.00", "0", "50"));
        assertEquals(Optional.of(Veredito.RAZOAVEL), v("5.01", "0", "50"));
    }
    @Test void faixaInconsistenteSemVeredito() {
        assertEquals(Optional.empty(), v("10", "5", "5"));
        assertEquals(Optional.empty(), v("10", "5", "4"));
    }
    @Test void semDadoSemVeredito() {
        BigDecimal dez = new BigDecimal("10");
        assertEquals(Optional.empty(), CalculadoraVeredito.calcula(dez, null, dez));
        assertEquals(Optional.empty(), CalculadoraVeredito.calcula(null, dez, dez));
        assertEquals(Optional.empty(), CalculadoraVeredito.calcula(dez, dez, null));
    }
}