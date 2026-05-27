package com.pengyouquan.english.controller;

import com.pengyouquan.english.model.Card;
import com.pengyouquan.english.model.UserCard;
import com.pengyouquan.english.repository.BattleHistoryRepository;
import com.pengyouquan.english.repository.CardRepository;
import com.pengyouquan.english.repository.UserCardRepository;
import com.pengyouquan.english.service.GoldService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
public class GoldController {

    private final GoldService goldService;
    private final BattleHistoryRepository battleHistoryRepository;
    private final CardRepository cardRepository;
    private final UserCardRepository userCardRepository;

    public GoldController(GoldService goldService,
                          BattleHistoryRepository battleHistoryRepository,
                          CardRepository cardRepository,
                          UserCardRepository userCardRepository) {
        this.goldService = goldService;
        this.battleHistoryRepository = battleHistoryRepository;
        this.cardRepository = cardRepository;
        this.userCardRepository = userCardRepository;
    }

    private Long getUserId(Authentication auth) {
        if (auth == null || auth.getName() == null) return null;
        try { return Long.parseLong(auth.getName()); } catch (NumberFormatException e) { return null; }
    }

    /** 查询金币余额 */
    @GetMapping("/api/gold/balance")
    public ResponseEntity<?> getBalance(Authentication auth) {
        Long userId = getUserId(auth);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("error", "未认证"));

        int balance = goldService.getBalance(userId);

        // 检查今日是否有首胜
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        List<?> todayWins = battleHistoryRepository.findWinsSince(userId, todayStart);
        boolean dailyWinClaimed = !todayWins.isEmpty();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "balance", balance,
                "dailyWinClaimed", dailyWinClaimed
        ));
    }

    /** 练习获得金币（每50句触发） */
    @PostMapping("/api/gold/earn")
    public ResponseEntity<?> earnGold(Authentication auth, @RequestBody Map<String, Object> body) {
        Long userId = getUserId(auth);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("error", "未认证"));

        int sentences = body.containsKey("sentences") ? ((Number) body.get("sentences")).intValue() : 0;
        int goldEarned = 0;

        // 每50句获得10金币
        if (sentences >= 50) {
            int batches = sentences / 50;
            goldEarned = batches * 10;
        }

        if (goldEarned > 0) {
            int newBalance = goldService.addGold(userId, goldEarned);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "goldEarned", goldEarned,
                    "newBalance", newBalance
            ));
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "goldEarned", 0,
                "message", "继续练习，每50句可获得10金币"
        ));
    }

    /** 每日首胜领取金币 */
    @PostMapping("/api/gold/daily-win")
    public ResponseEntity<?> claimDailyWin(Authentication auth) {
        Long userId = getUserId(auth);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("error", "未认证"));

        // 检查今日是否已有首胜记录
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        List<?> todayWins = battleHistoryRepository.findWinsSince(userId, todayStart);

        if (todayWins.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "今日尚无胜利记录"
            ));
        }

        int newBalance = goldService.addDailyWinBonus(userId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "goldEarned", 20,
                "newBalance", newBalance,
                "message", "每日首胜+20金币"
        ));
    }

    /** 购买卡包（100金币 → 1卡包 = 5张随机卡） */
    @PostMapping("/api/shop/buy-pack")
    public ResponseEntity<?> buyPack(Authentication auth) {
        Long userId = getUserId(auth);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("error", "未认证"));

        final int PACK_COST = 100;
        boolean deducted = goldService.deductGold(userId, PACK_COST);
        if (!deducted) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "金币不足，需要100金币"
            ));
        }

        // 开包：5张随机卡
        List<Card> allCards = cardRepository.findAll();
        if (allCards.isEmpty()) {
            return ResponseEntity.ok(Map.of("success", true, "cards", new ArrayList<>()));
        }

        Random rand = new Random();
        List<Map<String, Object>> openedCards = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            Card picked = pickRarityWeighted(allCards, rand);
            // 给用户添加该卡牌
            Optional<UserCard> existing = userCardRepository.findByUserIdAndCardId(userId, picked.getId());
            if (existing.isPresent()) {
                UserCard uc = existing.get();
                uc.setQuantity(uc.getQuantity() + 1);
                userCardRepository.save(uc);
            } else {
                UserCard uc = new UserCard();
                uc.setUserId(userId);
                uc.setCardId(picked.getId());
                uc.setQuantity(1);
                userCardRepository.save(uc);
            }

            Map<String, Object> cardInfo = new HashMap<>();
            cardInfo.put("id", picked.getId());
            cardInfo.put("nameCn", picked.getNameCn());
            cardInfo.put("nameEn", picked.getNameEn());
            cardInfo.put("rarity", picked.getRarity());
            cardInfo.put("cardType", picked.getCardType());
            cardInfo.put("cost", picked.getCost());
            cardInfo.put("attack", picked.getAttack());
            cardInfo.put("health", picked.getHealth());
            openedCards.add(cardInfo);
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "cards", openedCards,
                "newBalance", goldService.getBalance(userId)
        ));
    }

    // ====== 工具方法 ======

    /**
     * 按稀有度加权随机选卡
     * common 60%, rare 25%, epic 12%, legendary 3%
     */
    private Card pickRarityWeighted(List<Card> pool, Random rand) {
        double roll = rand.nextDouble();
        String targetRarity;
        if (roll < 0.60) targetRarity = "common";
        else if (roll < 0.85) targetRarity = "rare";
        else if (roll < 0.97) targetRarity = "epic";
        else targetRarity = "legendary";

        List<Card> filtered = pool.stream()
                .filter(c -> targetRarity.equals(c.getRarity()))
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            // 回退到全卡池
            return pool.get(rand.nextInt(pool.size()));
        }
        return filtered.get(rand.nextInt(filtered.size()));
    }
}
