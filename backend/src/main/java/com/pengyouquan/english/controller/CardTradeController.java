package com.pengyouquan.english.controller;

import com.pengyouquan.english.dto.ApiResponse;
import com.pengyouquan.english.security.CurrentUserId;
import com.pengyouquan.english.service.CardTradeService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 公会换卡控制器
 * 公会成员之间互相交换卡牌
 */
@RestController
@RequestMapping("/api/cards/trade")
public class CardTradeController {

    private final CardTradeService cardTradeService;

    public CardTradeController(CardTradeService cardTradeService) {
        this.cardTradeService = cardTradeService;
    }

    /** 发送换卡请求 */
    @PostMapping("/request")
    public ApiResponse<Map<String, Object>> sendTradeRequest(
            @CurrentUserId Long userId,
            @RequestBody Map<String, Object> body) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        try {
            Long receiverId = ((Number) body.get("receiverId")).longValue();
            Long requestedCardId = ((Number) body.get("requestedCardId")).longValue();
            Long offeredCardId = body.get("offeredCardId") != null
                    ? ((Number) body.get("offeredCardId")).longValue() : null;
            return ApiResponse.success(cardTradeService.sendTradeRequest(userId, receiverId, requestedCardId, offeredCardId));
        } catch (IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /** 接受换卡请求 */
    @PostMapping("/{id}/accept")
    public ApiResponse<Map<String, Object>> acceptTradeRequest(
            @CurrentUserId Long userId,
            @PathVariable Long id) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        try {
            return ApiResponse.success(cardTradeService.acceptTradeRequest(id, userId));
        } catch (IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /** 拒绝换卡请求 */
    @PostMapping("/{id}/reject")
    public ApiResponse<Map<String, Object>> rejectTradeRequest(
            @CurrentUserId Long userId,
            @PathVariable Long id) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        try {
            return ApiResponse.success(cardTradeService.rejectTradeRequest(id, userId));
        } catch (IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /** 获取收到的换卡请求 */
    @GetMapping("/received")
    public ApiResponse<List<Map<String, Object>>> getReceivedRequests(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        return ApiResponse.success(cardTradeService.getReceivedRequests(userId));
    }

    /** 获取发送的换卡请求 */
    @GetMapping("/sent")
    public ApiResponse<List<Map<String, Object>>> getSentRequests(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        return ApiResponse.success(cardTradeService.getSentRequests(userId));
    }

    /** 获取交易历史 */
    @GetMapping("/history")
    public ApiResponse<List<Map<String, Object>>> getTradeHistory(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        return ApiResponse.success(cardTradeService.getTradeHistory(userId));
    }

    /** 获取每日换卡限额 */
    @GetMapping("/daily-limit")
    public ApiResponse<Map<String, Object>> getDailyLimit(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        return ApiResponse.success(cardTradeService.getDailyLimit(userId));
    }
}
