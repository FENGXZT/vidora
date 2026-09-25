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

@Component
public class LoginInterceptor implements HandlerInterceptor {
    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 预处理逻辑
        String token = request.getHeader("token");
        if (token == null) throw new UnauthorizedException("未登录");
        jwtUtil.validateToken(token);
        Claims value = jwtUtil.getPayload(token);
        UserContextUtil.set(value);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 请求完成后的逻辑
        UserContextUtil.remove();
    }
}
