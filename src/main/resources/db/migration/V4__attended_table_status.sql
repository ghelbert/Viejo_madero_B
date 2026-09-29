ALTER TABLE restaurant_tables
    DROP CONSTRAINT chk_tables_status;

ALTER TABLE restaurant_tables
    ADD CONSTRAINT chk_tables_status
    CHECK (status IN ('FREE', 'OCCUPIED', 'ATTENDED', 'RESERVED', 'CLEANING'));