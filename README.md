# Vidora · 视频投稿与播放平台

一个前后端分离的视频投稿与播放平台，后端基于 Spring Boot。

支持用户注册登录、视频投稿、**分片上传 / 断点续传 / 秒传**，以及基于 Redis 的缓存与降级。

> 📌 **项目文档**
> - [踩坑日志](docs/踩坑日志.md) —— 开发过程中遇到的真实问题与排查过程
> - [测试数据](docs/test-data.sql) —— 用于验证分页、模糊搜索、动态排序的测试数据

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
| 分页 | PageHelper |
| 构建 | Maven |
| 部署 | Docker（进行中） |

---

## 功能清单

### ✅ 已完成

**用户与鉴权**

- 用户注册 —— BCrypt 密码加密、用户名唯一索引、参数校验
- 用户登录 —— 校验密码后签发 JWT；登录失败统一提示（防用户名枚举）
- 统一鉴权拦截器；**身份（userId）一律从 token 解析，不信任客户端输入**

**视频浏览与搜索**

- 视频列表 —— 分页 + 标题/作者模糊搜索 + 动态排序（白名单防 SQL 注入）
- 视频详情 —— Redis 缓存，缓存故障时自动降级查库
- 我的投稿 —— 含上传中、已下架的视频（与公开列表语义不同）

**视频上传**

- 简单上传（小文件）
- **分片上传** —— 初始化 / 上传分片 / 合并 / 放弃
- **断点续传** —— 中断后只补缺失的分片
- **秒传** —— 基于文件 MD5 去重，命中直接复用存储对象
- 分片完整性校验（缺片拒绝合并）
- 上传会话失效自动降级为重新上传
- 上传会话定时清理（每天凌晨 3 点清理超 24h 未完成的记录）

**对象存储**

- 私有 Bucket + **预签名 URL** 访问（URL 临时生成，不落库）
- 数据库只存 object key，不存完整 URL

### ⏳ 进行中

- Docker 部署上线

### 📋 规划中

- 播放量统计（Redis 计数 + 异步落库）
- 点赞 / 评论

---

## 关键设计

| 设计点 | 说明 |
|---|---|
| **统一返回结构** | `Result<T>` 统一 code / msg / data |
| **全局异常处理** | 区分业务异常 / 参数校验异常 / 兜底异常；日志分级（预期内 warn 不打堆栈，意外 error 打堆栈） |
| **身份不可伪造** | `userId` 从 JWT 解析并存入 ThreadLocal；请求体、查询参数里传 `userId` 无效 |
| **缓存降级** | Redis 不可用时：读缓存视为未命中、写缓存失败忽略 —— 缓存是"加速器"不是"必需品" |
| **预签名 URL** | Bucket 保持私有；访问时临时签名，有效期可控，过期自动失效 |
| **存储解耦** | 数据库只存 object key；底层存储可平滑切换（已实测从 MinIO SDK 迁到标准 S3 SDK，业务代码与数据零改动） |
| **防 SQL 注入** | 动态排序字段走白名单校验，用户输入不直接进入 SQL |
| **分片上传** | 规避单请求大小限制，且失败只重传单个分片 |
| **秒传的边界** | 复用**存储对象**，但每个用户拥有**独立业务记录**（作者、播放量、权限各自独立） |

---

## 快速开始

> ⏳ 完整部署文档将在 Docker 化完成后补全。

### 依赖

- JDK 21
- MySQL 8
- Redis 7
- MinIO（或任意 S3 兼容对象存储）

### 本地启动中间件

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

### 配置

敏感配置走环境变量（`.env` 已在 `.gitignore` 中）：

```
JWT_SECRET=<openssl rand -base64 32>
MINIO_ENDPOINT=http://localhost:9000
MINIO_ACCESS_KEY=...
MINIO_SECRET_KEY=...
```

### 启动

```powershell
mvn spring-boot:run
```

---

## 接口速览

### 用户

| 方法 | 路径 | 说明 | 需登录 |
|---|---|---|---|
| POST | `/user/register` | 用户注册 | ❌ |
| POST | `/user/login` | 用户登录，返回 JWT | ❌ |

### 视频

| 方法 | 路径 | 说明 | 需登录 |
|---|---|---|---|
| GET | `/video` | 视频列表（分页 + 模糊搜索 + 排序） | ✅ |
| GET | `/video/{id}` | 视频详情（走 Redis 缓存） | ✅ |
| GET | `/video/mine` | 我的投稿 | ✅ |
| POST | `/video` | 新增视频记录 | ✅ |

### 上传

| 方法 | 路径 | 说明 | 需登录 |
|---|---|---|---|
| POST | `/upload/image` | 图片上传（封面 / 头像） | ✅ |
| POST | `/upload/video` | 简单上传（小文件） | ✅ |
| POST | `/upload/video/init` | **初始化上传** —— 内部自动分流：秒传 / 断点续传 / 全新上传 | ✅ |
| POST | `/upload/video/part` | 上传分片 | ✅ |
| POST | `/upload/video/complete` | 合并分片 | ✅ |
| POST | `/upload/video/abort` | 放弃上传并清理会话 | ✅ |

> 💡 `/upload/video/init` 返回的 `mode` 决定前端后续行为：
> - `instant` —— 秒传命中，无需上传
> - `resume` —— 断点续传，返回 `uploadedParts` 供前端跳过
> - `new` —— 全新上传
>
> 三种情况下后续都调用**同一个** `/upload/video/part`，区别只在于传哪些分片。
