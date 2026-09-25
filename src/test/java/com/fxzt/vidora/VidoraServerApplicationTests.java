package com.fxzt.vidora;

import com.fxzt.vidora.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.crypto.SecretKey;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

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
        assertThrows(Exception.class, () -> jwtUtil.validateToken(token+"1"));
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

    @Test
    void testParseUnsecured() {
        Date now = new Date();
        String token = Jwts.builder()
                .header().add("type", "JWT").add("alg", "none").and()
                .issuedAt(now).expiration(new Date(now.getTime() + 86400000))
                .compact();

        Object object = Jwts.parser()
                .unsecured()
                .build()
                .parse(token);

        assertNotNull(object);
    }

    @Test
    void testParseSignedClaimsRejectsUnsecured() {
        Date now = new Date();
        String token = Jwts.builder()
                .header().add("type", "JWT").add("alg", "none").and()
                .issuedAt(now).expiration(new Date(now.getTime() + 86400000))
                .compact();

        assertThrows(UnsupportedJwtException.class, () -> {
            Jwts.parser()
                    .unsecured()
                    .build()
                    .parseSignedClaims(token);   // 必须抛异常
        });
    }
}
