package com.pengyouquan.english.controller;

import com.pengyouquan.english.dto.ApiResponse;
import com.pengyouquan.english.security.CurrentUserId;
import com.pengyouquan.english.service.ExpeditionCombatService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/expedition/battle")
public class ExpeditionCombatController {

    private final ExpeditionCombatService combatService;

    public ExpeditionCombatController(ExpeditionCombatService combatService) {
        this.combatService = combatService;
    }

    @PostMapping("/start")
    public ApiResponse<Map<String, Object>> startBattle(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        try {
            return ApiResponse.success(combatService.startBattle(userId));
        } catch (IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/play-card")
    public ApiResponse<Map<String, Object>> playCard(
            @CurrentUserId Long userId,
            @RequestBody Map<String, Object> body) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        try {
            int cardUid = ((Number) body.get("cardUid")).intValue();
            Integer targetIndex = body.get("targetIndex") != null
                    ? ((Number) body.get("targetIndex")).intValue() : null;
            return ApiResponse.success(combatService.playCard(userId, cardUid, targetIndex));
        } catch (IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.badRequest("参数错误：" + e.getMessage());
        }
    }

    @PostMapping("/end-turn")
    public ApiResponse<Map<String, Object>> endTurn(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        try {
            return ApiResponse.success(combatService.endTurn(userId));
        } catch (IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/use-potion")
    public ApiResponse<Map<String, Object>> usePotion(
            @CurrentUserId Long userId,
            @RequestBody Map<String, Object> body) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        try {
            Long potionId = ((Number) body.get("potionId")).longValue();
            return ApiResponse.success(combatService.usePotion(userId, potionId));
        } catch (IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.badRequest("参数错误：" + e.getMessage());
        }
    }

    @GetMapping("/state")
    public ApiResponse<Map<String, Object>> getBattleState(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        try {
            return ApiResponse.success(combatService.getBattleStateData(userId));
        } catch (IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }
}
