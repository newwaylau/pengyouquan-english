package com.pengyouquan.english.service;

import com.pengyouquan.english.dto.CardResponse;
import com.pengyouquan.english.model.Card;
import com.pengyouquan.english.model.UserCard;
import com.pengyouquan.english.model.UserChest;
import com.pengyouquan.english.model.UserStats;
import com.pengyouquan.english.repository.CardRepository;
import com.pengyouquan.english.repository.UserCardRepository;
import com.pengyouquan.english.repository.UserChestRepository;
import com.pengyouquan.english.repository.UserStatsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChestService {

    private final UserChestRepository userChestRepository;
    private final UserStatsRepository userStatsRepository;
    private final CardRepository cardRepository;
    private final UserCardRepository userCardRepository;

    private static final int MAX_CHESTS_PER_TYPE = 3;
    private static final int MAX_TOTAL_CHESTS = 4;

    public ChestService(UserChestRepository userChestRepository,
                        UserStatsRepository userStatsRepository,
                        CardRepository cardRepository,
                        UserCardRepository userCardRepository) {
        this.userChestRepository = userChestRepository;
        this.userStatsRepository = userStatsRepository;
        this.cardRepository = cardRepository;
        this.userCardRepository = userCardRepository;
    }

    /**
     * 对战后发放宝箱（在胜利时调用）
     */
    @Transactional
    public List<UserChest> grantBattleChest(Long userId, String source) {
        UserStats stats = userStatsRepository.findById(userId).orElse(null);
        if (stats == null) return List.of();

        List<UserChest> granted = new ArrayList<>();

        // 青铜：赢≥1场即可
        if (canAddChest(userId, "bronze")) {
            UserChest chest = createChest(userId, "bronze", 10, source);
            granted.add(chest);
        }

        // 白银：赢≥2场
        if (stats.getWins() >= 2 && canAddChest(userId, "silver")) {
            UserChest chest = createChest(userId, "silver", 25, source);
            granted.add(chest);
        }

        // 黄金：3连胜以上
        if (stats.getWinStreak() >= 3 && canAddChest(userId, "gold")) {
            UserChest chest = createChest(userId, "gold", 50, source);
            granted.add(chest);
        }

        return granted;
    }

    /**
     * 练习听写后推进解锁进度
     */
    @Transactional
    public List<UserChest> progressChest(Long userId, int sentencesDone) {
        List<UserChest> unlocking = userChestRepository
                .findByUserIdAndStatusOrderByCreatedAtAsc(userId, "unlocking");
        List<UserChest> updated = new ArrayList<>();
        for (UserChest chest : unlocking) {
            chest.setUnlockProgress(chest.getUnlockProgress() + sentencesDone);
            if (chest.getUnlockProgress() >= chest.getUnlockRequired()) {
                chest.setUnlockProgress(chest.getUnlockRequired());
                chest.setStatus("ready");
            }
            userChestRepository.save(chest);
            updated.add(chest);
        }
        return updated;
    }

    /**
     * 领取宝箱奖励
     */
    @Transactional
    public ChestClaimResult claimChest(Long chestId, Long userId) {
        UserChest chest = userChestRepository.findById(chestId)
                .orElseThrow(() -> new IllegalStateException("宝箱不存在"));
        if (!chest.getUserId().equals(userId)) {
            throw new IllegalStateException("无权领取此宝箱");
        }
        if (!"ready".equals(chest.getStatus())) {
            throw new IllegalStateException("宝箱未就绪");
        }

        // 发放卡牌奖励
        List<Card> rewardCards = generateReward(chest.getChestType());
        List<CardResponse> responses = new ArrayList<>();
        for (Card card : rewardCards) {
            Optional<UserCard> existing = userCardRepository.findByUserIdAndCardId(userId, card.getId());
            if (existing.isPresent()) {
                UserCard uc = existing.get();
                uc.setQuantity(uc.getQuantity() + 1);
                userCardRepository.save(uc);
                responses.add(CardResponse.fromCard(card, uc.getQuantity(), ""));
            } else {
                UserCard uc = new UserCard();
                uc.setUserId(userId);
                uc.setCardId(card.getId());
                uc.setQuantity(1);
                userCardRepository.save(uc);
                responses.add(CardResponse.fromCard(card, 1, ""));
            }
        }

        chest.setStatus("claimed");
        userChestRepository.save(chest);

        return new ChestClaimResult(chest, responses);
    }

    /**
     * 获取用户所有宝箱
     */
    public List<UserChest> getChests(Long userId) {
        return userChestRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * 检查能否添加同类型宝箱
     */
    private boolean canAddChest(Long userId, String chestType) {
        int sameTypeCount = userChestRepository
                .countByUserIdAndStatusAndChestType(userId, "locked", chestType)
                + userChestRepository
                .countByUserIdAndStatusAndChestType(userId, "unlocking", chestType)
                + userChestRepository
                .countByUserIdAndStatusAndChestType(userId, "ready", chestType);
        if (sameTypeCount >= MAX_CHESTS_PER_TYPE) return false;

        // 检查总槽位（locked + unlocking + ready 总数）
        List<UserChest> all = userChestRepository.findByUserIdOrderByCreatedAtDesc(userId);
        long activeCount = all.stream()
                .filter(c -> !"claimed".equals(c.getStatus()))
                .count();
        return activeCount < MAX_TOTAL_CHESTS;
    }

    private UserChest createChest(Long userId, String chestType, int unlockRequired, String source) {
        UserChest chest = new UserChest();
        chest.setUserId(userId);
        chest.setChestType(chestType);
        chest.setStatus("locked");
        chest.setUnlockProgress(0);
        chest.setUnlockRequired(unlockRequired);
        chest.setSource(source);
        return userChestRepository.save(chest);
    }

    /**
     * 根据宝箱类型生成奖励卡牌
     */
    private List<Card> generateReward(String chestType) {
        Random rand = new Random();
        List<Card> result = new ArrayList<>();
        List<Card> allCards = cardRepository.findAll();
        if (allCards.isEmpty()) return result;

        switch (chestType) {
            case "bronze":
                // 1稀有 + 2普通
                result.addAll(pickRandomByRarity(allCards, "rare", 1, rand));
                result.addAll(pickRandomByRarity(allCards, "common", 2, rand));
                break;
            case "silver":
                // 1史诗 + 3混合
                result.addAll(pickRandomByRarity(allCards, "epic", 1, rand));
                result.addAll(pickRandom(allCards, 3, rand));
                break;
            case "gold":
                // 1传说(30%) + 5随机
                if (rand.nextDouble() < 0.3) {
                    result.addAll(pickRandomByRarity(allCards, "legendary", 1, rand));
                }
                if (result.isEmpty()) {
                    // 没出传说，补一张史诗
                    result.addAll(pickRandomByRarity(allCards, "epic", 1, rand));
                }
                result.addAll(pickRandom(allCards, 5, rand));
                break;
        }
        return result;
    }

    private List<Card> pickRandomByRarity(List<Card> pool, String rarity, int count, Random rand) {
        List<Card> filtered = pool.stream()
                .filter(c -> rarity.equals(c.getRarity()))
                .collect(Collectors.toList());
        if (filtered.isEmpty()) return List.of();
        List<Card> result = new ArrayList<>();
        for (int i = 0; i < count && !filtered.isEmpty(); i++) {
            result.add(filtered.get(rand.nextInt(filtered.size())));
        }
        return result;
    }

    private List<Card> pickRandom(List<Card> pool, int count, Random rand) {
        List<Card> result = new ArrayList<>();
        for (int i = 0; i < count && !pool.isEmpty(); i++) {
            result.add(pool.get(rand.nextInt(pool.size())));
        }
        return result;
    }

    /**
     * 领取结果
     */
    public static class ChestClaimResult {
        public final UserChest chest;
        public final List<CardResponse> cards;

        public ChestClaimResult(UserChest chest, List<CardResponse> cards) {
            this.chest = chest;
            this.cards = cards;
        }
    }
}
