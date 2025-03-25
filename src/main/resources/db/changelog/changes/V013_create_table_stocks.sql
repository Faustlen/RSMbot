CREATE TABLE stocks (
    stocks_id         INTEGER       NOT NULL,
    partner_tg_id     INTEGER       NOT NULL,
    image             BYTEA         ,
    head              VARCHAR(100)  NOT NULL,
    period_stocks_start   DATE          NOT NULL,
    period_stocks_end   DATE          NOT NULL,
    description       VARCHAR(250)  NOT NULL,
    CONSTRAINT pk_stocks PRIMARY KEY (stocks_id),
    CONSTRAINT fk_stocks_partners FOREIGN KEY (partner_tg_id) REFERENCES partners (partner_tg_id)
);

