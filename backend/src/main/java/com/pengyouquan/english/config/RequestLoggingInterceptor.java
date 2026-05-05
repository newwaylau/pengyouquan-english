package com.pengyouquan.english.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * API 请求拦截器
 * 统计每个 API 端点的调用次数、响应时间和最后调用时间，用于管理后台监控
 */
@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {

    /** 全站总请求数 */
    private final AtomicLong totalRequests = new AtomicLong(0);

    /** 按路径统计的请求数 */
    private final ConcurrentHashMap<String, AtomicLong> pathCounts = new ConcurrentHashMap<>();

    /** 每个路径的最后调用时间戳 */
    private final ConcurrentHashMap<String, Long> lastCalledAt = new ConcurrentHashMap<>();

    /** 线程局部变量：记录每个请求的开始时间（纳秒） */
    private final ThreadLocal<Long> startTimeNanos = new ThreadLocal<>();

    /** 每条路径的最近100次响应耗时列表（毫秒） */
    private final ConcurrentHashMap<String, List<Long>> pathLatencies = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        totalRequests.incrementAndGet();

        String key = method + " " + path;
        pathCounts.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
        lastCalledAt.put(key, System.currentTimeMillis());

        // 记录请求开始时间
        startTimeNanos.set(System.nanoTime());

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        Long start = startTimeNanos.get();
        if (start != null) {
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            String key = request.getMethod() + " " + request.getRequestURI();

            // 线程安全地更新该路径的耗时列表，保留最近100次
            pathLatencies.compute(key, (k, list) -> {
                if (list == null) {
                    list = Collections.synchronizedList(new ArrayList<>());
                }
                list.add(elapsedMs);
                // 超出100条时移除最旧的
                while (list.size() > 100) {
                    list.remove(0);
                }
                return list;
            });
        }
        // 清理 ThreadLocal，避免内存泄漏
        startTimeNanos.remove();
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

    /**
     * 获取各路径的平均响应时间（毫秒）
     * 只返回有数据的路径，按平均耗时降序排列
     */
    public Map<String, Long> getAvgResponseTime() {
        Map<String, Long> result = new LinkedHashMap<>();
        pathLatencies.forEach((key, latencies) -> {
            if (latencies != null && !latencies.isEmpty()) {
                long sum = 0;
                int count = 0;
                // 使用 synchronized 快照避免并发干扰
                synchronized (latencies) {
                    for (long ms : latencies) {
                        sum += ms;
                        count++;
                    }
                }
                result.put(key, count > 0 ? sum / count : 0);
            }
        });
        // 按平均耗时降序排序
        return result.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new));
    }
}
