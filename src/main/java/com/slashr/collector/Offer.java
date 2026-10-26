package com.slashr.collector;

import java.math.BigDecimal;

public record Offer(String gameId, String nomeJogo,
                    BigDecimal preco, BigDecimal precoNormal,
                    String moeda, int desconto, String url)
{
    public Offer comNome(String nome) {
        return new Offer(gameId, nome, preco, precoNormal, moeda, desconto, url);
    }
}
