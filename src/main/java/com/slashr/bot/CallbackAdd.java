package com.slashr.bot;

import java.util.Optional;
import java.util.regex.Pattern;

/*
 * Formato do callback_data dos botões do /add: add:<idDoJogo>:<idDoUsuário>.
 * Monta (formata) e lê (parse) no mesmo lugar, para o formato não ficar espalhado.
 * O parse nunca confia no que vem do cliente: fora do formato, devolve vazio e o clique é ignorado.
 * Cabe nos 64 bytes do Telegram (máx. 56), por isso o título não vai aqui.
 */
public record CallbackAdd(String itadId, long userId) {

    private static final String PREFIXO = "add";
    private static final Pattern UUID_CANONICO =
            Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
    public static String formata(String itadId, long userId) {
        if (itadId == null || !UUID_CANONICO.matcher(itadId.toLowerCase()).matches()) {
            throw new IllegalArgumentException("id do ITAD inválido");
        }
        return PREFIXO + ":" + itadId.toLowerCase() + ":" + userId;
    }

    public static Optional<CallbackAdd> parse(String data) {
        if (data == null) return Optional.empty();
        String[] p = data.split(":", -1);
        if (p.length != 3 || !p[0].equals(PREFIXO)) return Optional.empty();
        if (!UUID_CANONICO.matcher(p[1]).matches()) return Optional.empty();
        try {
            return Optional.of(new CallbackAdd(p[1], Long.parseLong(p[2])));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
