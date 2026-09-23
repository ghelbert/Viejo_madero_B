CREATE TABLE roles (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(200)
);

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    role_id INT NOT NULL REFERENCES roles(id),
    full_name VARCHAR(120) NOT NULL,
    username VARCHAR(60) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    sort_order INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    category_id BIGINT NOT NULL REFERENCES categories(id),
    name VARCHAR(150) NOT NULL,
    description TEXT,
    base_price NUMERIC(10,2) NOT NULL CHECK (base_price >= 0),
    image_url VARCHAR(500),
    available BOOLEAN NOT NULL DEFAULT TRUE,
    is_wood_oven BOOLEAN NOT NULL DEFAULT FALSE,
    prep_minutes INT CHECK (prep_minutes IS NULL OR prep_minutes > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE restaurant_tables (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    capacity INT NOT NULL DEFAULT 2 CHECK (capacity > 0),
    zone VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'FREE',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_tables_status CHECK (status IN ('FREE','OCCUPIED','RESERVED','CLEANING'))
);

CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    order_type VARCHAR(20) NOT NULL DEFAULT 'DINE_IN',
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    table_id BIGINT REFERENCES restaurant_tables(id),
    created_by BIGINT NOT NULL REFERENCES users(id),
    confirmed_by BIGINT REFERENCES users(id),
    confirmed_at TIMESTAMPTZ,
    served_at TIMESTAMPTZ,
    notes TEXT,
    subtotal NUMERIC(10,2) NOT NULL DEFAULT 0,
    total NUMERIC(10,2) NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_orders_type CHECK (order_type IN ('DINE_IN','DELIVERY','TAKEAWAY')),
    CONSTRAINT chk_orders_status CHECK (status IN ('OPEN','CONFIRMED','IN_KITCHEN','READY','SERVED','CANCELLED')),
    CONSTRAINT chk_orders_table CHECK ((order_type = 'DINE_IN' AND table_id IS NOT NULL) OR order_type <> 'DINE_IN')
);

CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id),
    product_name VARCHAR(150) NOT NULL,
    unit_price NUMERIC(10,2) NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    line_total NUMERIC(10,2) NOT NULL,
    notes TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    CONSTRAINT chk_order_items_status CHECK (status IN ('PENDING','PREPARING','READY','SERVED','CANCELLED'))
);

CREATE TABLE order_status_history (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    from_status VARCHAR(30),
    to_status VARCHAR(30) NOT NULL,
    changed_by BIGINT REFERENCES users(id),
    changed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_order_items_order ON order_items(order_id);
CREATE INDEX idx_users_role ON users(role_id);
CREATE INDEX idx_products_available ON products(available) WHERE available = TRUE;
