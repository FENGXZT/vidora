package com.fxzt.vidora.controller;

import com.fxzt.vidora.common.Result;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

@Validated
@RestController
public class TestController {

    @Autowired
    StringRedisTemplate redis;

    @GetMapping("/test")
    public Result<List<String>> test() {
        List<String> list = List.of("a", "b", "c");
        return Result.success(list);
    }

    @GetMapping("/test/redis")
    public Result<String> testRedis() {
        redis.opsForValue().set("vidora:test:hello","vidora", Duration.ofMinutes(10));
        String value = redis.opsForValue().get("vidora:test:hello");
        long ttl = redis.getExpire("vidora:test:hello");
        return Result.success("值" + value + "/剩余时间" + ttl);
    }


    @PostMapping("/test/echo")
    public Result<String> testEcho(@NotBlank(message = "参数不能为空") String value) {
        return Result.success("success");
    }
}
