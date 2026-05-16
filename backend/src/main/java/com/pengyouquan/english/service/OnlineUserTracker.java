package com.pengyouquan.english.service;

import com.pengyouquan.english.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 在线用户追踪器
 * <p>
 * 记录每个用户的最后一次 API 请求时间，定期清理过期会话。
 * 管理员可通过 SSE 订阅实时在线人数。
 */
@Service
public class OnlineUserTracker {

    private static final Logger log = LoggerFactory.getLogger(OnlineUserTracker.class);

    /** 5 分钟无活动视为离线 */
    private static final long OFFLINE_THRESHOLD_MS = 5 * 60 * 1000L;

    /** 每 3 秒推送一次在线人数到所有连接的 SS E */
    private static final long PUSH_INTERVAL_MS = 3000L;

    /** userId → lastActiveEpochMs */
    private final ConcurrentHashMap<Long, Long> activeUsers = new ConcurrentHashMap<>();

    /** 所有管理员 SSE 连接 */
    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    private final JwtUtil jwtUtil;
    private final UserService userService;

    public OnlineUserTracker(JwtUtil jwtUtil, UserService userService) {
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }

    /**
     * 记录用户活动时间
     */
    public void recordActivity(Long userId) {
        if (userId != null) {
            activeUsers.put(userId, System.currentTimeMillis());
        }
    }

    /**
     * 获取当前在线人数
     */
    public int getOnlineCount() {
        long now = System.currentTimeMillis();
        long cutoff = now - OFFLINE_THRESHOLD_MS;
        // 清理过期条目并计数
        int count = 0;
        for (Map.Entry<Long, Long> entry : activeUsers.entrySet()) {
            if (entry.getValue() < cutoff) {
                activeUsers.remove(entry.getKey(), entry.getValue());
            } else {
                count++;
            }
        }
        return count;
    }

    /**
     * 管理员订阅在线人数实时推送
     * token 通过查询参数传递（因为 EventSource 不支持自定义 header）
     */
    public SseEmitter subscribe(String token) {
        // 验证 token 和管理员身份
        if (token == null || !jwtUtil.validateToken(token)) {
            SseEmitter err = new SseEmitter(0L);
            try {
                err.send(SseEmitter.event().name("error").data("{\"error\":\"unauthorized\"}"));
            } catch (IOException ignored) {}
            err.completeWithError(new SecurityException("未授权"));
            return err;
        }

        Long userId = jwtUtil.getUserIdFromToken(token);
        if (userId == null) {
            SseEmitter err = new SseEmitter(0L);
            try {
                err.send(SseEmitter.event().name("error").data("{\"error\":\"unauthorized\"}"));
            } catch (IOException ignored) {}
            err.completeWithError(new SecurityException("未授权"));
            return err;
        }

        // 检查是否是管理员
        try {
            userService.checkAdmin(userId);
        } catch (Exception e) {
            SseEmitter err = new SseEmitter(0L);
            try {
                err.send(SseEmitter.event().name("error").data("{\"error\":\"forbidden\"}"));
            } catch (IOException ignored) {}
            err.completeWithError(new SecurityException("无权限"));
            return err;
        }

        // 创建 SSE 连接，timeout = 0 表示永不超时
        SseEmitter emitter = new SseEmitter(0L);

        // 注册清理回调
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));

        emitters.add(emitter);
        log.info("管理员 SSE 连接建立，当前连接数: {}", emitters.size());

        // 立即发送一条初始化数据
        try {
            emitter.send(SseEmitter.event()
                    .name("init")
                    .data("{\"onlineCount\":" + getOnlineCount() + ",\"connectedAt\":" + System.currentTimeMillis() + "}"));
        } catch (IOException e) {
            emitters.remove(emitter);
        }

        return emitter;
    }

    /**
     * 每 3 秒清理过期用户并推送在线人数到所有管理员
     */
    @Scheduled(fixedRate = PUSH_INTERVAL_MS)
    public void pushOnlineCount() {
        if (emitters.isEmpty()) return;

        int count = getOnlineCount();
        String data = "{\"onlineCount\":" + count + ",\"timestamp\":" + System.currentTimeMillis() + "}";

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("online")
                        .data(data));
            } catch (IOException e) {
                emitters.remove(emitter);
            }
        }
    }
}
