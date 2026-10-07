package com.fxzt.vidora.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class CacheHelper {
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public CacheHelper(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    /**
     * 从缓存中获取数据
     * @param key 缓存key
     * @param clazz 数据类型
     * @return 数据
     */
    public <T> T get(String key,Class<T> clazz){
        String json;
        try {
            json = redis.opsForValue().get(key);
            if (json != null) {
                return objectMapper.readValue(json,clazz);
            }
        }catch (DataAccessException e){
            log.warn("读缓存失败(Redis不可用)，返回null。key={}", key);
        }catch (Exception e) {
            log.error("读缓存失败(未知原因)。key={}", key, e);
        }
        return null;
    }

    /**
     * 从缓存中获取数据
     * @param key 缓存key
     * @param typeRef 数据类型
     * @return 数据
     */
    public <T> T get(String key, TypeReference<T> typeRef){
        String json;
        try {
            json = redis.opsForValue().get(key);
            if (json != null) {
                return objectMapper.readValue(json, typeRef);
            }
        } catch (DataAccessException e) {
            log.warn("读缓存失败(Redis不可用)，返回null。key={}", key);
        } catch (Exception e) {
            log.error("读缓存失败(未知原因)。key={}", key, e);
        }
        return null;
    }

    /**
     * 将数据写入缓存
     * @param key 缓存key
     * @param value 数据
     * @param time 缓存时间
     */
    public <T> void set(String key, T value , Duration time){
        if (value == null) {
            log.warn("异常数据，key={}", key);
            return;
        }
        if (time == null || time.isNegative() || time.isZero()){
            log.warn("非法缓存时间，key={}，time={}", key, time);
            return;
        }
        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(value), time);
        }catch ( DataAccessException e){
            log.warn("写缓存失败，key={}", key);
        }catch (Exception e) {
            log.error("写缓存失败(未知原因)，key={}", key, e);
        }
    }

    public Long incr(String key){
        try {
            return redis.opsForValue().increment(key);
        }catch (DataAccessException e){
            log.warn("缓存自增失败，key={}", key);
        }catch (Exception e) {
            log.error("缓存自增失败(未知原因)，key={}", key, e);
        }
        return null;
    }

    /**
     * 删除缓存
     * @param key 缓存key
     */
    public void delete(String key){
        try {
            redis.delete(key); // 删除缓存
        }catch (DataAccessException e){
            log.warn("删除缓存失败，key={}", key);
        } catch (Exception e) {
            log.error("删除缓存失败(未知原因)，key={}", key, e);
        }
    }

    /**
     * 添加缓存
     * @param key 缓存key
     * @param value 数据
     */
    public void add(String key,String value){
        try {
            redis.opsForSet().add(key, value);
        }catch (DataAccessException e){
            log.warn("添加缓存失败，key={}", key);
        } catch (Exception e) {
            log.error("添加缓存失败(未知原因)，key={}", key, e);
        }
    }

    /**
     * 删除缓存
     * @param key 缓存key
     * @param value 数据
     */
    public void rem(String key, String value){
        try {
            redis.opsForSet().remove(key, value);
        }catch (DataAccessException e){
            log.warn("删除缓存失败，key={}", key);
        }catch (Exception e) {
            log.error("删除缓存失败(未知原因)，key={}", key, e);
        }
    }

    /**
     * 判断 member 是否在 Set 中
     * @return 1 = 在，0 = 不在 / Redis 异常
     */
    public Integer isMember(String key, String member) {
        try {
            if (Boolean.FALSE.equals(redis.hasKey(key))) return null;
            Boolean result = redis.opsForSet().isMember(key, member);
            return Boolean.TRUE.equals(result) ? 1 : 0;
        } catch (DataAccessException e) {
            log.warn("判断 Set 成员失败(Redis不可用)，key={}", key);
            return null;
        } catch (Exception e) {
            log.error("判断 Set 成员失败(未知原因)，key={}", key, e);
            return null;
        }
    }

    /**
     * 添加缓存
     * @param key 缓存key
     * @param member 数据
     * @param score 分数
     * @param maxCount 最大数量
     * @param ttl 过期时间
     */
    public Long zAddMonotonicWithTtl(String key, String member, long score,int maxCount,  Duration ttl){
        if (ttl == null || ttl.isZero() || ttl.isNegative()){
            log.warn("TTL 非法，跳过写入：key={}, ttl={}", key, ttl);
            return null;
        }
        if (maxCount <= 0){
            log.warn("最大数量非法，跳过写入：key={}, maxCount={}", key, maxCount);
            return null;
        }
        try{
            DefaultRedisScript<String> script = new DefaultRedisScript<>(LUA_SCRIPT, String.class);
            String result = redis.execute(script,
                    Collections.singletonList(key),
                    String.valueOf(score),
                    member,
                    String.valueOf(maxCount),
                    String.valueOf(ttl.toSeconds()));
            return result == null ? null : Long.parseLong(result);
        }catch (DataAccessException e){
            log.warn("ZSet 写入失败：key={}, member={}", key, member, e);
            return null;
        }catch (Exception e){
            log.error("ZSet 写入失败(未知原因)：key={}, member={}", key, member, e);
            return null;
        }
    }

    /**
     * 获取缓存
     * @param key 缓存key
     * @param min 分数下限
     * @param max 分数上限
     * @param count 返回数量
     * @return 数据
     */
    public List<ScoredMember> zReverseRangeByScore(String key,double min, double max,int count){
        try{
            Set<ZSetOperations.TypedTuple<String>> tuples = redis.opsForZSet()
                    .reverseRangeByScoreWithScores(key,min,max,0,count);
            if (tuples == null) return List.of();
            return tuples.stream()
                    .map(t-> new ScoredMember(t.getValue(),
                            t.getScore() == null ? 0 : t.getScore()))
                    .sorted(Comparator.comparingDouble(ScoredMember::score).reversed())
                    .toList();
        }catch (DataAccessException e){
            log.warn("ZSet 读取失败：key={}", key, e);
            return null;        //redis故障返回null，方便区分
        }catch (Exception e){
            log.error("ZSet 读取失败(未知原因)：key={}", key, e);
            return null;
        }
    }

    /**
     * 带分数的成员
     */
    public  record ScoredMember(String member, double score){}

    /**
     * Lua脚本
     * 1. 获取当前最大分数
     * 2. 如果有最大分数，则将分数加1
     * 3. 添加成员
     * 4. 删除过期成员
     * 5. 设置过期时间
     * 6. 返回分数
     */
    private static final String LUA_SCRIPT = """
            local top = redis.call('ZREVRANGE', KEYS[1], 0, 0, 'WITHSCORES')
            local score = tonumber(ARGV[1])
            if top[2] ~= nil then
                local max = math.floor(tonumber(top[2]))
                if score <= max then score = max + 1 end
            end
            redis.call('ZADD', KEYS[1], score, ARGV[2])
            redis.call('ZREMRANGEBYRANK', KEYS[1], 0, -tonumber(ARGV[3]) - 1)
            redis.call('EXPIRE', KEYS[1], tonumber(ARGV[4]))
            return tostring(score)
            """;
}
