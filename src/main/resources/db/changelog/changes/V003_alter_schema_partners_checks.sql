ALTER TABLE checks
    DROP CONSTRAINT fk_checks_partner;

ALTER TABLE partners
    DROP CONSTRAINT pk_partners;

ALTER TABLE partners
    ALTER COLUMN partner_tg_id TYPE BIGINT;

ALTER TABLE checks
    ALTER COLUMN partner_tg_id TYPE BIGINT;

ALTER TABLE partners
    ADD CONSTRAINT pk_partners PRIMARY KEY (partner_tg_id);

ALTER TABLE checks
    ADD CONSTRAINT fk_checks_partner FOREIGN KEY (partner_tg_id) REFERENCES partners (partner_tg_id);