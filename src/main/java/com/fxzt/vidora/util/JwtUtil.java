package com.fxzt.vidora.util;

import com.fxzt.vidora.exception.UnauthorizedException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Date;

@Slf4j
@Component
public class JwtUtil {
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Duration expiration;

    private SecretKey key;

    @PostConstruct
    public void init() {
        key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    /**
     * 生成JWT令牌
     *
     * @return 生成的令牌
     */
    public String generateToken(Long id,String username){
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration.toMillis());
        return Jwts.builder()
                .claim("id", id)
                .claim("username", username)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * 解析JWT令牌
     *
     * @param token 令牌
     * @return 解析结果
     */
    public Jws<Claims> parseToken(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(key)               // 设置签名密钥
                .build()
                .parseSignedClaims(token);      // 解析 token

    }

    /**
     * 从JWT令牌中获取载荷
     *
     * @param token 令牌
     * @return 载荷
     */
    public Claims getPayload(String token) {
        try {
            return parseToken(token).getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("token 已过期");
            throw new UnauthorizedException("token 已过期");
        }
        catch (SignatureException e)  {
            log.warn("token 签名无效");
            throw new UnauthorizedException("token 签名无效");
        }
        catch (JwtException e){
            log.warn("token 无效");
            throw new UnauthorizedException("token 无效");
        }
    }
}
