package com.slashr.collector;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementação do PriceCollector sobre a API do IsThereAnyDeal, restrita à loja Steam.
 */
@Component
public class ItadPriceCollector implements PriceCollector {

    private static final String BASE_URL = "https://api.isthereanydeal.com";
    private static final int MAX_BUSCA = 10;
    //Tamanho máximo de ids por requisição de preços.
    private static final int LOTE = 200;

    private final RestClient http;
    private final String pais;
    private final int lojaSteam;

    public ItadPriceCollector(RestClient.Builder builder,
                              @Value("${itad.api-key}") String apiKey,
                              @Value("${itad.country:BR}") String pais,
                              @Value("${itad.steam-shop-id:61}") int lojaSteam) {
        var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        var factory = new JdkClientHttpRequestFactory(client);
        factory.setReadTimeout(Duration.ofSeconds(20));
        this.http = builder.baseUrl(BASE_URL)
                .defaultHeader("ITAD-API-Key", apiKey)
                .requestFactory(factory).build();
        this.pais = pais;
        this.lojaSteam = lojaSteam;
    }

    @Override
    public List<GameMatch> busca(String nomeJogo) {
        if (nomeJogo == null || nomeJogo.isBlank()) {
            return List.of();
        }
        JsonNode root = executa(() -> http.get()
                .uri(b -> b.path("/games/search/v1")
                        .queryParam("title", nomeJogo.trim())
                        .queryParam("results", MAX_BUSCA)
                        .build())
                .retrieve()
                .body(JsonNode.class));
        return parseBusca(root);
    }

    @Override
    public List<Offer> precos(List<String> gameIds) {
        List<Offer> resultado = new ArrayList<>();
        for (List<String> lote : lotes(gameIds)) {
            JsonNode root = executa(() -> http.post()
                    .uri(b -> b.path("/games/prices/v3")
                            .queryParam("country", pais)
                            .queryParam("shops", lojaSteam)
                            .queryParam("deals", false)
                            .build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(lote)
                    .retrieve()
                    .body(JsonNode.class));
            resultado.addAll(parsePrecos(root, lojaSteam));
        }
        return resultado;
    }

    @Override
    public Map<String, BigDecimal> precoHistorico(List<String> gameIds) {
        Map<String, BigDecimal> resultado = new HashMap<>();
        for (List<String> lote : lotes(gameIds)) {
            JsonNode root = executa(() -> http.post()
                    .uri(b -> b.path("/games/storelow/v2")
                            .queryParam("country", pais)
                            .queryParam("shops", lojaSteam)
                            .build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(lote)
                    .retrieve()
                    .body(JsonNode.class));
            resultado.putAll(parseMenorHistorico(root, lojaSteam));
        }
        return resultado;
    }

    //Os métodos parse são estáticos e sem rede para poderem ser testados com JSON fixo
    static List<GameMatch> parseBusca(JsonNode root) {
        List<GameMatch> out = new ArrayList<>();
        if (root == null || !root.isArray()) return out;
        Set<String> vistos = new HashSet<>();
        for (JsonNode n : root) {
            String id = n.path("id").asText("");
            String titulo = n.path("title").asText("");
            if (id.isBlank() || titulo.isBlank() || !vistos.add(id)) continue;
            out.add(new GameMatch(id, titulo));
        }
        return out;
    }

    static List<Offer> parsePrecos(JsonNode root, int lojaSteam) {
        List<Offer> out = new ArrayList<>();
        if (root == null || !root.isArray()) return out;
        for (JsonNode jogo : root) {
            String id = jogo.path("id").asText("");
            if (id.isBlank()) continue;
            for (JsonNode deal : jogo.path("deals")) {
                if (deal.path("shop").path("id").asInt(-1) != lojaSteam) continue;
                BigDecimal preco = dinheiro(deal.path("price"));
                BigDecimal normal = dinheiro(deal.path("regular"));
                String url = deal.path("url").asText("");
                if (preco == null || normal == null || url.isBlank()) continue;
                String moeda = deal.path("price").path("currency").asText("BRL");
                out.add(new Offer(id, null, preco, normal, moeda, deal.path("cut").asInt(0), url));
                break;
            }
        }
        return out;
    }

    static Map<String, BigDecimal> parseMenorHistorico(JsonNode root, int lojaSteam) {
        Map<String, BigDecimal> out = new HashMap<>();
        if (root == null || !root.isArray()) return out;
        for (JsonNode jogo : root) {
            String id = jogo.path("id").asText("");
            if (id.isBlank()) continue;
            for (JsonNode low : jogo.path("lows")) {
                if (low.path("shop").path("id").asInt(-1) != lojaSteam) continue;
                BigDecimal v = dinheiro(low.path("price"));
                if (v != null) out.put(id, v);
                break;
            }
        }
        return out;
    }

    //O ITAD envia o valor int (amountInt, ex.: 1999); as casas decimais vêm da moeda.
    static BigDecimal dinheiro(JsonNode price) {
        if (price == null || !price.hasNonNull("amountInt")) return null;
        int casas = 2;
        try {
            casas = Math.max(Currency.getInstance(price.path("currency").asText("BRL")).getDefaultFractionDigits(), 0);
        } catch (IllegalArgumentException ignored) {
        }
        return BigDecimal.valueOf(price.path("amountInt").asLong(), casas);
    }

    private static List<List<String>> lotes(List<String> ids) {
        List<String> unicos = new ArrayList<>(new LinkedHashSet<>(ids == null ? List.of() : ids));
        List<List<String>> out = new ArrayList<>();
        for (int i = 0; i < unicos.size(); i += LOTE) {
            out.add(unicos.subList(i, Math.min(i + LOTE, unicos.size())));
        }
        return out;
    }

    //Converte falhas do RestClient em ItadException, sem expor a URL nem a chave de API nos logs.
    private static <T> T executa(java.util.function.Supplier<T> chamada) {
        try {
            return chamada.get();
        } catch (RestClientResponseException e) {
            throw new ItadException(e.getStatusCode().value(), "ITAD respondeu " + e.getStatusCode().value());
        } catch (RestClientException e) {
            throw new ItadException(0, "Falha de rede/JSON ao falar com o ITAD (" + e.getClass().getSimpleName() + ")");
        }
    }
}
