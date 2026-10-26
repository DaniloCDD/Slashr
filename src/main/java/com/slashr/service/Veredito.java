package com.slashr.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;


@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum Veredito {
    COMPRE("🟢", "Compre"),
    RAZOAVEL("🟡", "Razoável"),
    ESPERE("🔴", "Espere");

    private final String emoji;
    private final String rotulo;
}
