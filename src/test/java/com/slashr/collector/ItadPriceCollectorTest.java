package com.slashr.collector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ItadPriceCollectorTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void parseBuscaIgnoraIdsTitulosVaziosEDuplicados() throws Exception {
        JsonNode json = json("""
                [
                  {"id":"id-1","title":"Hades"},
                  {"id":"id-1","title":"Hades duplicado"},
                  {"id":"","title":"Sem ID"},
                  {"id":"id-2","title":"  "},
                  {"id":"id-3","title":"Celeste"}
                ]
                """);

        List<GameMatch> resultado = ItadPriceCollector.parseBusca(json);

        assertThat(resultado).containsExactly(
                new GameMatch("id-1", "Hades"),
                new GameMatch("id-3", "Celeste")
        );
    }

    @Test
    void parseBuscaRetornaListaVaziaParaRaizNulaOuNaoArray() throws Exception {
        assertThat(ItadPriceCollector.parseBusca(null)).isEmpty();
        assertThat(ItadPriceCollector.parseBusca(json("{\"id\":\"id-1\"}"))).isEmpty();
    }

    @Test
    void parsePrecosFiltraLojaSteamEOfertasSemDadosObrigatorios() throws Exception {
        JsonNode json = json("""
                [
                  {
                    "id":"game-1",
                    "deals":[
                      {"shop":{"id":99},"price":{"amountInt":1000,"currency":"BRL"},"regular":{"amountInt":2000,"currency":"BRL"},"url":"https://other.example"},
                      {"shop":{"id":61},"price":{"amountInt":1999,"currency":"BRL"},"regular":{"amountInt":5999,"currency":"BRL"},"url":"https://steam.example/game-1","cut":67}
                    ]
                  },
                  {
                    "id":"game-2",
                    "deals":[
                      {"shop":{"id":61},"price":{"currency":"BRL"},"regular":{"amountInt":2000,"currency":"BRL"},"url":"https://steam.example/game-2"}
                    ]
                  },
                  {
                    "id":"game-3",
                    "deals":[
                      {"shop":{"id":61},"price":{"amountInt":500,"currency":"USD"},"regular":{"amountInt":1000,"currency":"USD"},"url":"https://steam.example/game-3","cut":50}
                    ]
                  },
                  {"id":"", "deals":[]}
                ]
                """);

        List<Offer> resultado = ItadPriceCollector.parsePrecos(json, 61);

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).gameId()).isEqualTo("game-1");
        assertThat(resultado.get(0).preco()).isEqualByComparingTo("19.99");
        assertThat(resultado.get(0).precoNormal()).isEqualByComparingTo("59.99");
        assertThat(resultado.get(0).moeda()).isEqualTo("BRL");
        assertThat(resultado.get(0).desconto()).isEqualTo(67);
        assertThat(resultado.get(0).url()).isEqualTo("https://steam.example/game-1");
        assertThat(resultado.get(1).gameId()).isEqualTo("game-3");
        assertThat(resultado.get(1).preco()).isEqualByComparingTo("5.00");
    }

    @Test
    void parsePrecosRetornaListaVaziaQuandoRaizNaoEArray() throws Exception {
        assertThat(ItadPriceCollector.parsePrecos(null, 61)).isEmpty();
        assertThat(ItadPriceCollector.parsePrecos(json("{}"), 61)).isEmpty();
    }

    @Test
    void parseMenorHistoricoSelecionaPrecoDaLojaInformada() throws Exception {
        JsonNode json = json("""
                [
                  {
                    "id":"game-1",
                    "lows":[
                      {"shop":{"id":99},"price":{"amountInt":500,"currency":"BRL"}},
                      {"shop":{"id":61},"price":{"amountInt":1234,"currency":"BRL"}}
                    ]
                  },
                  {
                    "id":"game-2",
                    "lows":[
                      {"shop":{"id":61},"price":{"amountInt":null,"currency":"BRL"}}
                    ]
                  },
                  {"id":"", "lows":[{"shop":{"id":61},"price":{"amountInt":100}}]}
                ]
                """);

        Map<String, BigDecimal> resultado = ItadPriceCollector.parseMenorHistorico(json, 61);

        assertThat(resultado).containsOnlyKeys("game-1");
        assertThat(resultado.get("game-1")).isEqualByComparingTo("12.34");
    }

    @Test
    void parseMenorHistoricoRetornaMapaVazioParaRaizInvalida() throws Exception {
        assertThat(ItadPriceCollector.parseMenorHistorico(null, 61)).isEmpty();
        assertThat(ItadPriceCollector.parseMenorHistorico(json("[]"), 61)).isEmpty();
        assertThat(ItadPriceCollector.parseMenorHistorico(json("{}"), 61)).isEmpty();
    }

    @Test
    void dinheiroInterpretaCasasDecimaisDaMoeda() throws Exception {
        assertThat(ItadPriceCollector.dinheiro(json("{\"amountInt\":1999,\"currency\":\"BRL\"}")))
                .isEqualByComparingTo("19.99");
        assertThat(ItadPriceCollector.dinheiro(json("{\"amountInt\":999,\"currency\":\"USD\"}")))
                .isEqualByComparingTo("9.99");
        assertThat(ItadPriceCollector.dinheiro(json("{\"amountInt\":123,\"currency\":\"JPY\"}")))
                .isEqualByComparingTo("123");
    }

    @Test
    void dinheiroUsaDuasCasasParaMoedaDesconhecidaEOmitePrecoSemAmountInt() throws Exception {
        assertThat(ItadPriceCollector.dinheiro(json("{\"amountInt\":1234,\"currency\":\"ZZZ\"}")))
                .isEqualByComparingTo("12.34");
        assertThat(ItadPriceCollector.dinheiro(json("{\"currency\":\"BRL\"}"))).isNull();
        assertThat(ItadPriceCollector.dinheiro(json("{\"amountInt\":null,\"currency\":\"BRL\"}"))).isNull();
        assertThat(ItadPriceCollector.dinheiro(null)).isNull();
    }

    private JsonNode json(String source) throws Exception {
        return mapper.readTree(source);
    }
}
