package com.pengyouquan.english.controller;

import com.pengyouquan.english.dto.ApiResponse;
import com.pengyouquan.english.security.CurrentUserId;
import com.pengyouquan.english.service.ExpeditionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/expedition")
public class ExpeditionController {

    private final ExpeditionService expeditionService;

    public ExpeditionController(ExpeditionService expeditionService) {
        this.expeditionService = expeditionService;
    }

    @PostMapping("/start")
    public ApiResponse<Map<String, Object>> startExpedition(
            @CurrentUserId Long userId,
            @RequestBody Map<String, Object> body) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        try {
            Long showId = ((Number) body.get("showId")).longValue();
            List<Long> deckCardIds = ((List<Number>) body.get("deckCardIds"))
                    .stream().map(Number::longValue).toList();
            return ApiResponse.success(expeditionService.startExpedition(userId, showId, deckCardIds));
        } catch (IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.badRequest("参数错误：" + e.getMessage());
        }
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> getExpedition(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        return ApiResponse.success(expeditionService.getExpedition(userId));
    }

    @PostMapping("/enter-combat")
    public ApiResponse<Map<String, Object>> enterCombat(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        try {
            return ApiResponse.success(expeditionService.enterCombat(userId));
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
            Long cardId = ((Number) body.get("cardId")).longValue();
            return ApiResponse.success(expeditionService.playCardInCombat(userId, cardId));
        } catch (IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.badRequest("参数错误：" + e.getMessage());
        }
    }

    @PostMapping("/answer")
    public ApiResponse<Map<String, Object>> answerQuestion(
            @CurrentUserId Long userId,
            @RequestBody Map<String, Object> body) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        try {
            Long sentenceId = ((Number) body.get("sentenceId")).longValue();
            String answer = (String) body.getOrDefault("answer", "");
            boolean correct = Boolean.TRUE.equals(body.get("correct"));
            return ApiResponse.success(expeditionService.answerQuestion(userId, sentenceId, answer, correct));
        } catch (IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.badRequest("参数错误：" + e.getMessage());
        }
    }

    @PostMapping("/reward")
    public ApiResponse<Map<String, Object>> chooseReward(
            @CurrentUserId Long userId,
            @RequestBody Map<String, Object> body) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        try {
            int choiceIndex = ((Number) body.get("choiceIndex")).intValue();
            return ApiResponse.success(expeditionService.chooseReward(userId, choiceIndex));
        } catch (IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/reward-choices")
    public ApiResponse<Map<String, Object>> getRewardChoices(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        return ApiResponse.success(expeditionService.getRewardChoices(userId));
    }

    @PostMapping("/apply-reward")
    public ApiResponse<Map<String, Object>> applyReward(
            @CurrentUserId Long userId,
            @RequestBody Map<String, Object> body) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        try {
            return ApiResponse.success(expeditionService.applyReward(userId, body));
        } catch (IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/event")
    public ApiResponse<Map<String, Object>> enterEvent(
            @CurrentUserId Long userId,
            @RequestBody Map<String, Object> body) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        try {
            int choiceIndex = ((Number) body.get("choiceIndex")).intValue();
            return ApiResponse.success(expeditionService.enterEvent(userId, choiceIndex));
        } catch (IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/rest")
    public ApiResponse<Map<String, Object>> enterRest(
            @CurrentUserId Long userId,
            @RequestBody Map<String, Object> body) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        try {
            String action = (String) body.getOrDefault("action", "heal");
            Long cardId = body.get("cardId") != null ? ((Number) body.get("cardId")).longValue() : null;
            return ApiResponse.success(expeditionService.enterRest(userId, action, cardId));
        } catch (IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/shop")
    public ApiResponse<Map<String, Object>> enterShop(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        return ApiResponse.success(expeditionService.enterShop(userId));
    }

    @PostMapping("/buy")
    public ApiResponse<Map<String, Object>> buyItem(
            @CurrentUserId Long userId,
            @RequestBody Map<String, Object> body) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        try {
            String type = (String) body.get("type");
            Long itemId = ((Number) body.get("itemId")).longValue();
            return ApiResponse.success(expeditionService.buyItem(userId, type, itemId));
        } catch (IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/next-node")
    public ApiResponse<Map<String, Object>> nextNode(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        try {
            return ApiResponse.success(expeditionService.nextNode(userId));
        } catch (IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/choose-path")
    public ApiResponse<Map<String, Object>> choosePath(
            @CurrentUserId Long userId,
            @RequestBody Map<String, Object> body) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        try {
            int choiceIndex = ((Number) body.get("choiceIndex")).intValue();
            return ApiResponse.success(expeditionService.choosePath(userId, choiceIndex));
        } catch (IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/abandon")
    public ApiResponse<Map<String, Object>> abandonExpedition(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        try {
            return ApiResponse.success(expeditionService.abandonExpedition(userId));
        } catch (IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/episode-story")
    public ApiResponse<Map<String, Object>> getEpisodeStory(
            @RequestParam Long showId,
            @RequestParam(defaultValue = "1") int season,
            @RequestParam(defaultValue = "1") int episode) {
        return ApiResponse.success(expeditionService.getEpisodeStory(showId, season, episode));
    }

    @GetMapping("/sentence")
    public ApiResponse<Map<String, Object>> getRandomSentence(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        return ApiResponse.success(expeditionService.getRandomSentence(userId));
    }

    // ==================== 药水接口 ====================

    @PostMapping("/use-potion")
    public ApiResponse<Map<String, Object>> usePotion(
            @CurrentUserId Long userId,
            @RequestBody Map<String, Object> body) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        try {
            Long potionId = ((Number) body.get("potionId")).longValue();
            return ApiResponse.success(expeditionService.usePotionInCombat(userId, potionId));
        } catch (IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.badRequest("参数错误：" + e.getMessage());
        }
    }

    @GetMapping("/potions")
    public ApiResponse<List<Map<String, Object>>> getPotions(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        return ApiResponse.success(expeditionService.getPotions(userId));
    }

    @PostMapping("/use-potion-outside")
    public ApiResponse<Map<String, Object>> usePotionOutside(
            @CurrentUserId Long userId,
            @RequestBody Map<String, Object> body) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        try {
            Long potionId = ((Number) body.get("potionId")).longValue();
            return ApiResponse.success(expeditionService.usePotion(userId, potionId));
        } catch (IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.badRequest("参数错误：" + e.getMessage());
        }
    }

    @GetMapping("/boss-info")
    public ApiResponse<Map<String, Object>> getBossInfo(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        try {
            return ApiResponse.success(expeditionService.getBossInfo(userId));
        } catch (Exception e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    // ==================== 新战斗系统接口 ====================

    @GetMapping("/map")
    public ApiResponse<Map<String, Object>> getMap(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        try {
            return ApiResponse.success(expeditionService.getExpedition(userId));
        } catch (Exception e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/enter-node")
    public ApiResponse<Map<String, Object>> enterNode(
            @CurrentUserId Long userId,
            @RequestBody Map<String, Object> body) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        try {
            Long nodeId = ((Number) body.get("nodeId")).longValue();
            return ApiResponse.success(expeditionService.enterCombat(userId));
        } catch (IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.badRequest("参数错误：" + e.getMessage());
        }
    }

    @PostMapping("/smith")
    public ApiResponse<Map<String, Object>> smith(
            @CurrentUserId Long userId,
            @RequestBody Map<String, Object> body) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        try {
            Long cardId = ((Number) body.get("cardId")).longValue();
            return ApiResponse.success(expeditionService.enterRest(userId, "smith", cardId));
        } catch (IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.badRequest("参数错误：" + e.getMessage());
        }
    }

    @PostMapping("/shop-list")
    public ApiResponse<Map<String, Object>> shopList(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        try {
            return ApiResponse.success(expeditionService.enterShop(userId));
        } catch (IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/remove-card")
    public ApiResponse<Map<String, Object>> removeCard(
            @CurrentUserId Long userId,
            @RequestBody Map<String, Object> body) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        try {
            Long cardId = ((Number) body.get("cardId")).longValue();
            return ApiResponse.success(expeditionService.chooseReward(userId, 0));
        } catch (Exception e) {
            return ApiResponse.badRequest("参数错误：" + e.getMessage());
        }
    }

    @PostMapping("/event-choose")
    public ApiResponse<Map<String, Object>> eventChoose(
            @CurrentUserId Long userId,
            @RequestBody Map<String, Object> body) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        try {
            int choiceIndex = ((Number) body.get("choiceIndex")).intValue();
            return ApiResponse.success(expeditionService.enterEvent(userId, choiceIndex));
        } catch (IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.badRequest("参数错误：" + e.getMessage());
        }
    }

    @GetMapping("/state")
    public ApiResponse<Map<String, Object>> getState(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        return ApiResponse.success(expeditionService.getExpedition(userId));
    }
}
