package com.pengyouquan.english.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * API 请求拦截器
 * 统计每个 API 端点的调用次数和最后调用时间，用于管理后台监控
 */
@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {

    /** 全站总请求数 */
    private final AtomicLong totalRequests = new AtomicLong(0);

    /** 按路径统计的请求数 */
    private final ConcurrentHashMap<String, AtomicLong> pathCounts = new ConcurrentHashMap<>();

    /** 每个路径的最后调用时间戳 */
    private final ConcurrentHashMap<String, Long> lastCalledAt = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        totalRequests.incrementAndGet();

        String key = method + " " + path;
        pathCounts.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
        lastCalledAt.put(key, System.currentTimeMillis());

        return true;
    }

    /**
     * 获取总请求数
     */
    public long getTotalRequests() {
        return totalRequests.get();
    }

    /**
     * 获取按路径统计的请求数（按调用次数降序排序）
     */
    public Map<String, Long> getPathCounts() {
        return pathCounts.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().get(),
                        (a, b) -> a,
                        () -> new java.util.LinkedHashMap<>()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        java.util.LinkedHashMap::new));
    }

    /**
     * 获取每个路径的最后调用时间（毫秒时间戳）
     */
    public Map<String, Long> getLastCalledAt() {
        return new java.util.LinkedHashMap<>(lastCalledAt);
    }
}
