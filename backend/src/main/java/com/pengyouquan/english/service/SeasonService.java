package com.pengyouquan.english.service;

import com.pengyouquan.english.model.*;
import com.pengyouquan.english.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class SeasonService {

    private final SeasonConfigRepository seasonConfigRepository;
    private final SeasonRewardRepository seasonRewardRepository;
    private final UserStatsRepository userStatsRepository;
    private final UserRepository userRepository;
    private final TrophyService trophyService;
    private final SeasonRankingRepository seasonRankingRepository;
    private final GuildRepository guildRepository;
    private final GuildMemberRepository guildMemberRepository;

    private static final Map<String, String> RANK_DOWNGRADE = new LinkedHashMap<>();
    private static final Map<String, Map<String, Object>> SEASON_REWARDS = new LinkedHashMap<>();

    static {
        // 段位降级规则
        RANK_DOWNGRADE.put("legend", "diamond");
        RANK_DOWNGRADE.put("diamond", "platinum");
        RANK_DOWNGRADE.put("platinum", "gold");
        RANK_DOWNGRADE.put("gold", "silver");
        RANK_DOWNGRADE.put("silver", "bronze");

        // 赛季宝箱奖励
        SEASON_REWARDS.put("legend", Map.of(
                "chestType", "legendary",
                "legendaryCards", 1, "randomCards", 5, "stardust", 300,
                "label", "传说"
        ));
        SEASON_REWARDS.put("diamond", Map.of(
                "chestType", "epic",
                "epicCards", 1, "randomCards", 4, "stardust", 200,
                "label", "钻石"
        ));
        SEASON_REWARDS.put("platinum", Map.of(
                "chestType", "epic",
                "epicCards", 1, "randomCards", 3, "stardust", 150,
                "label", "白金"
        ));
        SEASON_REWARDS.put("gold", Map.of(
                "chestType", "rare",
                "rareCards", 1, "randomCards", 3, "stardust", 100,
                "label", "黄金"
        ));
        SEASON_REWARDS.put("silver", Map.of(
                "chestType", "rare",
                "rareCards", 1, "randomCards", 2, "stardust", 50,
                "label", "白银"
        ));
        SEASON_REWARDS.put("bronze", Map.of(
                "chestType", "common",
                "commonCards", 3, "randomCards", 0, "stardust", 20,
                "label", "青铜"
        ));
    }

    public SeasonService(SeasonConfigRepository seasonConfigRepository,
                         SeasonRewardRepository seasonRewardRepository,
                         UserStatsRepository userStatsRepository,
                         UserRepository userRepository,
                         TrophyService trophyService,
                         SeasonRankingRepository seasonRankingRepository,
                         GuildRepository guildRepository,
                         GuildMemberRepository guildMemberRepository) {
        this.seasonConfigRepository = seasonConfigRepository;
        this.seasonRewardRepository = seasonRewardRepository;
        this.userStatsRepository = userStatsRepository;
        this.userRepository = userRepository;
        this.trophyService = trophyService;
        this.seasonRankingRepository = seasonRankingRepository;
        this.guildRepository = guildRepository;
        this.guildMemberRepository = guildMemberRepository;
    }

    /**
     * 获取当前赛季信息
     */
    public Map<String, Object> getCurrentSeason() {
        Optional<SeasonConfig> activeOpt = seasonConfigRepository.findByIsActiveTrue();
        if (activeOpt.isEmpty()) {
            return Map.of("active", false);
        }

        SeasonConfig config = activeOpt.get();
        LocalDateTime now = LocalDateTime.now();
        long daysLeft = ChronoUnit.DAYS.between(now, config.getEndDate());
        long totalDays = ChronoUnit.DAYS.between(config.getStartDate(), config.getEndDate());
        long elapsedDays = ChronoUnit.DAYS.between(config.getStartDate(), now);
        double progress = totalDays > 0 ? Math.min(1.0, (double) elapsedDays / totalDays) : 0;

        Map<String, Object> result = new HashMap<>();
        result.put("active", true);
        result.put("seasonNumber", config.getSeasonNumber());
        result.put("titleCn", config.getTitleCn());
        result.put("startDate", config.getStartDate().toString());
        result.put("endDate", config.getEndDate().toString());
        result.put("daysLeft", Math.max(0, daysLeft));
        result.put("progress", progress);

        // 赛季奖励预览
        List<Map<String, Object>> rewardsPreview = new ArrayList<>();
        for (Map.Entry<String, Map<String, Object>> entry : SEASON_REWARDS.entrySet()) {
            Map<String, Object> tierReward = new HashMap<>(entry.getValue());
            tierReward.put("tier", entry.getKey());
            rewardsPreview.add(tierReward);
        }
        result.put("rewards", rewardsPreview);

        return result;
    }

    /**
     * 获取用户的赛季历史
     */
    public List<Map<String, Object>> getSeasonHistory(Long userId) {
        List<SeasonReward> rewards = seasonRewardRepository.findByUserId(userId);
        List<Map<String, Object>> history = new ArrayList<>();
        for (SeasonReward sr : rewards) {
            Map<String, Object> item = new HashMap<>();
            item.put("seasonNumber", sr.getSeasonNumber());
            item.put("finalRank", sr.getFinalRank());
            item.put("finalTrophies", sr.getFinalTrophies());
            item.put("rewardClaimed", sr.getRewardClaimed());
            item.put("chestType", sr.getChestType());
            item.put("createdAt", sr.getCreatedAt());
            history.add(item);
        }
        return history;
    }

    /**
     * 手动赛季结算
     */
    @Transactional
    public Map<String, Object> settleSeason(Long userId) {
        Optional<SeasonConfig> activeOpt = seasonConfigRepository.findByIsActiveTrue();
        if (activeOpt.isEmpty()) {
            throw new IllegalStateException("没有激活的赛季");
        }

        SeasonConfig config = activeOpt.get();
        UserStats stats = trophyService.getOrCreateUserStats(userId);
        int trophies = stats.getTrophies();
        String tierName = getTierName(trophies);
        String chestType = getChestType(tierName);

        // 保存赛季奖励记录
        Optional<SeasonReward> existing = seasonRewardRepository.findByUserIdAndSeasonNumber(
                userId, config.getSeasonNumber());
        if (existing.isPresent()) {
            return Map.of("success", true, "message", "已结算过本赛季");
        }

        SeasonReward reward = new SeasonReward();
        reward.setUserId(userId);
        reward.setSeasonNumber(config.getSeasonNumber());
        reward.setFinalRank(tierName);
        reward.setFinalTrophies(trophies);
        reward.setChestType(chestType);
        reward.setRewardClaimed(false);
        seasonRewardRepository.save(reward);

        // 段位降级
        String downgradedRank = RANK_DOWNGRADE.getOrDefault(tierName, "bronze");
        int downgradedTrophies = getTierMinTrophies(downgradedRank);

        stats.setTrophies(downgradedTrophies);
        userStatsRepository.save(stats);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("seasonNumber", config.getSeasonNumber());
        result.put("finalRank", tierName);
        result.put("finalTrophies", trophies);
        result.put("newRank", downgradedRank);
        result.put("newTrophies", downgradedTrophies);
        result.put("chestType", chestType);
        result.put("reward", SEASON_REWARDS.getOrDefault(tierName, SEASON_REWARDS.get("bronze")));

        return result;
    }

    /**
     * 领取赛季宝箱奖励
     */
    @Transactional
    public Map<String, Object> claimSeasonReward(Long userId) {
        Optional<SeasonConfig> activeOpt = seasonConfigRepository.findByIsActiveTrue();
        if (activeOpt.isEmpty()) {
            throw new IllegalStateException("没有激活的赛季");
        }

        SeasonConfig config = activeOpt.get();
        SeasonReward reward = seasonRewardRepository
                .findByUserIdAndSeasonNumber(userId, config.getSeasonNumber())
                .orElseThrow(() -> new IllegalStateException("没有可领取的赛季奖励"));

        if (reward.getRewardClaimed()) {
            throw new IllegalStateException("奖励已领取");
        }

        // 发放奖励
        Map<String, Object> rewardData = SEASON_REWARDS.getOrDefault(
                reward.getFinalRank(), SEASON_REWARDS.get("bronze"));

        // 发放星尘
        int stardustAmount = (int) rewardData.getOrDefault("stardust", 0);
        if (stardustAmount > 0) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalStateException("用户不存在"));
            user.setStardust(user.getStardust() + stardustAmount);
            userRepository.save(user);
        }

        reward.setRewardClaimed(true);
        reward.setClaimedAt(LocalDateTime.now());
        seasonRewardRepository.save(reward);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("finalRank", reward.getFinalRank());
        result.put("chestType", reward.getChestType());
        result.put("stardustGained", stardustAmount);
        result.put("rewardData", rewardData);

        return result;
    }

    /**
     * 全服段位降级（管理员调用）
     */
    @Transactional
    public Map<String, Object> resetAllRanks() {
        List<UserStats> allStats = userStatsRepository.findAll();
        int count = 0;
        for (UserStats stats : allStats) {
            int trophies = stats.getTrophies();
            String tierName = getTierName(trophies);
            if (RANK_DOWNGRADE.containsKey(tierName)) {
                String downgraded = RANK_DOWNGRADE.get(tierName);
                stats.setTrophies(getTierMinTrophies(downgraded));
                userStatsRepository.save(stats);
                count++;
            } else if ("bronze".equals(tierName)) {
                stats.setTrophies(0);
                userStatsRepository.save(stats);
                count++;
            }
        }

        // 切换赛季
        Optional<SeasonConfig> activeOpt = seasonConfigRepository.findByIsActiveTrue();
        activeOpt.ifPresent(config -> {
            config.setIsActive(false);
            seasonConfigRepository.save(config);
        });

        Optional<SeasonConfig> nextOpt = seasonConfigRepository.findBySeasonNumber(
                activeOpt.map(c -> c.getSeasonNumber() + 1).orElse(2));
        nextOpt.ifPresent(config -> {
            config.setIsActive(true);
            seasonConfigRepository.save(config);
        });

        return Map.of("success", true, "affectedUsers", count);
    }

    /**
     * 获取用户当前赛季的奖励状态
     */
    public Map<String, Object> getUserSeasonRewards(Long userId) {
        Optional<SeasonConfig> activeOpt = seasonConfigRepository.findByIsActiveTrue();
        if (activeOpt.isEmpty()) {
            return Map.of("seasonActive", false);
        }

        Map<String, Object> result = new HashMap<>();
        SeasonConfig config = activeOpt.get();

        Optional<SeasonReward> reward = seasonRewardRepository
                .findByUserIdAndSeasonNumber(userId, config.getSeasonNumber());

        result.put("seasonActive", true);
        result.put("seasonNumber", config.getSeasonNumber());

        if (reward.isPresent()) {
            SeasonReward sr = reward.get();
            result.put("settled", true);
            result.put("finalRank", sr.getFinalRank());
            result.put("finalTrophies", sr.getFinalTrophies());
            result.put("chestType", sr.getChestType());
            result.put("rewardClaimed", sr.getRewardClaimed());
            result.put("rewardData", SEASON_REWARDS.getOrDefault(sr.getFinalRank(), SEASON_REWARDS.get("bronze")));
        } else {
            result.put("settled", false);
        }

        return result;
    }

    /**
     * 奖励预览
     */
    public List<Map<String, Object>> getRewardsPreview() {
        List<Map<String, Object>> preview = new ArrayList<>();
        for (Map.Entry<String, Map<String, Object>> entry : SEASON_REWARDS.entrySet()) {
            Map<String, Object> tierReward = new HashMap<>(entry.getValue());
            tierReward.put("tier", entry.getKey());
            preview.add(tierReward);
        }
        return preview;
    }

    private String getTierName(int trophies) {
        if (trophies >= 1000) return "legend";
        if (trophies >= 700) return "diamond";
        if (trophies >= 400) return "platinum";
        if (trophies >= 200) return "gold";
        if (trophies >= 50) return "silver";
        return "bronze";
    }

    private String getChestType(String tierName) {
        return switch (tierName) {
            case "legend" -> "legendary";
            case "diamond", "platinum" -> "epic";
            case "gold", "silver" -> "rare";
            default -> "common";
        };
    }

    private int getTierMinTrophies(String tierName) {
        return switch (tierName) {
            case "legend" -> 1000;
            case "diamond" -> 700;
            case "platinum" -> 400;
            case "gold" -> 200;
            case "silver" -> 50;
            default -> 0;
        };
    }

    // ========== 三模式全服排行 ==========

    /**
     * 计算全服综合排名（综合评分 = PVP奖杯数×0.5 + 远征最高分×0.3 + 公会贡献×0.2）
     */
    @Transactional
    public Map<String, Object> calculateSeasonRankings(int seasonNumber) {
        List<User> allUsers = userRepository.findAll();
        List<SeasonRanking> rankings = new ArrayList<>();

        for (User user : allUsers) {
            UserStats stats = trophyService.getOrCreateUserStats(user.getId());
            int pvpScore = stats.getTrophies();

            // 远征最高分：简化处理，用奖杯数/2 作为远征评分
            int expeditionScore = stats.getTrophies() / 2;

            // 公会贡献
            int guildScore = 0;
            var guildMemberOpt = guildMemberRepository.findByUserId(user.getId());
            if (guildMemberOpt.isPresent()) {
                guildScore = guildMemberOpt.get().getWeeklyScore();
            }

            int totalScore = (int)(pvpScore * 0.5 + expeditionScore * 0.3 + guildScore * 0.2);

            SeasonRanking sr = new SeasonRanking();
            sr.setUserId(user.getId());
            sr.setSeasonNumber(seasonNumber);
            sr.setPvpScore(pvpScore);
            sr.setExpeditionScore(expeditionScore);
            sr.setGuildScore(guildScore);
            sr.setTotalScore(totalScore);
            rankings.add(sr);
        }

        // 排序并保存
        rankings.sort((a, b) -> Integer.compare(b.getTotalScore(), a.getTotalScore()));
        seasonRankingRepository.deleteAll(
                seasonRankingRepository.findBySeasonNumberOrderByTotalScoreDesc(seasonNumber));
        seasonRankingRepository.saveAll(rankings);

        return Map.of("success", true, "totalRanked", rankings.size());
    }

    /**
     * 发放赛季称号和奖励
     */
    @Transactional
    public Map<String, Object> awardSeasonTitles(int seasonNumber) {
        List<SeasonRanking> rankings = seasonRankingRepository
                .findBySeasonNumberOrderByTotalScoreDesc(seasonNumber);

        if (rankings.isEmpty()) {
            return Map.of("success", false, "message", "赛季排名为空");
        }

        for (int i = 0; i < rankings.size(); i++) {
            SeasonRanking sr = rankings.get(i);
            sr.setRankPosition(i + 1);

            if (i == 0) {
                sr.setTitle("月之王者");
            } else if (i <= 2) {
                sr.setTitle("月之大师");
            } else if (i <= 9) {
                sr.setTitle("月之勇士");
            } else {
                sr.setTitle("");
            }

            User user = userRepository.findById(sr.getUserId()).orElse(null);
            if (user != null) {
                if (i == 0) {
                    user.setStardust(user.getStardust() + 1000);
                } else if (i <= 2) {
                    user.setStardust(user.getStardust() + 500);
                } else if (i <= 9) {
                    user.setStardust(user.getStardust() + 300);
                } else {
                    user.setStardust(user.getStardust() + 50);
                }
                userRepository.save(user);
            }

            seasonRankingRepository.save(sr);
        }

        return Map.of("success", true, "awarded", rankings.size());
    }

    /**
     * 获取用户当前排名和综合分
     */
    public Map<String, Object> getCurrentRanking(Long userId) {
        int seasonNumber = getCurrentSeasonNumber();
        Optional<SeasonRanking> srOpt = seasonRankingRepository
                .findByUserIdAndSeasonNumber(userId, seasonNumber);

        if (srOpt.isEmpty()) {
            return Map.of("hasRanking", false, "seasonNumber", seasonNumber);
        }

        SeasonRanking sr = srOpt.get();
        User user = userRepository.findById(userId).orElse(null);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("hasRanking", true);
        result.put("seasonNumber", sr.getSeasonNumber());
        result.put("pvpScore", sr.getPvpScore());
        result.put("expeditionScore", sr.getExpeditionScore());
        result.put("guildScore", sr.getGuildScore());
        result.put("totalScore", sr.getTotalScore());
        result.put("rankPosition", sr.getRankPosition());
        result.put("title", sr.getTitle() != null && !sr.getTitle().isEmpty() ? sr.getTitle() : "");
        result.put("nickname", user != null ? user.getNickname() : "");
        return result;
    }

    /**
     * TOP100排行
     */
    public List<Map<String, Object>> getTop100() {
        int seasonNumber = getCurrentSeasonNumber();
        List<SeasonRanking> rankings = seasonRankingRepository
                .findBySeasonNumberOrderByTotalScoreDesc(seasonNumber);

        List<Map<String, Object>> result = new ArrayList<>();
        int limit = Math.min(100, rankings.size());

        for (int i = 0; i < limit; i++) {
            SeasonRanking sr = rankings.get(i);
            User user = userRepository.findById(sr.getUserId()).orElse(null);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("rank", i + 1);
            item.put("userId", sr.getUserId());
            item.put("nickname", user != null ? user.getNickname() : "未知");
            item.put("totalScore", sr.getTotalScore());
            item.put("pvpScore", sr.getPvpScore());
            item.put("expeditionScore", sr.getExpeditionScore());
            item.put("guildScore", sr.getGuildScore());
            item.put("title", sr.getTitle() != null && !sr.getTitle().isEmpty() ? sr.getTitle() : "");
            result.add(item);
        }

        return result;
    }

    private int getCurrentSeasonNumber() {
        var activeOpt = seasonConfigRepository.findByIsActiveTrue();
        return activeOpt.map(SeasonConfig::getSeasonNumber).orElse(1);
    }
}
