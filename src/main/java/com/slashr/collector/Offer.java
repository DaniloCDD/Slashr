package com.slashr.collector;

import java.math.BigDecimal;

/*
* essa classe para verificar a oferta(vulgo desconto) da steam
* esse "record" é pra deixar a classe imutável
*
* tem o parametro "url" ali pq de acordo com os termos do ITAD, a gente não pode alterar os dados
* tem que ser o link da ITAD exato
*
* o dinheiro precisa ser BigDecimal, pq se for double ele arredonda
*/
public record Offer(String gameId, String nomeJogo,
                    BigDecimal preco, BigDecimal precoNormal,
                    String moeda, int desconto, String url)
{
    //a API de preços do ITAD nao devolve o nome do jogo, então quem chamar essa função vai receber o nome do banco
    public Offer comNome(String nome) {
        return new Offer(gameId, nome, preco, precoNormal, moeda, desconto, url);
    }
}
