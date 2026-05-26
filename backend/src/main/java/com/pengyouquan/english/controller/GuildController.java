package com.pengyouquan.english.controller;

import com.pengyouquan.english.dto.ApiResponse;
import com.pengyouquan.english.security.CurrentUserId;
import com.pengyouquan.english.service.GuildService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/guilds")
public class GuildController {

    private final GuildService guildService;

    public GuildController(GuildService guildService) {
        this.guildService = guildService;
    }

    @PostMapping("/create")
    public ApiResponse<Map<String, Object>> createGuild(
            @CurrentUserId Long userId,
            @RequestBody Map<String, Object> body) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        try {
            String name = (String) body.get("name");
            String description = (String) body.getOrDefault("description", "");
            return ApiResponse.success(guildService.createGuild(userId, name, description));
        } catch (IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/search")
    public ApiResponse<Map<String, Object>> searchGuilds(
            @CurrentUserId Long userId,
            @RequestParam(required = false) String q) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        return ApiResponse.success(guildService.searchGuilds(q));
    }

    @PostMapping("/{id}/join")
    public ApiResponse<Map<String, Object>> joinGuild(
            @CurrentUserId Long userId,
            @PathVariable Long id) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        try {
            return ApiResponse.success(guildService.joinGuild(userId, id));
        } catch (IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/leave")
    public ApiResponse<Map<String, Object>> leaveGuild(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        try {
            return ApiResponse.success(guildService.leaveGuild(userId));
        } catch (IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/{id}/kick/{targetId}")
    public ApiResponse<Map<String, Object>> kickMember(
            @CurrentUserId Long userId,
            @PathVariable Long id,
            @PathVariable Long targetId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        try {
            return ApiResponse.success(guildService.kickMember(userId, targetId));
        } catch (IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/{id}/transfer/{targetId}")
    public ApiResponse<Map<String, Object>> transferLeadership(
            @CurrentUserId Long userId,
            @PathVariable Long id,
            @PathVariable Long targetId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        try {
            return ApiResponse.success(guildService.transferLeadership(userId, targetId));
        } catch (IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> getGuildInfo(
            @CurrentUserId Long userId,
            @PathVariable Long id) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        try {
            return ApiResponse.success(guildService.getGuildInfo(id));
        } catch (IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/mine")
    public ApiResponse<Map<String, Object>> getMyGuild(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        return ApiResponse.success(guildService.getMyGuild(userId));
    }

    @GetMapping("/leaderboard")
    public ApiResponse<Map<String, Object>> getLeaderboard(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        return ApiResponse.success(guildService.getLeaderboard());
    }

    @PostMapping("/treasure/{treasureId}/claim")
    public ApiResponse<Map<String, Object>> claimTreasure(
            @CurrentUserId Long userId,
            @PathVariable Long treasureId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        try {
            return ApiResponse.success(guildService.claimTreasure(userId, treasureId));
        } catch (IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/settle")
    public ApiResponse<Map<String, Object>> settleTerritoryWar(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        // 仅管理员可调用
        return ApiResponse.success(guildService.settleTerritoryWar());
    }

    // ========== 公会联赛 ==========

    @GetMapping("/league")
    public ApiResponse<java.util.List<Map<String, Object>>> getLeagueStandings(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        return ApiResponse.success(guildService.getLeagueStandings());
    }

    @GetMapping("/{id}/league")
    public ApiResponse<Map<String, Object>> getGuildLeagueInfo(
            @CurrentUserId Long userId,
            @PathVariable Long id) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        return ApiResponse.success(guildService.getGuildLeagueInfo(id));
    }

    @GetMapping("/league/history")
    public ApiResponse<Map<String, Object>> getLeagueHistory(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        Map<String, Object> result = Map.of("message", "历史赛季功能即将上线");
        return ApiResponse.success(result);
    }

    @PostMapping("/admin/calculate-league")
    public ApiResponse<Map<String, Object>> calculateLeague(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        return ApiResponse.success(guildService.calculateLeagueScores());
    }
}
