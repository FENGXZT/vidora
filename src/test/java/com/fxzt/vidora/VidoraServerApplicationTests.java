package com.fxzt.vidora;

import com.fxzt.vidora.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@SpringBootTest
class VidoraServerApplicationTests {

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    void contextLoads() {
    }

    @Test
    void testJWTUtil() {
        log.info(jwtUtil.generateToken(1L, "test"));
    }

    @Test
    void testValidateToken() {
        String token = jwtUtil.generateToken(2L, "test");
        assertTrue(jwtUtil.validateToken(token));
        log.info(token);
    }

    @Test
    void testValidateTokenError() {
        String token = jwtUtil.generateToken(2L, "test");
        assertTrue(jwtUtil.validateToken(token+"1"));
        log.info(token);
    }

    @Test
    void testParseToken() {
        String token = jwtUtil.generateToken(3L, "test");
        assertNotNull(jwtUtil.getPayload(token));
        log.info(token);
    }
    @Test
    void testParseTokenError() {
        String token = jwtUtil.generateToken(3L, "test");
        assertNotNull(jwtUtil.getPayload(token + "1"));
        log.info(token);
    }
}
