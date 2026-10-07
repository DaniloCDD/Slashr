package com.slashr.collector;

import java.math.BigDecimal;

public record Offer(String loja, String nomeProduto, BigDecimal preco, String url) {
}
