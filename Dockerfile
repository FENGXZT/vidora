# syntax=docker/dockerfile:1.6
# ↑ 必须在第一行：启用 BuildKit 的 --mount=type=cache（依赖层缓存）。Docker 23+ 默认就是 BuildKit。

# ============================================================
# 阶段 1：构建（大：JDK + Maven + 源码 + 依赖，但只存在于构建机）
# ============================================================
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

# ① 只拷 pom.xml：这一层只在 pom 变化时才失效 → 依赖缓存的命中率最高
COPY pom.xml .

# ② 预下载依赖。--mount=type=cache 的目录【不进镜像层】，能跨构建复用
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp dependency:go-offline

# ③ 再拷源码：改 Java 代码不会让 ①② 两层失效
COPY src ./src

# ④ 打包。-DskipTests 的理由见 13- §4.2
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp -DskipTests package

# ============================================================
# 阶段 2：运行（小：只有 JRE + 一个 jar，没有 Maven / 源码 / 依赖目录）
# ============================================================
FROM eclipse-temurin:21-jre-jammy AS runtime

ENV TZ=Asia/Shanghai \
    LANG=C.UTF-8 \
    JAVA_OPTS="-XX:MaxRAMPercentage=65 -XX:+ExitOnOutOfMemoryError -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/app/logs"

# curl：只为 HEALTHCHECK；tzdata：让 TZ 生效
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl tzdata \
 && ln -snf /usr/share/zoneinfo/$TZ /etc/localtime \
 && echo $TZ > /etc/timezone \
 && rm -rf /var/lib/apt/lists/*

# 非 root 运行。UID/GID 固定成 10001，方便在宿主机上给挂载目录授权
RUN groupadd -g 10001 vidora \
 && useradd -u 10001 -g vidora -m -s /usr/sbin/nologin vidora

WORKDIR /app
RUN mkdir -p /app/logs /app/tmp && chown -R vidora:vidora /app

# 只从构建阶段"搬"这一个 jar 过来
COPY --from=build --chown=vidora:vidora /build/target/*.jar /app/app.jar

USER vidora
EXPOSE 8080

# 用 liveness 而不是完整 health：见 13- §6.1 ③ 末尾的解释
HEALTHCHECK --interval=15s --timeout=5s --start-period=90s --retries=5 \
  CMD curl -fsS http://localhost:8080/actuator/health/liveness || exit 1

# exec 形式：让 java 成为 PID 1，才能收到 SIGTERM → 优雅停机才会发生
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Djava.io.tmpdir=/app/tmp -jar /app/app.jar"]
```