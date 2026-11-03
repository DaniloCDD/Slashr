package com.slashr.collector;

import java.math.BigDecimal;

/**
 * Oferta atual de um jogo na Steam. O desconto é percentual (0 = sem promoção).
 */
public record Offer(String gameId, String nomeJogo,
                    BigDecimal preco, BigDecimal precoNormal,
                    String moeda, int desconto, String url) {

    /**
     * O ITAD não devolve o título nos preços; ele é preenchido a partir do banco.
     */
    public Offer comNome(String nome) {
        return new Offer(gameId, nome, preco, precoNormal, moeda, desconto, url);
    }
}
