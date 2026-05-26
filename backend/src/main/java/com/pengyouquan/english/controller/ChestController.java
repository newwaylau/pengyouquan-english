package com.pengyouquan.english.controller;

import com.pengyouquan.english.dto.ApiResponse;
import com.pengyouquan.english.dto.CardResponse;
import com.pengyouquan.english.model.UserChest;
import com.pengyouquan.english.security.CurrentUserId;
import com.pengyouquan.english.service.ChestService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chests")
public class ChestController {

    private final ChestService chestService;

    public ChestController(ChestService chestService) {
        this.chestService = chestService;
    }

    /**
     * 获取用户所有宝箱
     */
    @GetMapping
    public ApiResponse<List<UserChest>> getChests(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        return ApiResponse.success(chestService.getChests(userId));
    }

    /**
     * 领取宝箱
     */
    @PostMapping("/claim/{chestId}")
    public ApiResponse<Map<String, Object>> claimChest(
            @CurrentUserId Long userId,
            @PathVariable Long chestId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        try {
            ChestService.ChestClaimResult result = chestService.claimChest(chestId, userId);
            Map<String, Object> data = new java.util.LinkedHashMap<>();
            data.put("chest", result.chest);
            data.put("cards", result.cards);
            return ApiResponse.success(data);
        } catch (IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /**
     * 推进宝箱解锁进度（由练习完成后内部调用）
     */
    @PostMapping("/progress")
    public ApiResponse<List<UserChest>> progressChest(
            @CurrentUserId Long userId,
            @RequestBody Map<String, Object> body) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        int sentencesDone = Integer.parseInt(body.getOrDefault("sentencesDone", "0").toString());
        return ApiResponse.success(chestService.progressChest(userId, sentencesDone));
    }
}
