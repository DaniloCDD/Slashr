package com.slashr.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Pedido de uma pessoa, em um chat, para acompanhar um jogo.
 */
@Entity
@Table(name = "price_alerts",
        uniqueConstraints = @UniqueConstraint(name = "uk_alerta_pessoa_jogo",
                columnNames = {"chat_id", "telegram_user_id", "game_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PriceAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_id", nullable = false)
    private Long chatId;

    @Column(name = "telegram_user_id", nullable = false)
    private Long telegramUserId;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    /**
     * @username do Telegram, sem o @. Nulo se a pessoa não tiver (aí a menção usa tg://user?id=).
     */
    @Column(length = 64)
    private String username;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    /**
     * Último preço avisado, em centavos. Nulo se nunca houve aviso.
     */
    @Column(name = "last_notified_price_cents")
    private Integer lastNotifiedPriceCents;

    /**
     * Indica que já foi avisado a promoção em andamento, para não repetir o aviso.
     */
    @Column(name = "on_sale", nullable = false)
    private boolean onSale;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void aoCriar() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
