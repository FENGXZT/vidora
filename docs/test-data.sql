-- ============================================================
-- Vidora 测试数据
-- 用途：验证分页、模糊搜索、动态排序
-- 建议在 Day 3 的"改需求练习"之前执行
-- ============================================================

-- ------------------------------------------------------------
-- 0. 先看看现在有什么（避免和已有数据冲突/混淆）
-- ------------------------------------------------------------
SELECT id, username, nickname FROM `user` ORDER BY id;
SELECT id, user_id, title, view_count, status, create_time FROM video ORDER BY id;


-- ------------------------------------------------------------
-- 1. 测试用户
--    注意：Day 2 你已经手动插过一条 testuser，这里只加两个新的，
--    用户名唯一索引保证不会重复（重复执行会报 Duplicate entry）
-- ------------------------------------------------------------
INSERT INTO `user` (username, password, nickname) VALUES
  ('alice', 'TODO_NOT_ENCRYPTED', '爱丽丝'),
  ('bob',   'TODO_NOT_ENCRYPTED', '鲍勃');


-- ------------------------------------------------------------
-- 2. 取用户 id（后面插视频要用）
-- ------------------------------------------------------------
SET @uid_testuser = (SELECT id FROM `user` WHERE username = 'testuser');
SET @uid_alice    = (SELECT id FROM `user` WHERE username = 'alice');
SET @uid_bob      = (SELECT id FROM `user` WHERE username = 'bob');

SELECT @uid_testuser AS testuser_id, @uid_alice AS alice_id, @uid_bob AS bob_id;


-- ------------------------------------------------------------
-- 3. 测试视频（10 条）
--
-- ★★★ 这批数据的关键设计：让 create_time 的顺序
--     和 view_count 的顺序【故意不同】
--
--     为什么？假设你写排序时把字段搞错了（该按 view_count 却按了 create_time），
--     如果两种排序碰巧结果一样，你就永远发现不了这个 bug。
--     测试数据的价值就在于【能让错误暴露出来】。
--
--     另外必须显式指定 create_time 和 view_count：
--     表里 create_time 默认当前时间、view_count 默认 0，
--     不指定的话所有数据都一样，排序测试毫无意义。
-- ------------------------------------------------------------
INSERT INTO video
  (user_id, title, introduction, video_key, cover_key, duration, status,
   view_count, like_count, favorite_count, create_time)
VALUES
  (@uid_testuser, 'Spring Boot 入门教程',      '从零开始学 Spring Boot',   'v/01.mp4', 'c/01.jpg',  600, 2,   50,   5,  2, '2026-08-01 10:00:00'),
  (@uid_alice,    'Spring Boot 实战',          '企业级项目实战',           'v/02.mp4', 'c/02.jpg', 1200, 2,  900,  80, 25, '2026-08-15 14:30:00'),
  (@uid_bob,      '我的第一个视频',            NULL,                       'v/03.mp4', 'c/03.jpg',  180, 2,   12,   1,  0, '2026-09-01 09:15:00'),
  (@uid_alice,    'Redis 缓存入门',            '缓存三兄弟一次讲清',       'v/04.mp4', 'c/04.jpg',  900, 2, 3000, 400, 88, '2026-09-05 20:00:00'),
  (@uid_testuser, 'Redis 实战：缓存穿透',      '布隆过滤器与空值缓存',     'v/05.mp4', 'c/05.jpg', 1500, 2,    7,   0,  0, '2026-09-10 11:20:00'),
  (@uid_bob,      '前端 Vue 快速上手',         'Vue3 + Element Plus',      'v/06.mp4', 'c/06.jpg',  720, 2,  450,  30,  9, '2026-09-12 16:45:00'),
  (@uid_alice,    '未完成的视频（转码中）',     '用于测试状态筛选',         'v/07.mp4', 'c/07.jpg',  300, 1,    0,   0,  0, '2026-09-15 08:00:00'),
  (@uid_bob,      '已下架的视频',              '用于测试状态筛选',         'v/08.mp4', 'c/08.jpg',  240, 4,  999,  99, 33, '2026-09-16 19:30:00'),
  (@uid_testuser, 'MySQL 索引原理',            'B+ 树与最左前缀原则',      'v/09.mp4', 'c/09.jpg', 1800, 2,  150,  22,  6, '2026-09-18 21:10:00'),
  (@uid_alice,    '测试视频：排序专用',        '标题含"测试"关键词',       'v/10.mp4', 'c/10.jpg',  360, 2,   80,   3,  1, '2026-09-20 13:00:00');


-- ------------------------------------------------------------
-- 4. 验证数据分布
-- ------------------------------------------------------------

-- 4.1 按创建时间倒序（你列表接口的默认排序）
SELECT id, title, view_count, create_time FROM video ORDER BY create_time DESC;

-- 4.2 按播放量倒序
SELECT id, title, view_count, create_time FROM video ORDER BY view_count DESC;

-- ★ 对比上面两个结果：顺序应该【完全不同】。
--   如果一样，说明测试数据设计失败，无法验证排序是否生效。

-- 4.3 按播放量升序（验证 asc/desc 都能用）
SELECT id, title, view_count FROM video ORDER BY view_count ASC;

-- 4.4 模糊搜索：标题含 "Redis"
SELECT id, title FROM video WHERE title LIKE '%Redis%';
-- 期望 2 条

-- 4.5 模糊搜索：标题含 "测试"（只有第 10 条标题含这个词）
SELECT id, title FROM video WHERE title LIKE '%测试%';
-- 期望 1 条

-- 4.5b 模糊搜索：标题含 "视频"（多条命中，更能验证模糊匹配）
SELECT id, title FROM video WHERE title LIKE '%视频%';
-- 期望 4 条（id 3、7、8、10）

-- 4.6 按作者筛选
SELECT u.nickname, v.id, v.title
FROM video v LEFT JOIN `user` u ON v.user_id = u.id
WHERE v.user_id = (SELECT id FROM `user` WHERE username = 'alice');
-- 期望 alice 的 4 条

-- 4.7 按状态筛选
SELECT status, COUNT(*) FROM video GROUP BY status;
-- 期望：status=2 有 8 条，status=1 有 1 条，status=4 有 1 条

-- 4.8 分页：第 1 页和第 2 页应该不重叠
SELECT id, title FROM video ORDER BY create_time DESC LIMIT 0, 5;
SELECT id, title FROM video ORDER BY create_time DESC LIMIT 5, 5;


-- ============================================================
-- 附：清理脚本（需要重新开始时用）
-- ============================================================
-- DELETE FROM video;
-- DELETE FROM `user` WHERE username IN ('alice', 'bob');
-- ALTER TABLE video AUTO_INCREMENT = 1;
-- ALTER TABLE `user` AUTO_INCREMENT = 1;

-- 注意：使用这些测试用户登录是无效的（密码是未加密的占位符），
--       Week 1 实现密码加密后需要重置或重新注册。
