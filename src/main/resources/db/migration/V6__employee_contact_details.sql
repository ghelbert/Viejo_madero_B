ALTER TABLE users
    ADD COLUMN dni VARCHAR(8),
    ADD COLUMN phone VARCHAR(9),
    ADD CONSTRAINT chk_users_dni_format
        CHECK (dni IS NULL OR dni ~ '^[0-9]{8}$'),
    ADD CONSTRAINT chk_users_phone_format
        CHECK (phone IS NULL OR phone ~ '^[0-9]{9}$');
