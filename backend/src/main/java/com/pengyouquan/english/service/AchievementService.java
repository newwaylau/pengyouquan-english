package com.pengyouquan.english.service;

import com.pengyouquan.english.model.*;
import com.pengyouquan.english.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AchievementService {

    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final UserRepository userRepository;
    private final CardRepository cardRepository;
    private final UserCardRepository userCardRepository;
    private final ShowRepository showRepository;

    public AchievementService(AchievementRepository achievementRepository,
                              UserAchievementRepository userAchievementRepository,
                              UserRepository userRepository,
                              CardRepository cardRepository,
                              UserCardRepository userCardRepository,
                              ShowRepository showRepository) {
        this.achievementRepository = achievementRepository;
        this.userAchievementRepository = userAchievementRepository;
        this.userRepository = userRepository;
        this.cardRepository = cardRepository;
        this.userCardRepository = userCardRepository;
        this.showRepository = showRepository;
    }

    /**
     * 新用户注册时初始化所有成就记录
     */
    @Transactional
    public void initUserAchievements(Long userId) {
        List<Achievement> all = achievementRepository.findAllByOrderBySortOrderAsc();
        for (Achievement a : all) {
            UserAchievement ua = new UserAchievement();
            ua.setUserId(userId);
            ua.setAchievementId(a.getId());
            ua.setProgress(0);
            ua.setTarget(a.getConditionValue());
            ua.setUnlocked(false);
            userAchievementRepository.save(ua);
        }
    }

    /**
     * 通用成就检查入口
     */
    @Transactional
    public List<Map<String, Object>> checkAchievements(Long userId, String triggerType, int value) {
        List<Achievement> achievements = achievementRepository.findByCategoryOrderBySortOrderAsc(triggerType);
        List<Map<String, Object>> newlyUnlocked = new ArrayList<>();

        for (Achievement a : achievements) {
            if (!a.getConditionType().equals(triggerType)) continue;

            UserAchievement ua = userAchievementRepository
                    .findByUserIdAndAchievementId(userId, a.getId())
                    .orElse(null);
            if (ua == null) continue;
            if (Boolean.TRUE.equals(ua.getUnlocked())) continue;

            ua.setProgress(Math.min(ua.getTarget(), value));
            if (ua.getProgress() >= ua.getTarget()) {
                ua.setUnlocked(true);
                ua.setUnlockedAt(LocalDateTime.now());
                newlyUnlocked.add(Map.of(
                        "achievementId", a.getId(),
                        "nameCn", a.getNameCn(),
                        "rewardStardust", a.getRewardStardust()
                ));
            }
            userAchievementRepository.save(ua);
        }
        return newlyUnlocked;
    }

    /**
     * 通用成就检查（按条件类型匹配）
     */
    @Transactional
    public List<Map<String, Object>> checkByConditionType(Long userId, String conditionType, int currentValue) {
        List<Achievement> relevant = achievementRepository.findAllByOrderBySortOrderAsc().stream()
                .filter(a -> a.getConditionType().equals(conditionType))
                .collect(Collectors.toList());

        List<Map<String, Object>> newlyUnlocked = new ArrayList<>();

        for (Achievement a : relevant) {
            UserAchievement ua = userAchievementRepository
                    .findByUserIdAndAchievementId(userId, a.getId())
                    .orElse(null);
            if (ua == null) continue;
            if (Boolean.TRUE.equals(ua.getUnlocked())) continue;

            ua.setProgress(Math.min(ua.getTarget(), currentValue));
            if (ua.getProgress() >= ua.getTarget()) {
                ua.setUnlocked(true);
                ua.setUnlockedAt(LocalDateTime.now());
                newlyUnlocked.add(Map.of(
                        "achievementId", a.getId(),
                        "nameCn", a.getNameCn(),
                        "rewardStardust", a.getRewardStardust()
                ));
            }
            userAchievementRepository.save(ua);
        }
        return newlyUnlocked;
    }

    /**
     * 获取用户所有成就进度
     */
    public Map<String, Object> getUserAchievements(Long userId) {
        List<Achievement> allAchievements = achievementRepository.findAllByOrderBySortOrderAsc();
        List<UserAchievement> userAchievements = userAchievementRepository.findByUserId(userId);

        Map<Long, UserAchievement> uaMap = userAchievements.stream()
                .collect(Collectors.toMap(UserAchievement::getAchievementId, ua -> ua));

        List<Map<String, Object>> achievementList = new ArrayList<>();
        int unlockedCount = 0;

        for (Achievement a : allAchievements) {
            UserAchievement ua = uaMap.get(a.getId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", a.getId());
            item.put("category", a.getCategory());
            item.put("keyName", a.getKeyName());
            item.put("nameCn", a.getNameCn());
            item.put("descriptionCn", a.getDescriptionCn());
            item.put("icon", a.getIcon() != null && !a.getIcon().isEmpty() ? a.getIcon() : getDefaultIcon(a.getCategory()));
            item.put("rarity", a.getRarity());
            item.put("conditionType", a.getConditionType());
            item.put("conditionValue", a.getConditionValue());
            item.put("rewardStardust", a.getRewardStardust());

            if (ua != null) {
                item.put("progress", ua.getProgress());
                item.put("target", ua.getTarget());
                item.put("unlocked", ua.getUnlocked());
                item.put("unlockedAt", ua.getUnlockedAt());
                if (Boolean.TRUE.equals(ua.getUnlocked())) unlockedCount++;
            } else {
                item.put("progress", 0);
                item.put("target", a.getConditionValue());
                item.put("unlocked", false);
                item.put("unlockedAt", null);
            }
            achievementList.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("achievements", achievementList);
        result.put("totalCount", allAchievements.size());
        result.put("unlockedCount", unlockedCount);
        return result;
    }

    /**
     * 领取成就奖励
     */
    @Transactional
    public Map<String, Object> claimAchievementReward(Long userId, Long achievementId) {
        Achievement achievement = achievementRepository.findById(achievementId)
                .orElseThrow(() -> new IllegalStateException("成就不存在"));

        UserAchievement ua = userAchievementRepository
                .findByUserIdAndAchievementId(userId, achievementId)
                .orElseThrow(() -> new IllegalStateException("成就记录不存在"));

        if (!Boolean.TRUE.equals(ua.getUnlocked())) {
            throw new IllegalStateException("成就尚未解锁");
        }

        if (ua.getProgress() > ua.getTarget()) {
            // 已经领取过
            throw new IllegalStateException("该成就奖励已领取");
        }

        // 发放星尘
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("用户不存在"));
        user.setStardust(user.getStardust() + achievement.getRewardStardust());
        userRepository.save(user);

        // 标记为已领取（把progress设置为超过target）
        ua.setProgress(ua.getTarget() + 1);
        userAchievementRepository.save(ua);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("stardustGained", achievement.getRewardStardust());
        result.put("newStardust", user.getStardust());
        result.put("achievementName", achievement.getNameCn());
        return result;
    }

    /**
     * 全收集统计
     */
    public Map<String, Object> getCollectionStats(Long userId) {
        List<Card> allCards = cardRepository.findAll();
        List<UserCard> userCards = userCardRepository.findByUserId(userId);

        Map<Long, Long> showCardCounts = allCards.stream()
                .collect(Collectors.groupingBy(Card::getShowId, Collectors.counting()));

        Map<Long, Set<Long>> userShowCards = userCards.stream()
                .collect(Collectors.groupingBy(
                    uc -> {
                        Card c = cardRepository.findById(uc.getCardId()).orElse(null);
                        return c != null ? c.getShowId() : -1L;
                    },
                    Collectors.mapping(UserCard::getCardId, Collectors.toSet())
                ));

        List<Map<String, Object>> showStats = new ArrayList<>();
        for (Map.Entry<Long, Long> entry : showCardCounts.entrySet()) {
            Long showId = entry.getKey();
            long total = entry.getValue();
            long owned = userShowCards.getOrDefault(showId, Collections.emptySet()).size();
            showStats.add(Map.of(
                    "showId", showId,
                    "showName", showRepository.findById(showId).map(Show::getName).orElse(""),
                    "owned", owned,
                    "total", total,
                    "complete", owned >= total
            ));
        }

        int totalCards = allCards.size();
        int ownedCards = (int) userCards.stream()
                .map(UserCard::getCardId)
                .distinct()
                .count();

        long goldenCount = userCards.stream()
                .filter(uc -> Boolean.TRUE.equals(uc.getIsGolden()))
                .count();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalCards", totalCards);
        result.put("ownedCards", ownedCards);
        result.put("completionRate", totalCards > 0 ? (double) ownedCards / totalCards : 0);
        result.put("goldenCount", goldenCount);
        result.put("shows", showStats);
        return result;
    }

    private String getDefaultIcon(String category) {
        return switch (category) {
            case "collection" -> "🎴";
            case "battle" -> "⚔️";
            case "expedition" -> "🗡️";
            case "guild" -> "🏰";
            case "streak" -> "📅";
            default -> "🏆";
        };
    }
}
