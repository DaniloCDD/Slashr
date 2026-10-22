package com.slashr.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;


@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
//enum padrao para setar quais vão ser as "reações" do bot com alguma promo
public enum Veredito {
    COMPRE("🟢", "Compre"),
    RAZOAVEL("🟡", "Razoável"),
    ESPERE("🔴", "Espere");

    private final String emoji;
    private final String rotulo;
}
