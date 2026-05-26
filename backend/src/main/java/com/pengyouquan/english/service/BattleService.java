package com.pengyouquan.english.service;

import com.pengyouquan.english.dto.BattleHistoryDTO;
import com.pengyouquan.english.dto.BattleResultRequest;
import com.pengyouquan.english.dto.RankInfoDTO;
import com.pengyouquan.english.model.*;
import com.pengyouquan.english.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BattleService {

    private final BattleHistoryRepository battleHistoryRepository;
    private final UserRepository userRepository;
    private final DeckRepository deckRepository;
    private final CardRepository cardRepository;
    private final PracticeLogRepository practiceLogRepository;
    private final TrophyService trophyService;
    private final UserStatsRepository userStatsRepository;
    private final TrophyTierRepository trophyTierRepository;
    private final ChestService chestService;

    private static final int TROPHY_GAIN = 30;
    private static final int TROPHY_LOSS = 25;

    public BattleService(BattleHistoryRepository battleHistoryRepository,
                         UserRepository userRepository,
                         DeckRepository deckRepository,
                         CardRepository cardRepository,
                         PracticeLogRepository practiceLogRepository,
                         TrophyService trophyService,
                         UserStatsRepository userStatsRepository,
                         TrophyTierRepository trophyTierRepository,
                         ChestService chestService) {
        this.battleHistoryRepository = battleHistoryRepository;
        this.userRepository = userRepository;
        this.deckRepository = deckRepository;
        this.cardRepository = cardRepository;
        this.practiceLogRepository = practiceLogRepository;
        this.trophyService = trophyService;
        this.userStatsRepository = userStatsRepository;
        this.trophyTierRepository = trophyTierRepository;
        this.chestService = chestService;
    }

    /** 发起挑战 */
    @Transactional
    public BattleHistoryDTO challengePlayer(Long challengerId, Long defenderId, Long deckId) {
        if (challengerId.equals(defenderId)) {
            throw new IllegalStateException("不能挑战自己");
        }

        userRepository.findById(defenderId)
                .orElseThrow(() -> new IllegalStateException("对手不存在"));

        deckRepository.findById(deckId)
                .orElseThrow(() -> new IllegalStateException("卡组不存在"));

        // 检查是否有未完成的挑战
        List<BattleHistory> pending = battleHistoryRepository
                .findByDefenderIdAndStatusOrderByCreatedAtDesc(defenderId, "pending");
        boolean alreadyChallenged = pending.stream()
                .anyMatch(b -> b.getChallengerId().equals(challengerId));
        if (alreadyChallenged) {
            throw new IllegalStateException("已向该玩家发起过挑战，请等待对方回应");
        }

        BattleHistory battle = new BattleHistory();
        battle.setChallengerId(challengerId);
        battle.setDefenderId(defenderId);
        battle.setChallengerDeckId(deckId);
        battle.setStatus("pending");
        battle = battleHistoryRepository.save(battle);

        User challenger = userRepository.findById(challengerId).orElse(null);
        User defender = userRepository.findById(defenderId).orElse(null);

        return toDTO(battle, challenger, defender);
    }

    /** 获取待处理的挑战 */
    public List<BattleHistoryDTO> getPendingBattles(Long defenderId) {
        List<BattleHistory> battles = battleHistoryRepository
                .findByDefenderIdAndStatusOrderByCreatedAtDesc(defenderId, "pending");
        return battles.stream().map(b -> {
            User challenger = userRepository.findById(b.getChallengerId()).orElse(null);
            User defender = userRepository.findById(b.getDefenderId()).orElse(null);
            return toDTO(b, challenger, defender);
        }).collect(Collectors.toList());
    }

    /** 接受挑战并执行AI对战 */
    @Transactional
    public BattleHistoryDTO acceptBattle(Long battleId, Long defenderId, BattleResultRequest request) {
        BattleHistory battle = battleHistoryRepository.findByIdAndStatus(battleId, "pending")
                .orElseThrow(() -> new IllegalStateException("挑战不存在或已处理"));

        if (!battle.getDefenderId().equals(defenderId)) {
            throw new IllegalStateException("无权操作此挑战");
        }

        // 1. 设置防守方数据
        battle.setDefenderDeckId(request.getDeckId());
        battle.setChallengerScore(request.getScore());
        battle.setChallengerAccuracy(request.getAccuracy());
        battle.setChallengerAvgDifficulty(request.getAvgDifficulty());

        // 2. AI 模拟防守方出牌
        AISimulationResult aiResult = simulateDefender(battle.getDefenderId(), request.getDeckId());
        battle.setDefenderScore(aiResult.score);
        battle.setDefenderAccuracy(BigDecimal.valueOf(aiResult.accuracy));
        battle.setDefenderAvgDifficulty(BigDecimal.valueOf(aiResult.avgDifficulty));

        // 3. 判定胜负
        int challengerScore = request.getScore();
        int defenderScore = aiResult.score;

        Long winnerId;
        if (challengerScore > defenderScore) {
            winnerId = battle.getChallengerId();
        } else if (defenderScore > challengerScore) {
            winnerId = battle.getDefenderId();
        } else {
            winnerId = null; // 平局
        }
        battle.setWinnerId(winnerId);

        // 4. 更新奖杯
        int trophyChange;
        if (winnerId == null) {
            trophyChange = 0; // 平局不变化
        } else if (winnerId.equals(battle.getChallengerId())) {
            trophyChange = TROPHY_GAIN;
            trophyService.updateTrophies(battle.getChallengerId(), TROPHY_GAIN);
            trophyService.updateTrophies(battle.getDefenderId(), -TROPHY_LOSS);
        } else {
            trophyChange = -TROPHY_LOSS;
            trophyService.updateTrophies(battle.getChallengerId(), -TROPHY_LOSS);
            trophyService.updateTrophies(battle.getDefenderId(), TROPHY_GAIN);
        }
        battle.setTrophyChange(Math.abs(trophyChange));

        // 5. 完成对战
        battle.setStatus("completed");
        battle.setCompletedAt(LocalDateTime.now());
        battle = battleHistoryRepository.save(battle);

        // 6. 宝箱发放：防守方（当前用户）获胜发放宝箱
        if (winnerId != null && winnerId.equals(battle.getDefenderId())) {
            chestService.grantBattleChest(battle.getDefenderId(), "pvp_battle");
        }

        User challenger = userRepository.findById(battle.getChallengerId()).orElse(null);
        User defender = userRepository.findById(battle.getDefenderId()).orElse(null);
        return toDTO(battle, challenger, defender);
    }

    /** AI 模拟防守方出牌 */
    public AISimulationResult simulateDefender(Long defenderId, Long deckId) {
        // 获取防守方近30天平均正确率
        Double rawAccuracy = practiceLogRepository.findAvgAccuracyLast30Days(defenderId);
        double accuracy = rawAccuracy != null ? rawAccuracy : 0.5; // 无数据默认50%

        // 获取卡组
        Deck deck = deckRepository.findById(deckId).orElse(null);
        if (deck == null) {
            return new AISimulationResult(0, 0.5, 0, 0);
        }

        // 解析卡组中的卡牌ID
        List<Long> cardIdList = parseCardIds(deck.getCardIds());
        if (cardIdList.isEmpty()) {
            return new AISimulationResult(0, accuracy, 0, 0);
        }

        List<Card> cards = cardRepository.findAllById(cardIdList);

        // 按费用排序（优先低费）
        cards.sort(Comparator.comparingInt(Card::getCost));

        // 取最多6张
        List<Card> playedCards = cards.subList(0, Math.min(6, cards.size()));

        Random random = new Random();
        int totalDamage = 0;
        int correctCount = 0;
        double totalDifficulty = 0;

        for (Card card : playedCards) {
            // 根据准确率判定是否答对
            boolean correct = random.nextDouble() < accuracy;
            if (correct) {
                correctCount++;
                // 费用越高伤害越高，费用1-10映射到伤害1-5
                int damage = Math.min(5, Math.max(1, card.getCost() / 2 + 1));
                totalDamage += damage;
            }
            // 难度按卡牌费用估算
            totalDifficulty += Math.min(5, Math.max(1, card.getCost() / 2 + 1));
        }

        double avgDifficulty = playedCards.isEmpty() ? 0 : totalDifficulty / playedCards.size();
        double actualAccuracy = playedCards.isEmpty() ? 0 : (double) correctCount / playedCards.size();

        return new AISimulationResult(totalDamage, actualAccuracy, avgDifficulty, playedCards.size());
    }

    /** 获取战报 */
    public List<BattleHistoryDTO> getBattleHistory(Long userId) {
        List<BattleHistory> battles = battleHistoryRepository.findCompletedByUserId(userId);
        return battles.stream().map(b -> {
            User challenger = userRepository.findById(b.getChallengerId()).orElse(null);
            User defender = userRepository.findById(b.getDefenderId()).orElse(null);
            return toDTO(b, challenger, defender);
        }).collect(Collectors.toList());
    }

    /** 获取段位信息 */
    public RankInfoDTO getRankInfo(Long userId) {
        UserStats stats = trophyService.getOrCreateUserStats(userId);
        int trophies = stats.getTrophies();
        TrophyTier tier = trophyService.getTier(trophies);

        // 计算排名（比当前用户奖杯数高的人数+1）
        List<UserStats> allStats = userStatsRepository.findAllByOrderByTrophiesDesc();
        int rank = 1;
        for (UserStats s : allStats) {
            if (s.getTrophies() > trophies) rank++;
        }

        return new RankInfoDTO(
                trophies,
                tier != null ? tier.getNameCn() : "未排名",
                tier != null ? tier.getIcon() : "❓",
                rank,
                tier != null ? tier.getSeasonRewardType() : "",
                tier != null ? tier.getSeasonRewardCount() : 0,
                stats.getWinStreak(),
                stats.getWins(),
                stats.getLosses()
        );
    }

    /** 排行榜 */
    public List<RankInfoDTO> getLeaderboard() {
        List<UserStats> allStats = userStatsRepository.findAllByOrderByTrophiesDesc();
        List<RankInfoDTO> result = new ArrayList<>();
        // 取前100
        int limit = Math.min(100, allStats.size());
        for (int i = 0; i < limit; i++) {
            UserStats stats = allStats.get(i);
            int trophies = stats.getTrophies();
            TrophyTier tier = trophyService.getTier(trophies);
            result.add(new RankInfoDTO(
                    trophies,
                    tier != null ? tier.getNameCn() : "未排名",
                    tier != null ? tier.getIcon() : "❓",
                    i + 1,
                    tier != null ? tier.getSeasonRewardType() : "",
                    tier != null ? tier.getSeasonRewardCount() : 0,
                    stats.getWinStreak(),
                    stats.getWins(),
                    stats.getLosses()
            ));
        }
        return result;
    }

    // ---- 辅助方法 ----

    private List<Long> parseCardIds(String cardIdsJson) {
        if (cardIdsJson == null || cardIdsJson.isBlank()) return List.of();
        try {
            // JSON 格式 "[1,2,3,4,5]"
            String trimmed = cardIdsJson.trim();
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                String inner = trimmed.substring(1, trimmed.length() - 1);
                if (inner.isBlank()) return List.of();
                return Arrays.stream(inner.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(Long::parseLong)
                        .collect(Collectors.toList());
            }
        } catch (Exception ignored) {}
        return List.of();
    }

    private BattleHistoryDTO toDTO(BattleHistory b, User challenger, User defender) {
        return new BattleHistoryDTO(
                b.getId(),
                b.getChallengerId(),
                challenger != null ? challenger.getNickname() : "已注销",
                b.getDefenderId(),
                defender != null ? defender.getNickname() : "已注销",
                b.getWinnerId(),
                b.getChallengerScore(),
                b.getDefenderScore(),
                b.getChallengerAccuracy(),
                b.getDefenderAccuracy(),
                b.getTrophyChange(),
                b.getStatus(),
                b.getCreatedAt(),
                b.getCompletedAt()
        );
    }

    /** AI 模拟结果 */
    public static class AISimulationResult {
        public final int score;
        public final double accuracy;
        public final double avgDifficulty;
        public final int cardsPlayed;

        public AISimulationResult(int score, double accuracy, double avgDifficulty, int cardsPlayed) {
            this.score = score;
            this.accuracy = accuracy;
            this.avgDifficulty = avgDifficulty;
            this.cardsPlayed = cardsPlayed;
        }
    }
}
