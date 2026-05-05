package com.pengyouquan.english.controller;

import com.pengyouquan.english.config.RequestLoggingInterceptor;
import com.pengyouquan.english.dto.ApiResponse;
import com.pengyouquan.english.model.SystemSetting;
import com.pengyouquan.english.repository.PracticeLogRepository;
import com.pengyouquan.english.repository.SentenceRepository;
import com.pengyouquan.english.repository.ShowRepository;
import com.pengyouquan.english.repository.SystemSettingRepository;
import com.pengyouquan.english.repository.UserRepository;
import com.pengyouquan.english.security.CurrentUserId;
import com.pengyouquan.english.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private final SystemSettingRepository systemSettingRepository;
    private final RequestLoggingInterceptor requestLoggingInterceptor;

    public AdminController(UserService userService,
                           PracticeLogRepository practiceLogRepository,
                           UserRepository userRepository,
                           ShowRepository showRepository,
                           SentenceRepository sentenceRepository,
                           SystemSettingRepository systemSettingRepository,
                           RequestLoggingInterceptor requestLoggingInterceptor) {
        this.userService = userService;
        this.practiceLogRepository = practiceLogRepository;
        this.userRepository = userRepository;
        this.showRepository = showRepository;
        this.sentenceRepository = sentenceRepository;
        this.systemSettingRepository = systemSettingRepository;
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
     * 返回总请求数和按路径统计的调用次数及最后调用时间
     */
    @GetMapping("/api-stats")
    public ApiResponse<Map<String, Object>> apiStats(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        userService.checkAdmin(userId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalRequests", requestLoggingInterceptor.getTotalRequests());
        result.put("pathCounts", requestLoggingInterceptor.getPathCounts());
        result.put("lastCalledAt", requestLoggingInterceptor.getLastCalledAt());

        return ApiResponse.success(result);
    }

    /**
     * 获取错误日志
     * 读取 /tmp/openclaw/openclaw-*.log 下的日志文件，返回最近 N 行
     * @param lines 返回行数（默认 100）
     */
    @GetMapping("/logs")
    public ApiResponse<Map<String, Object>> getLogs(@CurrentUserId Long userId,
                                                    @RequestParam(defaultValue = "100") int lines) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        userService.checkAdmin(userId);

        Map<String, Object> result = new LinkedHashMap<>();
        List<String> logLines = new ArrayList<>();
        String logFile = "";

        // 尝试从多个日志目录读取
        List<String> logDirs = List.of(
            "/tmp/openclaw",
            "backend/logs"
        );

        for (String dir : logDirs) {
            File dirFile = new File(dir);
            if (!dirFile.isDirectory()) continue;

            // 查找符合条件的日志文件（按修改时间排序取最新）
            File[] files = dirFile.listFiles((d, name) -> name.startsWith("openclaw") && name.endsWith(".log"));
            if (files == null || files.length == 0) continue;

            Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
            logFile = files[0].getAbsolutePath();

            try {
                List<String> allLines = Files.readAllLines(files[0].toPath());
                int start = Math.max(0, allLines.size() - lines);
                logLines = allLines.subList(start, allLines.size());
            } catch (IOException e) {
                logLines.add("[读取日志失败] " + e.getMessage());
            }
            break;
        }

        result.put("file", logFile);
        result.put("totalLines", logLines.size());
        result.put("lines", logLines);

        return ApiResponse.success(result);
    }

    /**
     * 导出全部用户数据（CSV）
     * 管理员专用
     */
    @GetMapping("/export/users")
    public void exportUsers(@CurrentUserId Long userId, HttpServletResponse response) throws IOException {
        if (userId == null) {
            response.setStatus(401);
            return;
        }
        userService.checkAdmin(userId);

        String filename = "pengyouquan-users-" + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + ".csv";
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=" + filename);

        // CSV 表头
        response.getWriter().write("ID,邮箱,昵称,角色,启用,注册时间\n");

        // 查询所有用户并写入 CSV
        var users = userRepository.findAll();
        for (var u : users) {
            String line = String.format("%d,%s,%s,%s,%s,%s\n",
                u.getId(),
                escapeCsv(u.getEmail()),
                escapeCsv(u.getNickname()),
                u.getRole(),
                u.getEnabled() != null && u.getEnabled() ? "是" : "否",
                u.getCreatedAt() != null ? u.getCreatedAt().toString() : ""
            );
            response.getWriter().write(line);
        }

        response.getWriter().flush();
    }

    /**
     * 用户留存分析
     * 返回近30天每日活跃用户数
     */
    @GetMapping("/retention")
    public ApiResponse<List<Map<String, Object>>> retention(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        userService.checkAdmin(userId);

        LocalDateTime since30 = LocalDate.now().minusDays(30).atStartOfDay();
        List<Object[]> data = practiceLogRepository.dailyActiveUsers(since30);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : data) {
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("day", row[0].toString());
            d.put("activeUsers", ((Number) row[1]).longValue());
            result.add(d);
        }
        return ApiResponse.success(result);
    }

    /**
     * 获取所有系统设置
     */
    @GetMapping("/settings")
    public ApiResponse<List<SystemSetting>> getSettings(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        userService.checkAdmin(userId);

        return ApiResponse.success(systemSettingRepository.findAll());
    }

    /**
     * 更新系统设置
     */
    @PutMapping("/settings")
    public ApiResponse<Void> updateSettings(@CurrentUserId Long userId,
                                             @RequestBody Map<String, String> settings) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        userService.checkAdmin(userId);

        for (Map.Entry<String, String> entry : settings.entrySet()) {
            SystemSetting setting = systemSettingRepository.findById(entry.getKey())
                    .orElse(new SystemSetting(entry.getKey(), entry.getValue()));
            setting.setSettingValue(entry.getValue());
            setting.setUpdatedAt(LocalDateTime.now());
            systemSettingRepository.save(setting);
        }
        return ApiResponse.success(null);
    }

    /**
     * 公开获取公告（无需登录）
     */
    @GetMapping("/public/announcement")
    public ApiResponse<Map<String, String>> getAnnouncement() {
        SystemSetting announcement = systemSettingRepository.findById("announcement").orElse(null);
        Map<String, String> result = new LinkedHashMap<>();
        result.put("announcement", announcement != null ? announcement.getSettingValue() : "");
        return ApiResponse.success(result);
    }

    /**
     * 用户列表（分页+角色筛选）
     * 管理员专用
     * @param page 页码（从0开始）
     * @param size 每页条数（默认20）
     * @param role 角色筛选（可选，如 "admin" 或 "user"）
     */
    @GetMapping("/users")
    public ApiResponse<Map<String, Object>> listUsers(
            @CurrentUserId Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String role) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        userService.checkAdmin(userId);

        size = Math.min(size, 100);
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));

        Page<com.pengyouquan.english.model.User> userPage;
        if (role != null && !role.isBlank()) {
            userPage = userRepository.findByRole(role, pageRequest);
        } else {
            userPage = userRepository.findAll(pageRequest);
        }

        List<com.pengyouquan.english.dto.UserInfoResponse> userList = userPage.getContent().stream()
                .map(com.pengyouquan.english.dto.UserInfoResponse::fromUser)
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("users", userList);
        result.put("total", userPage.getTotalElements());
        result.put("page", page);
        result.put("totalPages", userPage.getTotalPages());

        return ApiResponse.success(result);
    }

    /** CSV 转义：如果包含逗号/引号/换行则包裹引号 */
    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
