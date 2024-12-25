ALTER TABLE checks DROP CONSTRAINT IF EXISTS fk_checks_partner;
ALTER TABLE partners ALTER COLUMN partner_tg_id SET DATA TYPE BIGINT;
ALTER TABLE checks ADD CONSTRAINT fk_checks_partner FOREIGN KEY (partner_tg_id) REFERENCES partners (partner_tg_id);
