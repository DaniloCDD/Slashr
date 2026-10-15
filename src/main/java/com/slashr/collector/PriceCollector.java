package com.slashr.collector;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/*
* esse aqui acho que é auto explicativo
* isso aqui se enquadra como um repository, mas colocamos nessa pasta pq ele cabe melhor aqui
* pq ele quem vai fazer as coletas
* */
public interface PriceCollector {

    //busca pelo nome, ai vai devolver id e o nome do jogo
    List<GameMatch> busca(String nomeJogo);

    //preco atual da steam de cada jogo que o user ta procurando informando o id
    List<Offer> precos(List<String> gameIds);

    //auto explicativo
    Map<String, BigDecimal> precoHistorico(List<String> gameIds);
}

