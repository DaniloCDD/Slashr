package com.slashr.collector;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Contrato da fonte de preços (hoje, o ITAD(IsThereAnyDeal)).
 */
public interface PriceCollector {

    List<GameMatch> busca(String nomeJogo);

    List<Offer> precos(List<String> gameIds);

    /**
     * Menor preço histórico na Steam, por id de jogo. Jogos sem dado ficam fora do mapa.
     */
    Map<String, BigDecimal> precoHistorico(List<String> gameIds);
}

