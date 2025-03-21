ALTER TABLE users
    ALTER COLUMN birth_date TYPE VARCHAR(20)
        USING TO_CHAR(birth_date, 'DD.MM.YYYY');