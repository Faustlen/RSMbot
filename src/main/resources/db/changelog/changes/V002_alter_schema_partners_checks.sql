ALTER TABLE partners
    RENAME COLUMN partner_id TO partner_tg_id;

ALTER TABLE partners
    ADD COLUMN is_valid BOOLEAN DEFAULT FALSE NOT NULL;

ALTER TABLE checks
    DROP CONSTRAINT fk_checks_user_card,
    DROP CONSTRAINT fk_checks_partner;

ALTER TABLE checks
    DROP COLUMN user_card;

ALTER TABLE checks
    ADD COLUMN user_tg_id BIGINT DEFAULT 0 NOT NULL;

ALTER TABLE checks
    ADD CONSTRAINT fk_checks_user FOREIGN KEY (user_tg_id) REFERENCES users (tg_id);

ALTER TABLE checks
    RENAME COLUMN partner_id TO partner_tg_id;

ALTER TABLE checks
    ADD CONSTRAINT fk_checks_partner FOREIGN KEY (partner_tg_id) REFERENCES partners (partner_tg_id);