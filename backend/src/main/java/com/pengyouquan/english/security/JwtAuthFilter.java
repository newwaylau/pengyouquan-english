package com.pengyouquan.english.security;

import com.pengyouquan.english.service.OnlineUserTracker;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器
 * - 从请求头 Authorization: Bearer *** 中提取 token
 * - 验证 token 有效性
 * - 把用户 ID 放入请求属性（供 controller 使用）
 * - 记录用户活跃时间到 {@link OnlineUserTracker}
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final OnlineUserTracker onlineUserTracker;

    public JwtAuthFilter(JwtUtil jwtUtil, OnlineUserTracker onlineUserTracker) {
        this.jwtUtil = jwtUtil;
        this.onlineUserTracker = onlineUserTracker;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtUtil.validateToken(token)) {
                Long userId = jwtUtil.getUserIdFromToken(token);
                request.setAttribute("userId", userId);
                // 记录用户活跃时间
                onlineUserTracker.recordActivity(userId);
            }
            // Token 无效——不设置 userId，controller 自行检查
        }
        // 无 token——直接放行，controller 自行判断是否需要登录

        filterChain.doFilter(request, response);
    }
}
