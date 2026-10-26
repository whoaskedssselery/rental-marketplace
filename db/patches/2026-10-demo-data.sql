-- Refresh only unchanged records from the original demonstration dataset.
-- Passwords and user-created/edited records are not touched. Safe to run again.
BEGIN;
LOCK TABLE users, listings, rental_requests, reviews IN SHARE ROW EXCLUSIVE MODE;

CREATE TEMP TABLE demo_user_updates ON COMMIT DROP AS
SELECT u.id, v.new_name, v.new_phone, v.age_days, v.clock
FROM (VALUES
    (1, 'ivan.petrov@example.com', 'Иван Петров', '+7 900 111-11-11', 'OWNER', 'Иван Андреевич Петров', '+7 900 184-27-63', 100, '10:20'),
    (2, 'anna.smirnova@example.com', 'Анна Смирнова', '+7 900 222-22-22', 'RENTER', 'Анна Викторовна Смирнова', '+7 900 752-46-18', 98, '15:45'),
    (3, 'sergey.kovalev@example.com', 'Сергей Ковалёв', '+7 900 333-33-33', 'OWNER', 'Сергей Павлович Ковалёв', '+7 900 316-58-42', 95, '09:10'),
    (4, 'maria.ivanova@example.com', 'Мария Иванова', '+7 900 444-44-44', 'RENTER', 'Мария Алексеевна Иванова', '+7 900 628-13-95', 93, '18:30'),
    (5, 'dmitry.sokolov@example.com', 'Дмитрий Соколов', '+7 900 555-55-55', 'ADMIN', 'Дмитрий Олегович Соколов', '+7 900 473-85-26', 120, '11:05'),
    (6, 'olga.novikova@example.com', 'Ольга Новикова', '+7 900 666-66-66', 'RENTER', 'Ольга Игоревна Новикова', '+7 900 861-39-74', 90, '14:15')
) AS v (demo_number, email, old_name, old_phone, role, new_name, new_phone, age_days, clock)
JOIN users u ON u.id = v.demo_number AND u.email = v.email AND u.full_name = v.old_name
    AND u.phone = v.old_phone AND u.role = v.role;

CREATE TEMP TABLE demo_listing_updates ON COMMIT DROP AS
SELECT l.id, v.demo_number, v.new_title, v.new_description, v.new_category, v.age_days
FROM (VALUES
    (1, 'ivan.petrov@example.com', 'Квартира-студия в центре', 'Уютная студия рядом с метро', 'Недвижимость', 2500, TRUE, 'Студия 28 м² рядом с метро', 'Двуспальная кровать, кухонная зона и Wi-Fi. До метро 7 минут пешком. Заезд после 14:00, выезд до 12:00.', 'Недвижимость', 75),
    (2, 'ivan.petrov@example.com', 'Перфоратор Bosch', 'Мощный перфоратор для ремонта', 'Инструменты', 500, TRUE, 'Перфоратор Bosch GBH 2-26', 'Для сверления бетона и кирпича. В комплекте кейс, боковая рукоятка и два бура; выдаю вечером по договорённости.', 'Инструменты', 67),
    (3, 'sergey.kovalev@example.com', 'Электросамокат Xiaomi', 'Складной самокат, полный заряд', 'Транспорт', 400, TRUE, 'Электросамокат Xiaomi Mi 1S', 'Заряжаю перед выдачей. Зарядное устройство и замок в комплекте. Забрать можно после 18:00, вернуть — в согласованное время.', 'Транспорт', 61),
    (4, 'sergey.kovalev@example.com', 'Палатка туристическая 4-местная', 'Влагостойкая палатка', 'Спорт', 350, TRUE, 'Туристическая палатка на 4 человека', 'Два входа, москитные сетки, колышки и ремкомплект в чехле. Подойдёт для поездки на машине; после дождя нужно просушить.', 'Туризм', 58),
    (5, 'ivan.petrov@example.com', 'Проектор Epson', 'Для презентаций и кинопросмотра', 'Электроника', 800, TRUE, 'Проектор Epson EB-X49', 'Для презентации или домашнего кино. HDMI-кабель, пульт и сумка в комплекте. Экран не входит; подключение покажу при выдаче.', 'Электроника', 53),
    (6, 'sergey.kovalev@example.com', 'Велосипед горный', 'Требует лёгкого обслуживания', 'Спорт', 450, FALSE, 'Горный велосипед Stels Navigator 500', 'Рама M, колёса 26 дюймов. Сейчас на обслуживании: меняем цепь и задние колодки. До завершения ремонта не выдаётся.', 'Спорт', 49)
) AS v (demo_number, owner_email, old_title, old_description, old_category, price,
    available, new_title, new_description, new_category, age_days)
JOIN users u ON u.email = v.owner_email
JOIN demo_user_updates du ON du.id = u.id
JOIN listings l ON l.id = v.demo_number AND l.owner_id = u.id AND l.title = v.old_title
    AND l.description = v.old_description AND l.category = v.old_category
    AND l.price_per_day = v.price AND l.available = v.available;

CREATE TEMP TABLE demo_request_updates ON COMMIT DROP AS
WITH candidates AS (
    SELECT r.id, r.listing_id, v.demo_number, CURRENT_DATE + v.start_offset AS new_start,
        CURRENT_DATE + v.end_offset AS new_end,
        CURRENT_DATE - v.age_days + v.clock::time AS new_created_at
    FROM (VALUES
    (1, 1, 'anna.smirnova@example.com', '2026-09-01', '2026-09-05', 'COMPLETED', 10000, -25, -21, 30, '16:40'),
    (2, 2, 'maria.ivanova@example.com', '2026-09-02', '2026-09-03', 'COMPLETED', 500, -18, -17, 22, '10:15'),
    (3, 3, 'anna.smirnova@example.com', '2026-09-05', '2026-09-07', 'ACTIVE', 800, -1, 1, 5, '19:05'),
    (4, 4, 'olga.novikova@example.com', '2026-09-10', '2026-09-15', 'CONFIRMED', 1750, 3, 8, 4, '13:20'),
    (5, 5, 'maria.ivanova@example.com', '2026-09-12', '2026-09-14', 'NEW', 1600, 2, 4, 1, '17:35'),
    (6, 1, 'olga.novikova@example.com', '2026-09-20', '2026-09-25', 'NEW', 12500, 8, 13, 2, '12:10'),
    (7, 3, 'maria.ivanova@example.com', '2026-09-08', '2026-09-09', 'CANCELLED', 400, 4, 5, 3, '20:00'),
    (8, 6, 'anna.smirnova@example.com', '2026-08-20', '2026-08-25', 'REJECTED', 2250, 5, 10, 2, '14:25'),
    (9, 2, 'olga.novikova@example.com', '2026-09-18', '2026-09-19', 'NEW', 500, 5, 6, 1, '18:15'),
    (10, 5, 'anna.smirnova@example.com', '2026-09-22', '2026-09-23', 'NEW', 800, 7, 8, 1, '12:20'),
    (11, 4, 'maria.ivanova@example.com', '2026-08-01', '2026-08-10', 'COMPLETED', 3150, -40, -31, 44, '09:45')
    ) AS v (demo_number, listing_number, renter_email, old_start, old_end, status,
        total_price, start_offset, end_offset, age_days, clock)
    JOIN demo_listing_updates dl ON dl.demo_number = v.listing_number
    JOIN users u ON u.email = v.renter_email
    JOIN demo_user_updates du ON du.id = u.id
    JOIN rental_requests r ON r.id = v.demo_number AND r.listing_id = dl.id AND r.renter_id = u.id
        AND r.start_date = v.old_start::date AND r.end_date = v.old_end::date
        AND r.status = v.status AND r.total_price = v.total_price
)
SELECT c.* FROM candidates c
WHERE NOT EXISTS (
    SELECT 1 FROM rental_requests other
    WHERE other.listing_id = c.listing_id AND other.id <> c.id
        AND other.status IN ('NEW', 'CONFIRMED', 'ACTIVE')
        AND other.start_date < c.new_end AND other.end_date > c.new_start
        AND other.id NOT IN (SELECT id FROM candidates)
);

CREATE TEMP TABLE demo_review_updates ON COMMIT DROP AS
SELECT review.id, review.request_id, v.new_comment
FROM (VALUES
    (1, 1, 5, 'Отличная студия, все как на фото, хозяин встретил вовремя', 'Останавливались на четыре ночи. В квартире тихо, постель чистая, до метро действительно недалеко. Иван передал ключи без задержек.'),
    (2, 2, 4, 'Перфоратор рабочий, но без запасного бура', 'Для бетонной стены подошёл, выдали с кейсом. Один бур оказался затуплен, пришлось взять свой. В остальном всё хорошо.'),
    (3, 11, 3, 'Палатка нормальная, но немного протекла при сильном дожде', 'Вчетвером разместились нормально, все колышки были на месте. После сильного дождя задний шов немного промок; для сухой погоды вариант хороший.')
) AS v (review_number, request_number, rating, old_comment, new_comment)
JOIN demo_request_updates dr ON dr.demo_number = v.request_number
JOIN reviews review ON review.id = v.review_number AND review.request_id = dr.id
    AND review.rating = v.rating AND review.comment = v.old_comment;

UPDATE users u SET full_name = d.new_name, phone = d.new_phone,
    created_at = CURRENT_DATE - d.age_days + d.clock::time
FROM demo_user_updates d WHERE u.id = d.id;

UPDATE listings l SET title = d.new_title, description = d.new_description,
    category = d.new_category, created_at = CURRENT_DATE - d.age_days + TIME '12:00'
FROM demo_listing_updates d WHERE l.id = d.id;

UPDATE rental_requests r SET start_date = d.new_start, end_date = d.new_end,
    created_at = d.new_created_at
FROM demo_request_updates d WHERE r.id = d.id;

UPDATE reviews review SET comment = d.new_comment,
    created_at = r.end_date + INTERVAL '1 day 18 hours 30 minutes'
FROM demo_review_updates d JOIN rental_requests r ON r.id = d.request_id
WHERE review.id = d.id;
COMMIT;
