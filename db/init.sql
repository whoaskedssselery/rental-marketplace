CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    phone VARCHAR(30),
    role VARCHAR(20) NOT NULL CHECK (role IN ('RENTER', 'OWNER', 'ADMIN')),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE listings (
    id SERIAL PRIMARY KEY,
    owner_id INT NOT NULL REFERENCES users (id),
    title VARCHAR(200) NOT NULL,
    description TEXT,
    price_per_day NUMERIC(10, 2) NOT NULL CHECK (price_per_day > 0),
    category VARCHAR(50) NOT NULL,
    available BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE rental_requests (
    id SERIAL PRIMARY KEY,
    listing_id INT NOT NULL REFERENCES listings (id),
    renter_id INT NOT NULL REFERENCES users (id),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('NEW', 'CONFIRMED', 'ACTIVE', 'COMPLETED', 'CANCELLED', 'REJECTED')),
    total_price NUMERIC(10, 2) NOT NULL CHECK (total_price > 0),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    CHECK (end_date > start_date),
    CONSTRAINT no_overlapping_active_requests EXCLUDE USING gist (
        listing_id WITH =,
        daterange(start_date, end_date) WITH &&
    ) WHERE (status IN ('NEW', 'CONFIRMED', 'ACTIVE'))
);

INSERT INTO users (full_name, email, phone, role) VALUES
    ('Иван Петров', 'ivan.petrov@example.com', '+7 900 111-11-11', 'OWNER'),
    ('Анна Смирнова', 'anna.smirnova@example.com', '+7 900 222-22-22', 'RENTER'),
    ('Сергей Ковалёв', 'sergey.kovalev@example.com', '+7 900 333-33-33', 'OWNER'),
    ('Мария Иванова', 'maria.ivanova@example.com', '+7 900 444-44-44', 'RENTER'),
    ('Дмитрий Соколов', 'dmitry.sokolov@example.com', '+7 900 555-55-55', 'ADMIN'),
    ('Ольга Новикова', 'olga.novikova@example.com', '+7 900 666-66-66', 'RENTER');

INSERT INTO listings (owner_id, title, description, price_per_day, category, available) VALUES
    (1, 'Квартира-студия в центре', 'Уютная студия рядом с метро', 2500.00, 'Недвижимость', TRUE),
    (1, 'Перфоратор Bosch', 'Мощный перфоратор для ремонта', 500.00, 'Инструменты', TRUE),
    (3, 'Электросамокат Xiaomi', 'Складной самокат, полный заряд', 400.00, 'Транспорт', TRUE),
    (3, 'Палатка туристическая 4-местная', 'Влагостойкая палатка', 350.00, 'Спорт', TRUE),
    (1, 'Проектор Epson', 'Для презентаций и кинопросмотра', 800.00, 'Электроника', TRUE),
    (3, 'Велосипед горный', 'Требует лёгкого обслуживания', 450.00, 'Спорт', FALSE);

INSERT INTO rental_requests (listing_id, renter_id, start_date, end_date, status, total_price) VALUES
    (1, 2, '2026-09-01', '2026-09-05', 'COMPLETED', 10000.00),
    (2, 4, '2026-09-02', '2026-09-03', 'COMPLETED', 500.00),
    (3, 2, '2026-09-05', '2026-09-07', 'ACTIVE', 800.00),
    (4, 6, '2026-09-10', '2026-09-15', 'CONFIRMED', 1750.00),
    (5, 4, '2026-09-12', '2026-09-14', 'NEW', 1600.00),
    (1, 6, '2026-09-20', '2026-09-25', 'NEW', 12500.00),
    (3, 4, '2026-09-08', '2026-09-09', 'CANCELLED', 400.00),
    (6, 2, '2026-08-20', '2026-08-25', 'REJECTED', 2250.00),
    (2, 6, '2026-09-18', '2026-09-19', 'NEW', 500.00),
    (5, 2, '2026-09-22', '2026-09-23', 'NEW', 800.00),
    (4, 4, '2026-08-01', '2026-08-10', 'COMPLETED', 3150.00);
CREATE INDEX idx_listings_owner_id ON listings (owner_id);
CREATE INDEX idx_rental_requests_renter_id ON rental_requests (renter_id);
CREATE INDEX idx_rental_requests_listing_status ON rental_requests (listing_id, status);
