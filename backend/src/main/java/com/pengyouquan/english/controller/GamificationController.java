package com.pengyouquan.english.controller;

import com.pengyouquan.english.dto.*;
import com.pengyouquan.english.security.CurrentUserId;
import com.pengyouquan.english.service.GamificationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/game")
public class GamificationController {

    private final GamificationService gamificationService;

    public GamificationController(GamificationService gamificationService) {
        this.gamificationService = gamificationService;
    }

    @GetMapping("/prestige")
    public ApiResponse<PrestigeResponse> getPrestige(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        return ApiResponse.success(gamificationService.getPrestige(userId));
    }

    @GetMapping("/daily-challenge")
    public ApiResponse<DailyChallengeResponse> getDailyChallenge(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        return ApiResponse.success(gamificationService.getOrCreateDailyChallenge(userId));
    }

    @PostMapping("/daily-challenge/{id}/submit")
    public ApiResponse<?> submitAnswer(
            @CurrentUserId Long userId,
            @PathVariable Long id,
            @RequestBody SubmitAnswerRequest request) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        try {
            var result = gamificationService.submitAnswer(userId, id, request.getQuestionId(), request.getAnswer());
            return ApiResponse.success(result);
        } catch (IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/daily-challenge/{id}/complete")
    public ApiResponse<?> completeChallenge(@CurrentUserId Long userId, @PathVariable Long id) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        try {
            var result = gamificationService.completeChallenge(userId, id);
            return ApiResponse.success(result);
        } catch (IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/leaderboard")
    public ApiResponse<List<LeaderboardEntry>> getLeaderboard(
            @CurrentUserId Long userId,
            @RequestParam(defaultValue = "today") String period) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        return ApiResponse.success(gamificationService.getLeaderboard(userId, period));
    }

    @GetMapping("/history")
    public ApiResponse<List<ChallengeHistoryEntry>> getHistory(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        return ApiResponse.success(gamificationService.getChallengeHistory(userId));
    }
}
