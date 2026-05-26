package com.pengyouquan.english.controller;

import com.pengyouquan.english.dto.ApiResponse;
import com.pengyouquan.english.security.CurrentUserId;
import com.pengyouquan.english.service.GuildWarService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 公会部落战控制器
 * 周循环：周一~周三备战 → 周四~周六战斗 → 周日结算
 */
@RestController
@RequestMapping("/api/guilds/war")
public class GuildWarController {

    private final GuildWarService guildWarService;

    public GuildWarController(GuildWarService guildWarService) {
        this.guildWarService = guildWarService;
    }

    /** 获取当前部落战状态 */
    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> getWarStatus(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        try {
            return ApiResponse.success(guildWarService.getGuildWarStatus(userId));
        } catch (IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /** 贡献卡牌（备战阶段） */
    @PostMapping("/contribute")
    public ApiResponse<Map<String, Object>> contributeCards(
            @CurrentUserId Long userId,
            @RequestBody Map<String, Object> body) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        try {
            int cardCount = ((Number) body.getOrDefault("cardCount", 0)).intValue();
            return ApiResponse.success(guildWarService.contributeCards(userId, cardCount));
        } catch (IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /** 记录战斗结果（战斗阶段） */
    @PostMapping("/battle-result")
    public ApiResponse<Map<String, Object>> recordBattleResult(
            @CurrentUserId Long userId,
            @RequestBody Map<String, Object> body) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        try {
            boolean won = (boolean) body.getOrDefault("won", false);
            return ApiResponse.success(guildWarService.recordBattleResult(userId, won));
        } catch (IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /** 自动匹配公会战（管理员调用） */
    @PostMapping("/auto-match")
    public ApiResponse<Map<String, Object>> autoMatch(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        return ApiResponse.success(guildWarService.autoMatchGuilds());
    }

    /** 结算战争（管理员调用） */
    @PostMapping("/settle")
    public ApiResponse<Map<String, Object>> settleWars(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        return ApiResponse.success(guildWarService.settleWars());
    }
}
