package com.slashr.collector;

import java.util.List;

public interface PriceCollector {
    String nomeLoja();
    List<Offer> busca(String query);
}
