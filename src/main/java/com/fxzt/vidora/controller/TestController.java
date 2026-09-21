package com.fxzt.vidora.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

@RestController
public class TestController {

    @Autowired
    StringRedisTemplate redis;

    @GetMapping("/test")
    public String test() {
        List<String> list = List.of("a", "b", "c");
        return list.toString();
    }

    @GetMapping("/test/redis")
    public String testRedis() {
        redis.opsForValue().set("vidora:test:hello","vidora", Duration.ofMinutes(10));
        String value = redis.opsForValue().get("vidora:test:hello");
        long ttl = redis.getExpire("vidora:test:hello");
        return "值" + value + "/剩余时间" + ttl;
    }
}
