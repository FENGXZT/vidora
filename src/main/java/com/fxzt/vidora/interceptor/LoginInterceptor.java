package com.fxzt.vidora.interceptor;

import com.fxzt.vidora.exception.UnauthorizedException;
import com.fxzt.vidora.util.JwtUtil;
import com.fxzt.vidora.util.UserContextUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.regex.Pattern;

@Component
public class LoginInterceptor implements HandlerInterceptor {
    @Autowired
    private JwtUtil jwtUtil;

    private static final Pattern ANON_READ = Pattern.compile("^/video$|^/video/feed$|^/video/\\d+$|^/video/\\d+/comment$");

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String url = request.getRequestURI();
        boolean optional = "GET".equals(request.getMethod()) && ANON_READ.matcher(url).matches();
        String token = resolveToken(request);

        if (token == null) {
            if (optional) return true;
            throw new UnauthorizedException("未登录");
        }
        try {
            Claims value = jwtUtil.getPayload(token);
            UserContextUtil.set(value);
        } catch (Exception e) {
            if (optional) return true;
            throw e;
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 请求完成后的逻辑
        UserContextUtil.remove();
    }

    private String resolveToken(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.regionMatches(true, 0, "Bearer ", 0, 7)) {
            token = token.substring(7).trim();
        }
        if (token == null || token.isBlank()) {
            token = request.getHeader("token");
        }
        if (token == null || token.isBlank()) return null;
        return token;
    }
}
