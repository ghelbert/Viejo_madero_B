INSERT INTO roles (name, description) VALUES
    ('RECEPCIONISTA', 'Registra pedidos para delivery y para llevar'),
    ('REPARTIDOR', 'Gestiona rutas y confirma entregas')
ON CONFLICT (name) DO NOTHING;

ALTER TABLE orders
    DROP CONSTRAINT chk_orders_status;

ALTER TABLE orders
    ADD CONSTRAINT chk_orders_status
    CHECK (status IN ('OPEN', 'CONFIRMED', 'IN_KITCHEN', 'READY', 'SERVED', 'IN_ROUTE', 'DELIVERED', 'CANCELLED'));

ALTER TABLE orders
    ADD COLUMN customer_phone VARCHAR(30),
    ADD COLUMN delivery_address VARCHAR(500),
    ADD COLUMN delivery_latitude NUMERIC(10,7),
    ADD COLUMN delivery_longitude NUMERIC(10,7),
    ADD COLUMN payment_method VARCHAR(20),
    ADD COLUMN delivery_notes TEXT,
    ADD COLUMN ready_at TIMESTAMPTZ,
    ADD COLUMN route_started_at TIMESTAMPTZ,
    ADD COLUMN delivered_at TIMESTAMPTZ,
    ADD COLUMN delivery_completion_note TEXT,
    ADD COLUMN delivery_user_id BIGINT REFERENCES users(id);

ALTER TABLE orders
    ADD CONSTRAINT chk_orders_payment_method
    CHECK (payment_method IS NULL OR payment_method IN ('YAPE', 'PLIN', 'CASH')),
    ADD CONSTRAINT chk_delivery_coordinates
    CHECK ((delivery_latitude IS NULL AND delivery_longitude IS NULL)
        OR (delivery_latitude BETWEEN -90 AND 90 AND delivery_longitude BETWEEN -180 AND 180)),
    ADD CONSTRAINT chk_delivery_required_address
    CHECK (order_type <> 'DELIVERY' OR delivery_address IS NOT NULL);

CREATE INDEX idx_delivery_orders_status
    ON orders(order_type, status, created_at);
