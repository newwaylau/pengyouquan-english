package com.pengyouquan.english.controller;

import com.pengyouquan.english.dto.ApiResponse;
import com.pengyouquan.english.security.CurrentUserId;
import com.pengyouquan.english.service.SeasonService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/season")
public class SeasonController {

    private final SeasonService seasonService;

    public SeasonController(SeasonService seasonService) {
        this.seasonService = seasonService;
    }

    @GetMapping("/current")
    public ApiResponse<Map<String, Object>> getCurrentSeason() {
        return ApiResponse.success(seasonService.getCurrentSeason());
    }

    @GetMapping("/history")
    public ApiResponse<List<Map<String, Object>>> getSeasonHistory(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        return ApiResponse.success(seasonService.getSeasonHistory(userId));
    }

    @PostMapping("/settle")
    public ApiResponse<Map<String, Object>> settleSeason(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        try {
            return ApiResponse.success(seasonService.settleSeason(userId));
        } catch (IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/claim")
    public ApiResponse<Map<String, Object>> claimSeasonReward(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        try {
            return ApiResponse.success(seasonService.claimSeasonReward(userId));
        } catch (IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/rewards")
    public ApiResponse<Map<String, Object>> getSeasonRewards(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        Map<String, Object> result = Map.of(
                "seasonReward", seasonService.getUserSeasonRewards(userId),
                "rewardsPreview", seasonService.getRewardsPreview()
        );
        return ApiResponse.success(result);
    }

    @PostMapping("/admin/reset-all")
    public ApiResponse<Map<String, Object>> resetAllRanks(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        return ApiResponse.success(seasonService.resetAllRanks());
    }

    // ========== 三模式全服排行 ==========

    @GetMapping("/ranking")
    public ApiResponse<Map<String, Object>> getCurrentRanking(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        return ApiResponse.success(seasonService.getCurrentRanking(userId));
    }

    @GetMapping("/ranking/top100")
    public ApiResponse<List<Map<String, Object>>> getTop100() {
        return ApiResponse.success(seasonService.getTop100());
    }

    @PostMapping("/admin/calculate-ranking")
    public ApiResponse<Map<String, Object>> calculateRanking() {
        var active = seasonService.getCurrentSeason();
        int seasonNumber = active.containsKey("seasonNumber") ?
                ((Number) active.get("seasonNumber")).intValue() : 1;
        return ApiResponse.success(seasonService.calculateSeasonRankings(seasonNumber));
    }

    @PostMapping("/admin/award-titles")
    public ApiResponse<Map<String, Object>> awardTitles() {
        var active = seasonService.getCurrentSeason();
        int seasonNumber = active.containsKey("seasonNumber") ?
                ((Number) active.get("seasonNumber")).intValue() : 1;
        return ApiResponse.success(seasonService.awardSeasonTitles(seasonNumber));
    }
}
