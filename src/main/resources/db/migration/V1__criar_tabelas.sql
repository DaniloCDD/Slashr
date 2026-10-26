CREATE TABLE games (
    id            BIGSERIAL    PRIMARY KEY,
    itad_id       VARCHAR(36)  NOT NULL UNIQUE,
    title         VARCHAR(255) NOT NULL
);

CREATE TABLE price_alerts (
    id                          BIGSERIAL    PRIMARY KEY,
    chat_id                     BIGINT       NOT NULL,
    telegram_user_id            BIGINT       NOT NULL,
    first_name                  VARCHAR(255) NOT NULL,
    username                    VARCHAR(64),
    game_id                     BIGINT       NOT NULL REFERENCES games (id) ON DELETE CASCADE,
    last_notified_price_cents   INTEGER,
    on_sale                     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at                  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_alerta_pessoa_jogo UNIQUE (chat_id, telegram_user_id, game_id)
);

CREATE INDEX idx_price_alerts_game ON price_alerts (game_id);

CREATE TABLE bot_state (
    nome   VARCHAR(50) PRIMARY KEY,
    valor  BIGINT      NOT NULL
);
