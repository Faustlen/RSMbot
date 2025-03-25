CREATE TABLE stocks (
    stocks_id         INTEGER       NOT NULL,
    partner_tg_id     INTEGER       NOT NULL,
    image             BYTEA         NOT NULL,
    head              VARCHAR(100)  NOT NULL,
    period_stocks_s   DATE          NOT NULL,
    period_stocks_e   DATE          NOT NULL,
    description       VARCHAR(250)  NOT NULL,
    CONSTRAINT pk_stocks PRIMARY KEY (stocks_id),
    CONSTRAINT fk_stocks_partners FOREIGN KEY (partner_tg_id) REFERENCES partners (partner_tg_id)
);

