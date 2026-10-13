CREATE TABLE IF NOT EXISTS reviews (
    id SERIAL PRIMARY KEY,
    request_id INT NOT NULL UNIQUE REFERENCES rental_requests (id) ON DELETE CASCADE,
    rating INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

INSERT INTO reviews (request_id, rating, comment)
SELECT v.request_id, v.rating, v.comment
FROM (VALUES
    (1, 5, 'Отличная студия, все как на фото, хозяин встретил вовремя'),
    (2, 4, 'Перфоратор рабочий, но без запасного бура'),
    (11, 3, 'Палатка нормальная, но немного протекла при сильном дожде')
) AS v (request_id, rating, comment)
WHERE EXISTS (SELECT 1 FROM rental_requests r WHERE r.id = v.request_id AND r.status = 'COMPLETED')
ON CONFLICT (request_id) DO NOTHING;
