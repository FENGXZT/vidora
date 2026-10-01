SET SESSION cte_max_recursion_depth = 20000;

INSERT INTO video (user_id, title, video_key, cover_key, duration, status,
                   view_count, like_count, favorite_count, create_time)
WITH RECURSIVE seq(n) AS (
    SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 10000
)
SELECT
    FLOOR(1 + RAND() * 6),
    CONCAT('压测视频 ', n),
    CONCAT('bench/', n, '.mp4'),
    CONCAT('cover/', n, '.jpg'),
    FLOOR(10 + RAND() * 3600),
    2,
    FLOOR(RAND() * 100000),
    FLOOR(RAND() * 5000),
    FLOOR(RAND() * 1000),
    NOW() - INTERVAL FLOOR(RAND() * 365) DAY
FROM seq;