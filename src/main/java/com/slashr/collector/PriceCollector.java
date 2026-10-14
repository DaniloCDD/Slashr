package com.slashr.collector;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface PriceCollector {
    List<GameMatch> busca(String nomeJogo);
    List<Offer> precos(List<String> gameIds);
    Map<String, BigDecimal> precoHistorico(List<String> gameIds);
}

