package com.slashr.service;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Define o veredito comparando o preço atual com a faixa entre o menor preço histórico e o preço cheio.
 * A posição na faixa (0% = no menor histórico, 100% = sem desconto) não depende da escala do preço.
 */
public final class CalculadoraVeredito {

    // Posição máxima na faixa: até 10% = Compre; até 30% = Razoável; acima disso = Espere.
    static final BigDecimal LIMITE_COMPRE = new BigDecimal("0.10");
    static final BigDecimal LIMITE_RAZOAVEL = new BigDecimal("0.30");

    private CalculadoraVeredito() {}

    /**
     * @return vazio quando falta dado ou a faixa é inconsistente (preço cheio não maior que o menor histórico):
     *         sem dado confiável, sem veredito.
     */
    public static Optional<Veredito> calcula(BigDecimal preco, BigDecimal menorHistorico, BigDecimal precoNormal) {
        if (preco == null || menorHistorico == null || precoNormal == null
                || preco.signum() < 0 || menorHistorico.signum() < 0) {
            return Optional.empty();
        }
        if (preco.compareTo(menorHistorico) <= 0) {
            return Optional.of(Veredito.COMPRE);
        }
        BigDecimal faixa = precoNormal.subtract(menorHistorico);
        if (faixa.signum() <= 0) {
            return Optional.empty();
        }
        // Compara sem dividir, para não haver arredondamento nos limites.
        BigDecimal distancia = preco.subtract(menorHistorico);
        if (distancia.compareTo(faixa.multiply(LIMITE_COMPRE)) <= 0) {
            return Optional.of(Veredito.COMPRE);
        }
        if (distancia.compareTo(faixa.multiply(LIMITE_RAZOAVEL)) <= 0) {
            return Optional.of(Veredito.RAZOAVEL);
        }
        return Optional.of(Veredito.ESPERE);
    }
}
