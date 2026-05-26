package com.pengyouquan.english.service;

import com.pengyouquan.english.model.*;
import com.pengyouquan.english.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 公会部落战服务
 * 周循环：周一~周三备战 → 周四~周六战斗 → 周日结算
 */
@Service
public class GuildWarService {

    private static final Logger log = LoggerFactory.getLogger(GuildWarService.class);

    private final GuildWarRepository guildWarRepository;
    private final GuildWarContributionRepository contributionRepository;
    private final GuildRepository guildRepository;
    private final GuildMemberRepository guildMemberRepository;
    private final UserRepository userRepository;
    private final UserStatsRepository userStatsRepository;
    private final AchievementService achievementService;

    public GuildWarService(GuildWarRepository guildWarRepository,
                           GuildWarContributionRepository contributionRepository,
                           GuildRepository guildRepository,
                           GuildMemberRepository guildMemberRepository,
                           UserRepository userRepository,
                           UserStatsRepository userStatsRepository,
                           AchievementService achievementService) {
        this.guildWarRepository = guildWarRepository;
        this.contributionRepository = contributionRepository;
        this.guildRepository = guildRepository;
        this.guildMemberRepository = guildMemberRepository;
        this.userRepository = userRepository;
        this.userStatsRepository = userStatsRepository;
        this.achievementService = achievementService;
    }

    // ==================== 周循环管理 ====================

    /** 获取当前周阶段 */
    public String getCurrentPhase() {
        LocalDate today = LocalDate.now();
        DayOfWeek day = today.getDayOfWeek();
        if (day == DayOfWeek.MONDAY || day == DayOfWeek.TUESDAY || day == DayOfWeek.WEDNESDAY) {
            return "preparation";
        } else if (day == DayOfWeek.THURSDAY || day == DayOfWeek.FRIDAY || day == DayOfWeek.SATURDAY) {
            return "battle";
        } else {
            return "settlement";
        }
    }

    /** 获取本周一日期 */
    public LocalDate getWeekStartDate() {
        return LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /** 获取本周日日期 */
    public LocalDate getWeekEndDate() {
        return LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
    }

    /** 获取当前周数 */
    public int getWeekNumber() {
        return LocalDate.now().get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
    }

    // ==================== 公会匹配 ====================

    /**
     * 自动匹配公会战（按公会总奖杯数）
     * 在备战阶段开始时调用
     */
    @Transactional
    public Map<String, Object> autoMatchGuilds() {
        int weekNumber = getWeekNumber();
        List<Guild> allGuilds = guildRepository.findAll();

        // 过滤掉已有本周战争的公会
        List<Guild> availableGuilds = allGuilds.stream()
                .filter(g -> guildWarRepository.findByGuildIdAndWeekNumber(g.getId(), weekNumber).isEmpty())
                .filter(g -> g.getMemberCount() >= 3) // 至少3人才能参战
                .collect(Collectors.toList());

        // 按总奖杯数排序（降序）
        availableGuilds.sort((a, b) -> {
            int aTrophies = getGuildTotalTrophies(a.getId());
            int bTrophies = getGuildTotalTrophies(b.getId());
            return Integer.compare(bTrophies, aTrophies);
        });

        LocalDate weekStart = getWeekStartDate();
        LocalDate weekEnd = getWeekEndDate();

        int matched = 0;
        for (int i = 0; i < availableGuilds.size() - 1; i += 2) {
            Guild g1 = availableGuilds.get(i);
            Guild g2 = availableGuilds.get(i + 1);

            int t1 = getGuildTotalTrophies(g1.getId());
            int t2 = getGuildTotalTrophies(g2.getId());

            // 创建战争记录（双向）
            createWar(g1.getId(), g2.getId(), weekNumber, weekStart, weekEnd, t1, t2);
            matched++;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("matched", matched);
        result.put("availableGuilds", availableGuilds.size());
        return result;
    }

    private void createWar(Long guildId, Long opponentId, int weekNumber,
                           LocalDate startDate, LocalDate endDate, int t1, int t2) {
        GuildWar war = new GuildWar();
        war.setGuildId(guildId);
        war.setOpponentGuildId(opponentId);
        war.setWeekNumber(weekNumber);
        war.setPhase("preparation");
        war.setStartDate(startDate);
        war.setEndDate(endDate);
        war.setGuildTrophies(t1);
        war.setOpponentTrophies(t2);
        guildWarRepository.save(war);
    }

    private int getGuildTotalTrophies(Long guildId) {
        List<GuildMember> members = guildMemberRepository.findByGuildId(guildId);
        return members.stream()
                .mapToInt(m -> {
                    UserStats stats = userStatsRepository.findById(m.getUserId()).orElse(null);
                    return stats != null ? stats.getTrophies() : 0;
                })
                .sum();
    }

    // ==================== 备战阶段 ====================

    /**
     * 贡献卡牌（备战阶段）
     */
    @Transactional
    public Map<String, Object> contributeCards(Long userId, int cardCount) {
        GuildMember member = guildMemberRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("你未加入公会"));

        GuildWar war = guildWarRepository.findByGuildIdAndPhase(member.getGuildId(), "preparation")
                .stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("当前不在备战阶段"));

        if (cardCount <= 0) {
            throw new IllegalStateException("贡献卡牌数量必须大于0");
        }

        GuildWarContribution contribution = contributionRepository
                .findByWarIdAndUserId(war.getId(), userId)
                .orElseGet(() -> {
                    GuildWarContribution c = new GuildWarContribution();
                    c.setWarId(war.getId());
                    c.setUserId(userId);
                    return c;
                });

        contribution.setCardsContributed(contribution.getCardsContributed() + cardCount);
        contributionRepository.save(contribution);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("cardsContributed", contribution.getCardsContributed());
        return result;
    }

    // ==================== 战斗阶段 ====================

    /**
     * 记录战斗结果
     */
    @Transactional
    public Map<String, Object> recordBattleResult(Long userId, boolean won) {
        GuildMember member = guildMemberRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("你未加入公会"));

        GuildWar war = guildWarRepository.findByGuildIdAndPhase(member.getGuildId(), "battle")
                .stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("当前不在战斗阶段"));

        GuildWarContribution contribution = contributionRepository
                .findByWarIdAndUserId(war.getId(), userId)
                .orElseGet(() -> {
                    GuildWarContribution c = new GuildWarContribution();
                    c.setWarId(war.getId());
                    c.setUserId(userId);
                    return c;
                });

        if (contribution.getBattlesFought() >= 3) {
            throw new IllegalStateException("每人每轮最多参与3场战斗");
        }

        contribution.setBattlesFought(contribution.getBattlesFought() + 1);
        if (won) {
            contribution.setBattlesWon(contribution.getBattlesWon() + 1);
        }
        contributionRepository.save(contribution);

        // 更新公会胜场
        if (won) {
            war.setGuildWins(war.getGuildWins() + 1);
        } else {
            war.setOpponentWins(war.getOpponentWins() + 1);
        }
        guildWarRepository.save(war);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("battlesLeft", 3 - contribution.getBattlesFought());
        return result;
    }

    // ==================== 结算阶段 ====================

    /**
     * 周日活动结算
     */
    @Transactional
    public Map<String, Object> settleWars() {
        List<GuildWar> wars = guildWarRepository.findByPhase("battle");
        int settled = 0;

        for (GuildWar war : wars) {
            Long winnerId;
            if (war.getGuildWins() > war.getOpponentWins()) {
                winnerId = war.getGuildId();
            } else if (war.getOpponentWins() > war.getGuildWins()) {
                winnerId = war.getOpponentGuildId();
            } else {
                winnerId = null; // 平局
            }

            war.setWinnerId(winnerId);
            war.setPhase("ended");
            guildWarRepository.save(war);

            if (winnerId != null) {
                // 胜利公会全员得星尘+公会经验
                rewardWinners(winnerId);
            }

            settled++;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("settled", settled);
        return result;
    }

    private void rewardWinners(Long guildId) {
        Guild guild = guildRepository.findById(guildId).orElse(null);
        if (guild == null) return;

        List<GuildMember> members = guildMemberRepository.findByGuildId(guildId);
        for (GuildMember member : members) {
            User user = userRepository.findById(member.getUserId()).orElse(null);
            if (user != null) {
                // 星尘奖励
                user.setStardust(user.getStardust() + 100);
                userRepository.save(user);
                // 成就检查
                achievementService.checkByConditionType(member.getUserId(), "guild_war_win", 1);
            }
        }

        // 公会经验
        guild.setRankPoints(guild.getRankPoints() + 50);
        guildRepository.save(guild);
    }

    // ==================== 查询接口 ====================

    public Map<String, Object> getGuildWarStatus(Long userId) {
        GuildMember member = guildMemberRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("你未加入公会"));

        String phase = getCurrentPhase();
        GuildWar activeWar = guildWarRepository.findByGuildIdAndPhase(member.getGuildId(), phase)
                .stream().findFirst().orElse(null);

        Map<String, Object> result = new HashMap<>();
        result.put("phase", phase);
        result.put("inWar", activeWar != null);
        result.put("weekNumber", getWeekNumber());
        result.put("weekStart", getWeekStartDate().toString());
        result.put("weekEnd", getWeekEndDate().toString());

        if (activeWar != null) {
            Guild opponent = guildRepository.findById(activeWar.getOpponentGuildId()).orElse(null);
            result.put("warId", activeWar.getId());
            result.put("opponentName", opponent != null ? opponent.getName() : "未知");
            result.put("guildWins", activeWar.getGuildWins());
            result.put("opponentWins", activeWar.getOpponentWins());
            result.put("guildTrophies", activeWar.getGuildTrophies());
            result.put("opponentTrophies", activeWar.getOpponentTrophies());

            // 我的贡献
            GuildWarContribution myContrib = contributionRepository
                    .findByWarIdAndUserId(activeWar.getId(), userId).orElse(null);
            result.put("myCardsContributed", myContrib != null ? myContrib.getCardsContributed() : 0);
            result.put("myBattlesFought", myContrib != null ? myContrib.getBattlesFought() : 0);
            result.put("myBattlesWon", myContrib != null ? myContrib.getBattlesWon() : 0);
            result.put("maxBattles", 3);
        }

        return result;
    }
}
