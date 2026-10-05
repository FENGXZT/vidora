package com.fxzt.vidora.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

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
}
