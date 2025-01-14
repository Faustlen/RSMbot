CREATE TABLE users_list (
    user_card    INTEGER      NOT NULL,
    full_name    VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20)  NOT NULL,
    birth_date   DATE         NOT NULL,
    CONSTRAINT pk_users_list PRIMARY KEY (user_card)
);

ALTER TABLE users
    ADD CONSTRAINT fk_users_users_list FOREIGN KEY (user_card) REFERENCES users_list(user_card);

ALTER TABLE checks
    DROP COLUMN user_tg_id;

ALTER TABLE checks
    ALTER COLUMN check_sum TYPE DECIMAL(9,2);
