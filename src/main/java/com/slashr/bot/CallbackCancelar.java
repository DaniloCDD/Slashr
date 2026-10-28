package com.slashr.bot;

import java.util.Optional;


public record CallbackCancelar(long userId) {

    public static final String PREFIXO = "can";

    public static String formata(long userId) {
        return PREFIXO + ":" + userId;
    }

    public static Optional<CallbackCancelar> parse(String data) {
        if (data == null) return Optional.empty();
        String[] p = data.split(":", -1);
        if (p.length != 2 || !p[0].equals(PREFIXO)) return Optional.empty();
        try {
            return Optional.of(new CallbackCancelar(Long.parseLong(p[1])));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
