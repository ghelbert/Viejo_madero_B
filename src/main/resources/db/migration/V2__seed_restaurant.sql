INSERT INTO roles (name, description) VALUES
 ('ADMINISTRADOR', 'Gestiona usuarios, carta y operación'),
 ('MOZO', 'Registra y confirma pedidos en mesa'),
 ('COCINERO', 'Gestiona los pedidos en cocina')
ON CONFLICT (name) DO NOTHING;

INSERT INTO categories (name, description, sort_order) VALUES
 ('Combos broaster', 'Pollo y acompañamientos', 1),
 ('Especialidades', 'Platos principales', 2),
 ('Bebidas', 'Bebidas frías', 3)
ON CONFLICT (name) DO NOTHING;

INSERT INTO users (role_id, full_name, username, password_hash)
SELECT r.id, u.full_name, u.username, u.password_hash
FROM roles r
JOIN (VALUES
 ('ADMINISTRADOR', 'Rosa Medina', 'admin', '$2a$10$DLAkpa0yCLvw/6IaH6Q63eWC5GYAvtUIpfIUpdmPVlThTZe/u39Rq'),
 ('MOZO', 'Carlos Ramos', 'mozo', '$2a$10$DLAkpa0yCLvw/6IaH6Q63eWC5GYAvtUIpfIUpdmPVlThTZe/u39Rq'),
 ('COCINERO', 'Miguel Torres', 'cocina', '$2a$10$DLAkpa0yCLvw/6IaH6Q63eWC5GYAvtUIpfIUpdmPVlThTZe/u39Rq')
) AS u(role_name, full_name, username, password_hash) ON u.role_name = r.name
ON CONFLICT (username) DO NOTHING;

INSERT INTO restaurant_tables (code, capacity, zone)
SELECT 'M' || n, CASE WHEN n IN (4, 7) THEN 6 ELSE 4 END, 'Salón'
FROM generate_series(1, 8) AS n
ON CONFLICT (code) DO NOTHING;

INSERT INTO products (category_id, name, description, base_price, prep_minutes)
SELECT c.id, p.name, p.description, p.price, p.minutes
FROM categories c
JOIN (VALUES
 ('Combos broaster', '1/4 Pollo Broaster Clásico', 'Pollo crujiente, papas y ensalada', 24.00, 18),
 ('Combos broaster', '1/2 Pollo Broaster Familiar', 'Medio pollo crocante y acompañamientos', 42.00, 25),
 ('Especialidades', 'Alitas BBQ Crocantes', 'Alitas doradas con salsa BBQ', 23.00, 15),
 ('Especialidades', 'Hamburguesa de Pollo Crispy', 'Filete crispy, queso y papas', 21.00, 12),
 ('Bebidas', 'Chicha Morada de la Casa', 'Bebida natural con canela y limón', 7.00, 3),
 ('Bebidas', 'Maracuyá Frozen', 'Bebida frozen preparada al momento', 10.00, 5)
) AS p(category_name, name, description, price, minutes) ON p.category_name = c.name
WHERE NOT EXISTS (SELECT 1 FROM products existing WHERE existing.name = p.name);
