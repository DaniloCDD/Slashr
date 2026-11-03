package com.slashr.service;

/**
 * Decide o que fazer com um alerta, dado o estado gravado e a oferta atual. Função pura, sem banco nem rede.
 * ENCERRAR_PROMOCAO zera o estado quando o desconto acaba; sem isso, a próxima promoção não seria avisada.
 * Se o preço cair mais durante a mesma promoção, avisa de novo.
 */
public final class DecisaoAlerta {

    public enum Acao {
        NOTIFICAR,
        ENCERRAR_PROMOCAO,
        NADA
    }

    private DecisaoAlerta() {}

    /**
     * @param onSale        estado gravado: já avisamos uma promoção em andamento?
     * @param ultimoCents   último preço avisado, em centavos (null se nunca)
     * @param desconto      percentual atual (0 = sem promoção)
     * @param precoCents    preço atual, em centavos
     */
    public static Acao decide(boolean onSale, Integer ultimoCents, int desconto, int precoCents) {
        if (desconto <= 0) return onSale ? Acao.ENCERRAR_PROMOCAO : Acao.NADA;

        if (!onSale || ultimoCents == null) return Acao.NOTIFICAR;

        return precoCents < ultimoCents ? Acao.NOTIFICAR : Acao.NADA;
    }
}
