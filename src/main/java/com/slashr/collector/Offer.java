package com.slashr.collector;

import java.math.BigDecimal;

public record Offer(String gameId, String nomeJogo,
                    BigDecimal preco, BigDecimal precoNormal,
                    String moeda, int desconto, String url)
{}