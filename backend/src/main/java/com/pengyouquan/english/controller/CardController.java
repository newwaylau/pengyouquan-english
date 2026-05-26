package com.pengyouquan.english.controller;

import com.pengyouquan.english.dto.*;
import com.pengyouquan.english.security.CurrentUserId;
import com.pengyouquan.english.service.CardService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    /**
     * 查询某剧集所有可收集卡牌
     */
    @GetMapping("/cards")
    public ApiResponse<List<CardResponse>> getCards(
            @RequestParam(required = false) Long showId) {
        return ApiResponse.success(cardService.getCards(showId));
    }

    /**
     * 我的牌库
     */
    @GetMapping("/cards/my")
    public ApiResponse<List<CardResponse>> getMyCards(
            @CurrentUserId Long userId,
            @RequestParam(required = false) Long showId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        return ApiResponse.success(cardService.getCollection(userId, showId));
    }

    /**
     * 发放卡牌包（听写完成后调用）
     */
    @PostMapping("/cards/grant-pack")
    public ApiResponse<CardPackResult> grantPack(
            @CurrentUserId Long userId,
            @RequestBody GrantPackRequest request) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        try {
            CardPackResult result = cardService.grantCardPack(userId, request.getShowId(), request.getAccuracy());
            return ApiResponse.success(result);
        } catch (IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /**
     * 保存卡组
     */
    @PostMapping("/decks")
    public ApiResponse<DeckDTO> saveDeck(
            @CurrentUserId Long userId,
            @RequestBody SaveDeckRequest request) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        try {
            DeckDTO result = cardService.saveDeck(userId, request.getName(), request.getCardIds());
            return ApiResponse.success(result);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /**
     * 获取所有卡组
     */
    @GetMapping("/decks")
    public ApiResponse<List<DeckDTO>> getDecks(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        return ApiResponse.success(cardService.getDeckList(userId));
    }
}
