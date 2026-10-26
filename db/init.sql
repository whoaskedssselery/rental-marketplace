CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash TEXT,
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

CREATE TABLE reviews (
    id SERIAL PRIMARY KEY,
    request_id INT NOT NULL UNIQUE REFERENCES rental_requests (id) ON DELETE CASCADE,
    rating INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

-- Fictional demonstration records; booking dates are relative to the day of initialization.
INSERT INTO users (full_name, email, phone, role, created_at) VALUES
    ('Иван Андреевич Петров', 'ivan.petrov@example.com', '+7 900 184-27-63', 'OWNER', CURRENT_DATE - 100 + TIME '10:20'),
    ('Анна Викторовна Смирнова', 'anna.smirnova@example.com', '+7 900 752-46-18', 'RENTER', CURRENT_DATE - 98 + TIME '15:45'),
    ('Сергей Павлович Ковалёв', 'sergey.kovalev@example.com', '+7 900 316-58-42', 'OWNER', CURRENT_DATE - 95 + TIME '09:10'),
    ('Мария Алексеевна Иванова', 'maria.ivanova@example.com', '+7 900 628-13-95', 'RENTER', CURRENT_DATE - 93 + TIME '18:30'),
    ('Дмитрий Олегович Соколов', 'dmitry.sokolov@example.com', '+7 900 473-85-26', 'ADMIN', CURRENT_DATE - 120 + TIME '11:05'),
    ('Ольга Игоревна Новикова', 'olga.novikova@example.com', '+7 900 861-39-74', 'RENTER', CURRENT_DATE - 90 + TIME '14:15');

INSERT INTO listings (owner_id, title, description, price_per_day, category, available, created_at) VALUES
    (1, 'Студия 28 м² рядом с метро', 'Двуспальная кровать, кухонная зона и Wi-Fi. До метро 7 минут пешком. Заезд после 14:00, выезд до 12:00.', 2500.00, 'Недвижимость', TRUE, CURRENT_DATE - 75 + TIME '12:00'),
    (1, 'Перфоратор Bosch GBH 2-26', 'Для сверления бетона и кирпича. В комплекте кейс, боковая рукоятка и два бура; выдаю вечером по договорённости.', 500.00, 'Инструменты', TRUE, CURRENT_DATE - 67 + TIME '12:00'),
    (3, 'Электросамокат Xiaomi Mi 1S', 'Заряжаю перед выдачей. Зарядное устройство и замок в комплекте. Забрать можно после 18:00, вернуть — в согласованное время.', 400.00, 'Транспорт', TRUE, CURRENT_DATE - 61 + TIME '12:00'),
    (3, 'Туристическая палатка на 4 человека', 'Два входа, москитные сетки, колышки и ремкомплект в чехле. Подойдёт для поездки на машине; после дождя нужно просушить.', 350.00, 'Туризм', TRUE, CURRENT_DATE - 58 + TIME '12:00'),
    (1, 'Проектор Epson EB-X49', 'Для презентации или домашнего кино. HDMI-кабель, пульт и сумка в комплекте. Экран не входит; подключение покажу при выдаче.', 800.00, 'Электроника', TRUE, CURRENT_DATE - 53 + TIME '12:00'),
    (3, 'Горный велосипед Stels Navigator 500', 'Рама M, колёса 26 дюймов. Сейчас на обслуживании: меняем цепь и задние колодки. До завершения ремонта не выдаётся.', 450.00, 'Спорт', FALSE, CURRENT_DATE - 49 + TIME '12:00');

INSERT INTO rental_requests (listing_id, renter_id, start_date, end_date, status, total_price, created_at) VALUES
    (1, 2, CURRENT_DATE - 25, CURRENT_DATE - 21, 'COMPLETED', 10000.00, CURRENT_DATE - 30 + TIME '16:40'),
    (2, 4, CURRENT_DATE - 18, CURRENT_DATE - 17, 'COMPLETED', 500.00, CURRENT_DATE - 22 + TIME '10:15'),
    (3, 2, CURRENT_DATE - 1, CURRENT_DATE + 1, 'ACTIVE', 800.00, CURRENT_DATE - 5 + TIME '19:05'),
    (4, 6, CURRENT_DATE + 3, CURRENT_DATE + 8, 'CONFIRMED', 1750.00, CURRENT_DATE - 4 + TIME '13:20'),
    (5, 4, CURRENT_DATE + 2, CURRENT_DATE + 4, 'NEW', 1600.00, CURRENT_DATE - 1 + TIME '17:35'),
    (1, 6, CURRENT_DATE + 8, CURRENT_DATE + 13, 'NEW', 12500.00, CURRENT_DATE - 2 + TIME '12:10'),
    (3, 4, CURRENT_DATE + 4, CURRENT_DATE + 5, 'CANCELLED', 400.00, CURRENT_DATE - 3 + TIME '20:00'),
    (6, 2, CURRENT_DATE + 5, CURRENT_DATE + 10, 'REJECTED', 2250.00, CURRENT_DATE - 2 + TIME '14:25'),
    (2, 6, CURRENT_DATE + 5, CURRENT_DATE + 6, 'NEW', 500.00, CURRENT_DATE - 1 + TIME '18:15'),
    (5, 2, CURRENT_DATE + 7, CURRENT_DATE + 8, 'NEW', 800.00, CURRENT_DATE - 1 + TIME '12:20'),
    (4, 4, CURRENT_DATE - 40, CURRENT_DATE - 31, 'COMPLETED', 3150.00, CURRENT_DATE - 44 + TIME '09:45');

CREATE UNIQUE INDEX users_email_lower_key ON users (lower(email));
CREATE INDEX idx_listings_owner_id ON listings (owner_id);
CREATE INDEX idx_rental_requests_renter_id ON rental_requests (renter_id);
CREATE INDEX idx_rental_requests_listing_status ON rental_requests (listing_id, status);

INSERT INTO reviews (request_id, rating, comment, created_at) VALUES
    (1, 5, 'Останавливались на четыре ночи. В квартире тихо, постель чистая, до метро действительно недалеко. Иван передал ключи без задержек.', CURRENT_DATE - 20 + TIME '18:30'),
    (2, 4, 'Для бетонной стены подошёл, выдали с кейсом. Один бур оказался затуплен, пришлось взять свой. В остальном всё хорошо.', CURRENT_DATE - 16 + TIME '18:30'),
    (11, 3, 'Вчетвером разместились нормально, все колышки были на месте. После сильного дождя задний шов немного промок; для сухой погоды вариант хороший.', CURRENT_DATE - 30 + TIME '18:30');
