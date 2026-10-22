package com.slashr.service;

import java.math.BigDecimal;
import java.util.Optional;

public final class CalculadoraVeredito {

    static final BigDecimal LIMITE_COMPRE = new BigDecimal("1.05");
    static final BigDecimal LIMITE_RAZOAVEL = new BigDecimal("1.25");

    private CalculadoraVeredito() {}

    /**
     * @return vazio quando não há menor histórico (ou preço) para comparar:
     *         sem dado, sem veredito (não inventamos).
     */

    public static Optional<Veredito> calcula(BigDecimal preco, BigDecimal menorHistorico) {
        if (preco == null || menorHistorico == null || preco.signum() < 0 || menorHistorico.signum() < 0) {
            return Optional.empty();
        }
        if (preco.compareTo(menorHistorico.multiply(LIMITE_COMPRE)) <= 0) {
            return Optional.of(Veredito.COMPRE);
        }
        if (preco.compareTo(menorHistorico.multiply(LIMITE_RAZOAVEL)) <= 0) {
            return Optional.of(Veredito.RAZOAVEL);
        }
        return Optional.of(Veredito.ESPERE);
    }
}
