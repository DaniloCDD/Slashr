package com.slashr.bot;

import java.util.Optional;
import java.util.regex.Pattern;


public record CallbackRemove(String itadId, long userId) {

    public static final String PREFIXO = "rem";
    private static final Pattern UUID_CANONICO =
            Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");

    public static String formata(String itadId, long userId) {
        if (itadId == null || !UUID_CANONICO.matcher(itadId.toLowerCase()).matches()) {
            throw new IllegalArgumentException("id do ITAD inválido");
        }
        return PREFIXO + ":" + itadId.toLowerCase() + ":" + userId;
    }

    public static Optional<CallbackRemove> parse(String data) {
        if (data == null) return Optional.empty();
        String[] p = data.split(":", -1);
        if (p.length != 3 || !p[0].equals(PREFIXO)) return Optional.empty();
        if (!UUID_CANONICO.matcher(p[1]).matches()) return Optional.empty();
        try {
            return Optional.of(new CallbackRemove(p[1], Long.parseLong(p[2])));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}