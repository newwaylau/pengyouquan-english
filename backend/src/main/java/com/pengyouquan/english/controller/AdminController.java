package com.pengyouquan.english.controller;

import com.pengyouquan.english.config.RequestLoggingInterceptor;
import com.pengyouquan.english.dto.ApiResponse;
import com.pengyouquan.english.repository.PracticeLogRepository;
import com.pengyouquan.english.repository.SentenceRepository;
import com.pengyouquan.english.repository.ShowRepository;
import com.pengyouquan.english.repository.UserRepository;
import com.pengyouquan.english.security.CurrentUserId;
import com.pengyouquan.english.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 管理后台统计接口
 * 仅管理员有权限
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserService userService;
    private final PracticeLogRepository practiceLogRepository;
    private final UserRepository userRepository;
    private final ShowRepository showRepository;
    private final SentenceRepository sentenceRepository;
    private final RequestLoggingInterceptor requestLoggingInterceptor;

    public AdminController(UserService userService,
                           PracticeLogRepository practiceLogRepository,
                           UserRepository userRepository,
                           ShowRepository showRepository,
                           SentenceRepository sentenceRepository,
                           RequestLoggingInterceptor requestLoggingInterceptor) {
        this.userService = userService;
        this.practiceLogRepository = practiceLogRepository;
        this.userRepository = userRepository;
        this.showRepository = showRepository;
        this.sentenceRepository = sentenceRepository;
        this.requestLoggingInterceptor = requestLoggingInterceptor;
    }

    /** 管理后台统计数据总览 */
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        userService.checkAdmin(userId);

        Map<String, Object> result = new LinkedHashMap<>();

        // 总体概览
        long totalUsers = userRepository.count();
        long totalPractices = practiceLogRepository.count();
        long todayPractices = practiceLogRepository
                .countByUserIdAndPracticedAtAfter(0L, LocalDate.now().atStartOfDay());
        result.put("totalUsers", totalUsers);
        result.put("totalPractices", totalPractices);
        result.put("todayPractices", todayPractices);

        // 每日练习趋势（近30天）
        LocalDateTime since30 = LocalDate.now().minusDays(30).atStartOfDay();
        List<Object[]> dailyData = practiceLogRepository.dailyPracticeCount(since30);
        List<Map<String, Object>> dailyTrend = new ArrayList<>();
        for (Object[] row : dailyData) {
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("day", row[0].toString());
            d.put("count", ((Number) row[1]).longValue());
            dailyTrend.add(d);
        }
        result.put("dailyTrend", dailyTrend);

        // 每日正确率趋势（近30天）
        List<Object[]> accData = practiceLogRepository.dailyAccuracy(since30);
        List<Map<String, Object>> accuracyTrend = new ArrayList<>();
        for (Object[] row : accData) {
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("day", row[0].toString());
            long correct = ((Number) row[1]).longValue();
            long total = ((Number) row[2]).longValue();
            d.put("accuracy", total > 0 ? Math.round(correct * 100.0 / total) : 0);
            d.put("total", total);
            d.put("correct", correct);
            accuracyTrend.add(d);
        }
        result.put("accuracyTrend", accuracyTrend);

        // 练习模式分布
        List<Object[]> modeData = practiceLogRepository.modeDistribution();
        List<Map<String, Object>> modeDist = new ArrayList<>();
        for (Object[] row : modeData) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("mode", row[0] != null ? row[0].toString() : "未知");
            m.put("count", ((Number) row[1]).longValue());
            modeDist.add(m);
        }
        result.put("modeDistribution", modeDist);

        // 剧集热度排行（按句子数排序）
        var shows = showRepository.findAllByOrderByImportedAtDesc();
        List<Map<String, Object>> showRanking = new ArrayList<>();
        for (var show : shows) {
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("id", show.getId());
            s.put("name", show.getName());
            s.put("sentenceCount", sentenceRepository.countByShowId(show.getId()));
            showRanking.add(s);
        }
        showRanking.sort((a, b) -> Long.compare(
                (Long) b.get("sentenceCount"), (Long) a.get("sentenceCount")));
        result.put("showRanking", showRanking);

        return ApiResponse.success(result);
    }

    /**
     * 获取系统信息
     * 返回应用名、版本、启动时间、JVM版本等
     */
    @GetMapping("/info")
    public ApiResponse<Map<String, Object>> info(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        userService.checkAdmin(userId);

        Map<String, Object> result = new LinkedHashMap<>();
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();

        result.put("appName", "朋友圈英语");
        result.put("appVersion", "0.0.1-SNAPSHOT");
        result.put("startTime", Instant.ofEpochMilli(runtime.getStartTime()).toString());
        result.put("uptime", Duration.ofMillis(runtime.getUptime()).toString());
        result.put("jvmName", runtime.getVmName());
        result.put("jvmVersion", runtime.getVmVersion());
        result.put("javaVersion", System.getProperty("java.version"));
        result.put("osName", System.getProperty("os.name"));
        result.put("osArch", System.getProperty("os.arch"));
        result.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        result.put("freeMemory", Runtime.getRuntime().freeMemory());
        result.put("totalMemory", Runtime.getRuntime().totalMemory());
        result.put("maxMemory", Runtime.getRuntime().maxMemory());

        return ApiResponse.success(result);
    }

    /**
     * 获取 API 请求量统计
     * 返回总请求数和按路径统计的调用次数
     */
    @GetMapping("/api-stats")
    public ApiResponse<Map<String, Object>> apiStats(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        userService.checkAdmin(userId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalRequests", requestLoggingInterceptor.getTotalRequests());
        result.put("pathCounts", requestLoggingInterceptor.getPathCounts());

        return ApiResponse.success(result);
    }
}
