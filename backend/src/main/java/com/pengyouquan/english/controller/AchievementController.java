package com.pengyouquan.english.controller;

import com.pengyouquan.english.dto.ApiResponse;
import com.pengyouquan.english.security.CurrentUserId;
import com.pengyouquan.english.service.AchievementService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/achievements")
public class AchievementController {

    private final AchievementService achievementService;

    public AchievementController(AchievementService achievementService) {
        this.achievementService = achievementService;
    }

    /**
     * 所有成就+用户进度
     */
    @GetMapping
    public ApiResponse<Map<String, Object>> getAchievements(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        return ApiResponse.success(achievementService.getUserAchievements(userId));
    }

    /**
     * 领取成就奖励
     */
    @PostMapping("/claim/{achievementId}")
    public ApiResponse<Map<String, Object>> claimReward(
            @CurrentUserId Long userId,
            @PathVariable Long achievementId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        try {
            return ApiResponse.success(achievementService.claimAchievementReward(userId, achievementId));
        } catch (IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /**
     * 全收集统计
     */
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getStats(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        return ApiResponse.success(achievementService.getCollectionStats(userId));
    }
}
