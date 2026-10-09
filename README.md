# Vidora · 视频投稿与播放平台

一个前后端分离的视频投稿与播放平台，后端基于 Spring Boot。

支持用户注册登录、视频投稿、**分片上传 / 断点续传 / 秒传**，点赞 / 收藏 / 评论 / 关注 / 浏览记录，
以及基于 Redis 的缓存、异步落库、热度分物化与推荐流。

> 📌 **项目文档**
> - [数据库结构](docs/schema.sql) —— 建库脚本（**只含结构，不含数据**）
> - [接口契约（OpenAPI）](docs/vidora-openapi.json) —— 接口定义与示例
> - [测试数据](docs/test-data.sql) / [压测数据](docs/bench-data.sql) —— ⚠️ 见下方「数据说明」
>
> ⚠️ **当前状态：本地开发 / 本地 Docker 演练**，**尚未部署到服务器**（见 §部署）。

---

## 技术栈

| 层次 | 技术 |
|---|---|
| 语言 | Java 21 |
| 框架 | Spring Boot 4.1.1 · Spring MVC · MyBatis |
| 数据库 | MySQL 8 |
| 缓存 | Redis 7（Lettuce 客户端） |
| 对象存储 | MinIO（通过 **AWS S3 SDK v2** 接入，标准 S3 协议） |
| 鉴权 | JWT（jjwt 0.13） |
| 分页 | PageHelper + **自研游标分页**（时间型 / 分数型两套） |
| 构建 | Maven |
| 部署 | Docker（**规划中**） |

---

## 功能清单

### ✅ 已完成

**用户与鉴权**

- 用户注册 —— BCrypt 密码加密、用户名唯一索引、参数校验
- 用户登录 —— 校验密码后签发 JWT；登录失败统一提示（防用户名枚举）
- 统一鉴权拦截器；**身份（userId）一律从 token 解析，不信任客户端输入**
- 匿名可读四个接口（仅 GET）：`/video`、`/video/feed`、`/video/{id}`、`/video/{id}/comment`

**用户中心与社交**

- 用户中心 —— `GET /user/me`、`PUT /user/profile`（改昵称 / 头像；改昵称会 bump 搜索缓存版本号）
- 关注 / 取消关注 —— **幂等**（唯一键 + `ON DUPLICATE KEY UPDATE`，重复关注仍返回成功）
- 我关注的人 —— 用户维度游标分页

**视频浏览**

- 视频列表 —— 分页 + 标题/作者模糊搜索 + 动态排序（**白名单校验**，防 SQL 注入）
- 视频详情 —— Redis 缓存（TTL 2h），缓存故障时自动降级查库
- 我的投稿 —— 含上传中、已下架的视频（与公开列表语义不同）
- **首页推荐流** `GET /video/feed` —— `sort=latest`（按发布时间）/ `sort=hot`（按热度分），游标分页

**视频互动**

- 点赞 / 取消点赞、收藏 / 取消收藏 —— **幂等**
- 评论 —— 发布 / 列表 / 删除；列表用时间型复合游标分页；返回 `mine` 标记但不暴露他人 userId
- 个人列表 —— 我点赞的 / 我收藏的 / 我关注的 / **浏览记录**

**播放量与热度**

- **播放量** —— Redis 计数（`INCR`），30 秒异步回写 MySQL；整点全量对账
- **浏览记录** —— Redis ZSet（**写入期保证 score 唯一**），最多 1000 条 / 人，TTL 30 天
- **热度分物化** —— `(播放×1 + 点赞×3 + 收藏×5) / (发布小时数 + 2)^1.5 × 1000`，
  每 5 分钟全表重算；按 `(hot_score, id)` **复合游标**分页（同分极多，单分量会漏数据）

**视频上传**

- 简单上传（小文件）
- **分片上传** —— 初始化 / 上传分片 / 合并 / 放弃；**每片固定 5 MiB**（最后一片除外），单文件上限 **1 GB**
- **断点续传** —— 中断后只补缺失的分片
- **秒传** —— 基于文件 MD5 去重，命中直接复用存储对象
- 分片完整性校验（缺片拒绝合并；etag 以服务端 `ListParts` 为准）
- 上传会话失效自动降级为重新上传
- 上传会话定时清理（每天凌晨 3 点清理超 24h 未完成的记录）

**对象存储**

- 私有 Bucket + **预签名 URL** 访问（URL 临时生成，不落库）；有效期按场景分级
- 数据库只存 object key，不存完整 URL

### 📋 规划中

- **Docker 部署上线** —— 本地 Docker Compose 全链路演练已通过；服务器部署方案已成型，尚未执行
- 生产环境收尾项（HTTPS / 域名、CI/CD、监控告警、对象存储换云 OSS）

---

## 关键设计

| 设计点 | 说明 |
|---|---|
| **统一返回结构** | `Result<T>` 统一 code / msg / data |
| **错误码约定** | `@Valid` 参数校验失败 → **HTTP 400**；业务异常 → **HTTP 200 + `code:0`**；405 / 404 走真实状态码 |
| **全局异常处理** | 区分业务异常 / 参数校验异常 / 兜底异常；日志分级（预期内 warn 不打堆栈，意外 error 打堆栈） |
| **身份不可伪造** | `userId` 从 JWT 解析并存入 ThreadLocal；请求体、查询参数里传 `userId` 无效 |
| **缓存降级** | Redis 不可用时：读缓存视为未命中、写缓存失败忽略 —— 缓存是"加速器"不是"必需品" |
| **缓存只存事实** | 缓存里存 object key，**不存预签名 URL**（URL 每次读时现签） |
| **预签名 URL** | Bucket 保持私有；访问时临时签名，有效期按场景分级，过期自动失效 |
| **存储解耦** | 数据库只存 object key；底层存储可平滑切换（已实测从 MinIO SDK 迁到标准 S3 SDK，业务代码与数据零改动） |
| **游标分页** | **时间型** `(create_time, id)` 与**分数型** `(score, id)` 两套；复合游标两分量必须**同源**（逐列写"表名.列名"） |
| **分数游标的两种情况** | 浏览时间戳可**在写入期保证唯一**（同毫秒顺延 1ms，Lua 原子完成）→ 单分量够用；<br>热度分是**算出来的整数**、同分是常态 → **必须带 id 做平局决胜** |
| **异步落库 + 对账** | 计数先写 Redis 再异步回写 MySQL（≤60s 滞后）；整点全量对账兜底漂移 |
| **防 SQL 注入** | 动态排序字段走白名单校验，用户输入不直接进入 SQL |
| **分片上传** | 规避单请求大小限制，且失败只重传单个分片 |
| **秒传的边界** | 复用**存储对象**，但每个用户拥有**独立业务记录**（作者、播放量、权限各自独立） |
| **定时任务** | 7 个 `@Scheduled`（播放量 30s / 点赞 60s / 收藏 60s / 两个整点全量对账 / 热度 5min / 上传会话清理每天 3 点），调度池独立配置 |

---

## 快速开始

### 依赖

- JDK 21
- MySQL 8
- Redis 7
- MinIO（或任意 S3 兼容对象存储）

### 本地启动中间件

仓库里有两份编排文件：`docker-compose.yml`（**开发版**）与 `docker-compose.prod.yml`（**生产版**）。

> ⚠️ **开发版 `docker-compose.yml` 默认【不发布 MinIO 的 9000 端口】**（那一行是注释状态）。
> 而预签名 URL 的 host 就是 9000 ⇒ 用它起中间件时，**封面 / 视频在浏览器里会打不开**。
> 要么用生产版：`docker compose -f docker-compose.prod.yml up -d mysql redis minio`，
> 要么按下面的 `docker run` 手工起（不依赖编排文件）。

```powershell
# Redis
docker run -d --name vidora-redis -p 6379:6379 --restart unless-stopped redis:7

# MinIO（注意镜像在 quay.io，不是 Docker Hub）
docker run -d --name vidora-minio `
  -p 9000:9000 -p 9001:9001 `
  --restart unless-stopped `
  -e MINIO_ROOT_USER=minioadmin `
  -e MINIO_ROOT_PASSWORD=minioadmin `
  -v vidora-minio-data:/data `
  quay.io/minio/minio server /data --console-address ":9001"
```

MinIO 控制台：`http://localhost:9001`，需创建两个私有 Bucket：`vidora-video`、`vidora-image`。

### 数据库

```sql
CREATE DATABASE IF NOT EXISTS vidora DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```

然后用 **`docs/schema.sql`** 建表（它**只含结构**，含 `hot_score` 与 `idx_status_hot`）：

```powershell
mysql -uroot -p vidora < docs/schema.sql
```

### ⚠️ 数据说明（**演示前必读**）

| 文件 | 内容 | 能不能用来演示 |
|---|---|---|
| `docs/schema.sql` | **只含结构**，无数据 | ✅ 建库用它 |
| `docs/test-data.sql`、`docs/bench-data.sql` | **测试/压测数据** | ❌ **别用来演示** |
| 根目录 `vidora_structure.sql` | 另一份结构文件 | ❌ **缺 `hot_score` 列**，按它建库 `sort=hot` 会直接报错 |

测试/压测数据的 `cover_key` / `video_key` 指向的是**编造的路径**，MinIO 里并不存在对应对象
⇒ 那些视频**封面 404、点开也播不了**。

**正确的演示数据做法**：走**真实上传流程**（`init` → 分片 → `complete`）传几个真实的视频与封面，
这样 object key 与 MinIO 里的对象天然对得上，**能点开、能播放**，热度分也会真的算出来。
建议传 **5~15 个**（首页一屏 + 翻页 + 推荐 Tab 都有内容），并在演示前自己点几个、给几条点赞收藏，
让热度分有区分度。

### 配置

敏感配置走环境变量（`.env` 已在 `.gitignore` 中；参考 `.env` 里的键名）：

```
# 数据源
SPRING_DATASOURCE_URL / SPRING_DATASOURCE_USERNAME / SPRING_DATASOURCE_PASSWORD

# Redis（★ TIMEOUT / CONNECT_TIMEOUT 没有默认值，缺了会启动失败）
SPRING_DATA_REDIS_HOST / SPRING_DATA_REDIS_PORT / SPRING_DATA_REDIS_PASSWORD
SPRING_DATA_REDIS_TIMEOUT / SPRING_DATA_REDIS_CONNECT_TIMEOUT

# 对象存储
MINIO_ENDPOINT            # 服务端访问地址（容器内用 http://minio:9000）
MINIO_PUBLIC_ENDPOINT     # ★ 浏览器能访问到的地址，预签名 URL 的 host 就是它
MINIO_ACCESS_KEY / MINIO_SECRET_KEY
MINIO_VIDEO_BUCKET_NAME / MINIO_IMAGE_BUCKET_NAME

# 鉴权（JWT_SECRET 用 openssl rand -base64 32 生成）
JWT_SECRET / JWT_EXPIRATION
```

### 启动

```powershell
mvn spring-boot:run
```

---

## 接口速览

> 完整的参数、响应字段与示例见 [OpenAPI 描述](docs/vidora-openapi.json)。
> **匿名可读的只有 4 个 GET**；其余全部需要 `Authorization: Bearer <token>`（或 `token` 头）。

### 用户

| 方法 | 路径 | 说明 | 需登录 |
|---|---|---|---|
| POST | `/user/register` | 用户注册 | ❌ |
| POST | `/user/login` | 用户登录，返回 JWT | ❌ |
| GET | `/user/me` | 当前用户信息 | ✅ |
| PUT | `/user/profile` | 修改昵称 / 头像 | ✅ |
| GET | `/user/liked` | 我点赞过的视频（游标分页） | ✅ |
| GET | `/user/favorites` | 我收藏过的视频（游标分页） | ✅ |
| GET | `/user/history` | 浏览记录（分数游标分页） | ✅ |
| POST / DELETE | `/user/{id}/follow` | 关注 / 取消关注（幂等） | ✅ |
| GET | `/user/following` | 我关注的人（游标分页） | ✅ |

### 视频

| 方法 | 路径 | 说明 | 需登录 |
|---|---|---|---|
| GET | `/video` | 视频列表（分页 + 模糊搜索 + 排序） | ❌ |
| GET | `/video/{id}` | 视频详情（走 Redis 缓存；同时记录浏览记录） | ❌ |
| GET | `/video/feed` | 首页推荐流 `sort=latest` / `sort=hot`（游标分页） | ❌ |
| GET | `/video/mine` | 我的投稿 | ✅ |
| POST / DELETE | `/video/{id}/like` | 点赞 / 取消点赞（幂等） | ✅ |
| POST / DELETE | `/video/{id}/favorite` | 收藏 / 取消收藏（幂等） | ✅ |
| GET / POST | `/video/{id}/comment` | 评论列表（游标分页）/ 发表评论 | ❌ / ✅ |
| DELETE | `/video/{id}/comment/{commentId}` | 删除自己的评论 | ✅ |

> 📌 视频记录由 `POST /upload/video/init` 直接创建（`complete` 后置为可播放状态），**没有独立的"创建视频"接口**。

### 上传

| 方法 | 路径 | 说明 | 需登录 |
|---|---|---|---|
| POST | `/upload/image` | 图片上传（封面 / 头像） | ✅ |
| POST | `/upload/video` | 简单上传（小文件） | ✅ |
| POST | `/upload/video/init` | **初始化上传** —— 内部自动分流：秒传 / 断点续传 / 全新上传 | ✅ |
| POST | `/upload/video/part` | 上传分片（**每片固定 5 MiB**） | ✅ |
| POST | `/upload/video/complete` | 合并分片 | ✅ |
| POST | `/upload/video/abort` | 放弃上传并清理会话 | ✅ |

> 💡 `/upload/video/init` 返回的 `mode` 决定前端后续行为：
> - `instant` —— 秒传命中，无需上传
> - `resume` —— 断点续传，返回 `uploadedParts` 供前端跳过
> - `new` —— 全新上传
>
> 三种情况下后续都调用**同一个** `/upload/video/part`，区别只在于传哪些分片。

---

## 部署

**📋 规划中。**

已完成的是**本地 Docker Compose 全链路演练**（MySQL / Redis / MinIO / 应用四个容器 + Nginx 网关，
含预签名 URL 的"内网 endpoint / 公网 endpoint"双地址配置）。

**尚未执行**的是**服务器部署**（买机器、上传镜像、配 Nginx 与安全组、配置备份与回滚）。
完整部署方案与从零到上线的步骤已成型，落地后再把本节补全。
