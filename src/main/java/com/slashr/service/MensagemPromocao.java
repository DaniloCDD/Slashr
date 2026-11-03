package com.slashr.service;

import com.slashr.collector.Offer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Optional;


/**
 * Monta o texto HTML da mensagem de promoção enviada ao grupo.
 */
public final class MensagemPromocao {

    private static final Locale PT_BR = Locale.of("pt", "BR");

    public record Mencao(long telegramUserId, String firstName, String username) {}

    private MensagemPromocao() {}

    public static String formata(Offer o, BigDecimal menorHistorico, List<Mencao> mencoes) {
        StringBuilder sb = new StringBuilder();
        sb.append("🔥 <b>").append(escapa(o.nomeJogo())).append("</b> está em promoção na Steam!\n");
        sb.append("💰 ").append(dinheiro(o.preco(), o.moeda()))
          .append(" (-").append(o.desconto()).append("%)")
          .append(" — antes ").append(dinheiro(o.precoNormal(), o.moeda())).append('\n');

        if (menorHistorico == null) {
            sb.append("📉 Menor preço histórico: sem dados\n");
        } else {
            sb.append("📉 Menor preço histórico: ").append(dinheiro(menorHistorico, o.moeda())).append('\n');
            Optional<Veredito> v = CalculadoraVeredito.calcula(o.preco(), menorHistorico);
            v.ifPresent(x -> sb.append(x.emoji()).append(" <b>").append(x.rotulo()).append("</b>")
                    .append(" — ").append(distancia(o.preco(), menorHistorico)).append('\n'));
        }

        sb.append("🔗 <a href=\"").append(escapa(o.url())).append("\">Ver no IsThereAnyDeal</a>\n");

        if (mencoes != null && !mencoes.isEmpty()) {
            sb.append("👤 ");
            for (int i = 0; i < mencoes.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(mencao(mencoes.get(i)));
            }
            sb.append('\n');
        }
        sb.append("<i>Dados: IsThereAnyDeal</i>");
        return sb.toString();
    }

    static String distancia(BigDecimal preco, BigDecimal menor) {
        if (preco.compareTo(menor) <= 0) return "no menor preço histórico";
        if (menor.signum() == 0) return "acima do menor histórico (grátis)";
        BigDecimal pct = preco.subtract(menor).multiply(BigDecimal.valueOf(100))
                .divide(menor, 0, RoundingMode.HALF_UP);
        return pct.toPlainString() + "% acima do menor histórico";
    }

    static String mencao(Mencao m) {
        if (m.username() != null && !m.username().isBlank()) {
            return "@" + escapa(m.username());
        }
        String nome = (m.firstName() == null || m.firstName().isBlank()) ? "usuário" : m.firstName();
        return "<a href=\"tg://user?id=" + m.telegramUserId() + "\">" + escapa(nome) + "</a>";
    }

    static String dinheiro(BigDecimal valor, String moeda) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(PT_BR);
        try {
            nf.setCurrency(Currency.getInstance(moeda));
        } catch (IllegalArgumentException | NullPointerException e) {
            //Moeda desconhecida, então, vai manter o formato padrão do real.
        }
        return nf.format(valor).replace('\u00A0', ' ');
    }

    // Escapa os caracteres especiais do parse_mode HTML do Telegram.
    static String escapa(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
