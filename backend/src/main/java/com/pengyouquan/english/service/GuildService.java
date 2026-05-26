package com.pengyouquan.english.service;

import com.pengyouquan.english.model.*;
import com.pengyouquan.english.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GuildService {

    private final GuildRepository guildRepository;
    private final GuildMemberRepository guildMemberRepository;
    private final GuildTreasureRepository guildTreasureRepository;
    private final GuildTreasureClaimRepository guildTreasureClaimRepository;
    private final UserRepository userRepository;
    private final GuildLeagueSeasonRepository guildLeagueSeasonRepository;
    private final AchievementService achievementService;

    private static final int CREATE_COST_STARDUST = 500;
    private static final List<String> PRESET_NAMES = List.of(
            "龙石岛", "君临城", "临冬城", "奔流城", "风息堡",
            "高庭", "凯岩城", "鹰巢城", "阳戟城", "派克城"
    );

    public GuildService(GuildRepository guildRepository,
                        GuildMemberRepository guildMemberRepository,
                        GuildTreasureRepository guildTreasureRepository,
                        GuildTreasureClaimRepository guildTreasureClaimRepository,
                        UserRepository userRepository,
                        GuildLeagueSeasonRepository guildLeagueSeasonRepository,
                        AchievementService achievementService) {
        this.guildRepository = guildRepository;
        this.guildMemberRepository = guildMemberRepository;
        this.guildTreasureRepository = guildTreasureRepository;
        this.guildTreasureClaimRepository = guildTreasureClaimRepository;
        this.userRepository = userRepository;
        this.guildLeagueSeasonRepository = guildLeagueSeasonRepository;
        this.achievementService = achievementService;
    }

    // ==================== 1. 创建公会 ====================

    @Transactional
    public Map<String, Object> createGuild(Long userId, String name, String description) {
        // 检查是否已有公会
        Optional<GuildMember> existing = guildMemberRepository.findByUserId(userId);
        if (existing.isPresent()) {
            throw new IllegalStateException("你已加入公会，不能创建新公会");
        }

        // 检查公会名是否已存在
        if (guildRepository.findByName(name).isPresent()) {
            throw new IllegalStateException("公会名已存在");
        }

        // 检查星尘
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("用户不存在"));
        if (user.getStardust() < CREATE_COST_STARDUST) {
            throw new IllegalStateException("星尘不足，创建公会需要 " + CREATE_COST_STARDUST + " 星尘");
        }

        // 扣星尘
        user.setStardust(user.getStardust() - CREATE_COST_STARDUST);
        userRepository.save(user);

        // 创建公会
        Guild guild = new Guild();
        guild.setName(name);
        guild.setLeaderId(userId);
        guild.setDescription(description != null ? description : "");
        guild.setMemberCount(1);
        guildRepository.save(guild);

        // 创建会长成员记录
        GuildMember leader = new GuildMember();
        leader.setGuildId(guild.getId());
        leader.setUserId(userId);
        leader.setRole("leader");
        guildMemberRepository.save(leader);

        // 创建初始宝库里程碑
        createTreasureMilestones(guild.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("guild", buildGuildData(guild, userId));
        return result;
    }

    // ==================== 2. 搜索公会 ====================

    public Map<String, Object> searchGuilds(String query) {
        List<Guild> guilds;
        if (query != null && !query.isBlank()) {
            guilds = guildRepository.findByNameContainingIgnoreCase(query);
        } else {
            guilds = guildRepository.findAll();
        }

        List<Map<String, Object>> result = guilds.stream()
                .map(g -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", g.getId());
                    item.put("name", g.getName());
                    item.put("description", g.getDescription());
                    item.put("memberCount", g.getMemberCount());
                    item.put("maxMembers", g.getMaxMembers());
                    item.put("weeklyScore", g.getWeeklyScore());
                    item.put("rankPoints", g.getRankPoints());
                    User leader = userRepository.findById(g.getLeaderId()).orElse(null);
                    item.put("leaderName", leader != null ? leader.getNickname() : "未知");
                    return item;
                })
                .collect(Collectors.toList());

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("guilds", result);
        return resultMap;
    }

    // ==================== 3. 加入公会 ====================

    @Transactional
    public Map<String, Object> joinGuild(Long userId, Long guildId) {
        // 检查是否有公会
        Optional<GuildMember> existing = guildMemberRepository.findByUserId(userId);
        if (existing.isPresent()) {
            throw new IllegalStateException("你已加入公会");
        }

        Guild guild = guildRepository.findById(guildId)
                .orElseThrow(() -> new IllegalStateException("公会不存在"));

        if (guild.getMemberCount() >= guild.getMaxMembers()) {
            throw new IllegalStateException("公会已满员");
        }

        GuildMember member = new GuildMember();
        member.setGuildId(guildId);
        member.setUserId(userId);
        member.setRole("member");
        guildMemberRepository.save(member);

        guild.setMemberCount(guild.getMemberCount() + 1);
        guildRepository.save(guild);

        // 成就检查
        achievementService.checkByConditionType(userId, "join_guild", 1);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("guildId", guildId);
        result.put("guildName", guild.getName());
        return result;
    }

    // ==================== 4. 退出公会 ====================

    @Transactional
    public Map<String, Object> leaveGuild(Long userId) {
        GuildMember member = guildMemberRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("你未加入公会"));

        Guild guild = guildRepository.findById(member.getGuildId())
                .orElseThrow(() -> new IllegalStateException("公会不存在"));

        // 会长不能退出，需先转让
        if ("leader".equals(member.getRole())) {
            // 检查是否有其他成员
            List<GuildMember> members = guildMemberRepository.findByGuildId(guild.getId());
            if (members.size() > 1) {
                throw new IllegalStateException("会长请先转让会长身份再退出");
            }
        }

        guildMemberRepository.delete(member);
        guild.setMemberCount(Math.max(0, guild.getMemberCount() - 1));
        guildRepository.save(guild);

        // 如果公会已空，删除公会
        if (guild.getMemberCount() <= 0) {
            guildRepository.delete(guild);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return result;
    }

    // ==================== 5. 转让会长 ====================

    @Transactional
    public Map<String, Object> transferLeadership(Long userId, Long newLeaderId) {
        GuildMember currentLeader = guildMemberRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("你未加入公会"));

        if (!"leader".equals(currentLeader.getRole())) {
            throw new IllegalStateException("只有会长才能转让会长");
        }

        GuildMember newLeader = guildMemberRepository.findByUserId(newLeaderId)
                .orElseThrow(() -> new IllegalStateException("该用户未加入公会"));

        if (!newLeader.getGuildId().equals(currentLeader.getGuildId())) {
            throw new IllegalStateException("该用户不在你的公会中");
        }

        currentLeader.setRole("member");
        newLeader.setRole("leader");
        guildMemberRepository.save(currentLeader);
        guildMemberRepository.save(newLeader);

        Guild guild = guildRepository.findById(currentLeader.getGuildId()).orElse(null);
        if (guild != null) {
            guild.setLeaderId(newLeaderId);
            guildRepository.save(guild);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return result;
    }

    // ==================== 6. 踢人 ====================

    @Transactional
    public Map<String, Object> kickMember(Long userId, Long kickedUserId) {
        GuildMember leader = guildMemberRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("你未加入公会"));

        if (!"leader".equals(leader.getRole())) {
            throw new IllegalStateException("只有会长才能踢人");
        }

        GuildMember target = guildMemberRepository.findByUserId(kickedUserId)
                .orElseThrow(() -> new IllegalStateException("该用户未加入公会"));

        if (!target.getGuildId().equals(leader.getGuildId())) {
            throw new IllegalStateException("该用户不在你的公会中");
        }

        if ("leader".equals(target.getRole())) {
            throw new IllegalStateException("不能踢出会长");
        }

        guildMemberRepository.delete(target);

        Guild guild = guildRepository.findById(leader.getGuildId()).orElse(null);
        if (guild != null) {
            guild.setMemberCount(Math.max(0, guild.getMemberCount() - 1));
            guildRepository.save(guild);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return result;
    }

    // ==================== 7. 获取公会信息 ====================

    public Map<String, Object> getGuildInfo(Long guildId) {
        Guild guild = guildRepository.findById(guildId)
                .orElseThrow(() -> new IllegalStateException("公会不存在"));

        List<GuildMember> members = guildMemberRepository.findByGuildId(guildId);
        List<Map<String, Object>> memberList = members.stream().map(m -> {
            Map<String, Object> item = new HashMap<>();
            item.put("userId", m.getUserId());
            item.put("role", m.getRole());
            item.put("weeklyCorrect", m.getWeeklyCorrect());
            item.put("weeklyScore", m.getWeeklyScore());
            item.put("joinedAt", m.getJoinedAt());
            User u = userRepository.findById(m.getUserId()).orElse(null);
            item.put("nickname", u != null ? u.getNickname() : "未知");
            return item;
        }).collect(Collectors.toList());

        // 本周排行Top3
        List<GuildMember> topMembers = guildMemberRepository.findTop3ByGuildIdOrderByWeeklyScoreDesc(guildId);
        List<Map<String, Object>> topList = topMembers.stream().map(m -> {
            Map<String, Object> item = new HashMap<>();
            item.put("userId", m.getUserId());
            item.put("weeklyScore", m.getWeeklyScore());
            User u = userRepository.findById(m.getUserId()).orElse(null);
            item.put("nickname", u != null ? u.getNickname() : "未知");
            return item;
        }).collect(Collectors.toList());

        // 宝库
        List<GuildTreasure> treasures = guildTreasureRepository.findByGuildId(guildId);
        List<Map<String, Object>> treasureList = treasures.stream().map(t -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", t.getId());
            item.put("milestone", t.getMilestone());
            item.put("chestType", t.getChestType());
            item.put("claimedCount", t.getClaimedCount());
            item.put("maxClaims", t.getMaxClaims());
            item.put("unlocked", guild.getTotalCardsCollected() >= t.getMilestone());
            return item;
        }).collect(Collectors.toList());

        User leader = userRepository.findById(guild.getLeaderId()).orElse(null);

        Map<String, Object> result = new HashMap<>();
        result.put("id", guild.getId());
        result.put("name", guild.getName());
        result.put("description", guild.getDescription());
        result.put("leaderId", guild.getLeaderId());
        result.put("leaderName", leader != null ? leader.getNickname() : "未知");
        result.put("memberCount", guild.getMemberCount());
        result.put("maxMembers", guild.getMaxMembers());
        result.put("totalCardsCollected", guild.getTotalCardsCollected());
        result.put("weeklyScore", guild.getWeeklyScore());
        result.put("rankPoints", guild.getRankPoints());
        result.put("createdAt", guild.getCreatedAt());
        result.put("members", memberList);
        result.put("topContributors", topList);
        result.put("treasures", treasureList);
        return result;
    }

    // ==================== 8. 获取我的公会 ====================

    public Map<String, Object> getMyGuild(Long userId) {
        Optional<GuildMember> memberOpt = guildMemberRepository.findByUserId(userId);
        if (memberOpt.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("inGuild", false);
            result.put("presetNames", PRESET_NAMES);
            return result;
        }

        GuildMember member = memberOpt.get();
        Map<String, Object> guildInfo = getGuildInfo(member.getGuildId());
        guildInfo.put("inGuild", true);
        guildInfo.put("myRole", member.getRole());
        guildInfo.put("myWeeklyCorrect", member.getWeeklyCorrect());
        guildInfo.put("myWeeklyScore", member.getWeeklyScore());
        return guildInfo;
    }

    // ==================== 9. 公会排行榜 ====================

    public Map<String, Object> getLeaderboard() {
        List<Guild> guilds = guildRepository.findAllByOrderByRankPointsDesc();
        List<Map<String, Object>> ranking = new ArrayList<>();
        int rank = 1;
        for (Guild g : guilds) {
            Map<String, Object> item = new HashMap<>();
            item.put("rank", rank++);
            item.put("id", g.getId());
            item.put("name", g.getName());
            item.put("memberCount", g.getMemberCount());
            item.put("weeklyScore", g.getWeeklyScore());
            item.put("rankPoints", g.getRankPoints());
            User leader = userRepository.findById(g.getLeaderId()).orElse(null);
            item.put("leaderName", leader != null ? leader.getNickname() : "未知");
            ranking.add(item);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("leaderboard", ranking);
        return result;
    }

    // ==================== 10. 更新贡献 ====================

    @Transactional
    public Map<String, Object> updateWeeklyScore(Long userId, int correctCount) {
        Optional<GuildMember> memberOpt = guildMemberRepository.findByUserId(userId);
        if (memberOpt.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("inGuild", false);
            return result;
        }

        GuildMember member = memberOpt.get();
        member.setWeeklyCorrect(member.getWeeklyCorrect() + correctCount);
        member.setWeeklyScore(member.getWeeklyScore() + correctCount * 10);
        guildMemberRepository.save(member);

        // 更新公会总收集数
        Guild guild = guildRepository.findById(member.getGuildId()).orElse(null);
        if (guild != null) {
            guild.setWeeklyScore(guild.getWeeklyScore() + correctCount * 10);
            guild.setTotalCardsCollected(guild.getTotalCardsCollected() + correctCount);
            guildRepository.save(guild);

            // 检查是否触发新的宝库里程碑
            checkTreasureMilestones(guild);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return result;
    }

    // ==================== 11. 领取宝库 ====================

    @Transactional
    public Map<String, Object> claimTreasure(Long userId, Long treasureId) {
        GuildTreasure treasure = guildTreasureRepository.findById(treasureId)
                .orElseThrow(() -> new IllegalStateException("宝库记录不存在"));

        GuildMember member = guildMemberRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("你未加入公会"));

        if (!member.getGuildId().equals(treasure.getGuildId())) {
            throw new IllegalStateException("该宝库不属于你的公会");
        }

        // 检查是否满足里程碑
        Guild guild = guildRepository.findById(treasure.getGuildId())
                .orElseThrow(() -> new IllegalStateException("公会不存在"));
        if (guild.getTotalCardsCollected() < treasure.getMilestone()) {
            throw new IllegalStateException("未达到里程碑");
        }

        // 检查是否已领取
        if (guildTreasureClaimRepository.findByTreasureIdAndUserId(treasureId, userId).isPresent()) {
            throw new IllegalStateException("已领取过该宝库奖励");
        }

        // 检查领取上限
        long currentClaims = guildTreasureClaimRepository.countByTreasureId(treasureId);
        if (currentClaims >= treasure.getMaxClaims()) {
            throw new IllegalStateException("该宝库奖励已被领完");
        }

        // 记录领取
        GuildTreasureClaim claim = new GuildTreasureClaim();
        claim.setTreasureId(treasureId);
        claim.setUserId(userId);
        guildTreasureClaimRepository.save(claim);

        treasure.setClaimedCount(treasure.getClaimedCount() + 1);
        guildTreasureRepository.save(treasure);

        // 发放奖励
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("用户不存在"));

        int stardustReward = switch (treasure.getChestType()) {
            case "gold" -> 300;
            case "silver" -> 150;
            default -> 50;
        };
        user.setStardust(user.getStardust() + stardustReward);
        userRepository.save(user);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("reward", stardustReward);
        result.put("chestType", treasure.getChestType());
        return result;
    }

    // ==================== 12. 领地战结算（定时任务） ====================

    @Transactional
    public Map<String, Object> settleTerritoryWar() {
        List<Guild> allGuilds = guildRepository.findAll();
        // 按周积分排序
        allGuilds.sort((a, b) -> Integer.compare(b.getWeeklyScore(), a.getWeeklyScore()));

        int rank = 1;
        for (Guild guild : allGuilds) {
            int rewardStardust = 0;
            String chestType = "bronze";

            if (rank <= 3) {
                rewardStardust = 200;
                chestType = "gold";
            } else if (rank <= 10) {
                rewardStardust = 100;
                chestType = "silver";
            } else {
                rewardStardust = 50;
                chestType = "bronze";
            }

            // 给每个成员发奖励
            List<GuildMember> members = guildMemberRepository.findByGuildId(guild.getId());
            for (GuildMember member : members) {
                User user = userRepository.findById(member.getUserId()).orElse(null);
                if (user != null) {
                    user.setStardust(user.getStardust() + rewardStardust);
                    userRepository.save(user);
                }
                // 成就检查
                achievementService.checkByConditionType(member.getUserId(), "guild_war_win", 1);
                // 重置周数据
                member.setWeeklyCorrect(0);
                member.setWeeklyScore(0);
                guildMemberRepository.save(member);
            }

            // 增加排名积分
            guild.setRankPoints(guild.getRankPoints() + Math.max(1, 11 - rank));
            guild.setWeeklyScore(0);
            guildRepository.save(guild);

            rank++;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("settled", true);
        return result;
    }

    // ==================== 内部方法 ====================

    private void createTreasureMilestones(Long guildId) {
        Object[][] milestones = {
                {500, "bronze"},
                {1000, "silver"},
                {2000, "gold"}
        };

        for (Object[] ms : milestones) {
            GuildTreasure t = new GuildTreasure();
            t.setGuildId(guildId);
            t.setMilestone((Integer) ms[0]);
            t.setChestType((String) ms[1]);
            t.setMaxClaims(30);
            guildTreasureRepository.save(t);
        }
    }

    private void checkTreasureMilestones(Guild guild) {
        List<GuildTreasure> treasures = guildTreasureRepository.findByGuildId(guild.getId());
        for (GuildTreasure t : treasures) {
            if (guild.getTotalCardsCollected() >= t.getMilestone()) {
                // 里程碑已达成，无需额外操作
            }
        }
    }

    // ==================== 14. 公会联赛 ====================

    /**
     * 计算所有公会联赛积分
     */
    @Transactional
    public Map<String, Object> calculateLeagueScores() {
        List<Guild> allGuilds = guildRepository.findAll();
        for (Guild guild : allGuilds) {
            // 联赛积分基于公会总收集数
            guild.setLeagueScore(guild.getTotalCardsCollected());
            guildRepository.save(guild);
        }

        // 排名
        List<Guild> ranked = guildRepository.findAllByOrderByRankPointsDesc();
        int rank = 1;
        for (Guild g : ranked) {
            g.setLeagueRank(rank++);
            guildRepository.save(g);
        }

        return Map.of("success", true, "totalGuilds", allGuilds.size());
    }

    /**
     * 获取联赛排行
     */
    public List<Map<String, Object>> getLeagueStandings() {
        List<Guild> ranked = guildRepository.findAllByOrderByRankPointsDesc();
        List<Map<String, Object>> standings = new ArrayList<>();
        int rank = 1;
        for (Guild g : ranked) {
            Map<String, Object> item = new HashMap<>();
            item.put("rank", rank++);
            item.put("id", g.getId());
            item.put("name", g.getName());
            item.put("memberCount", g.getMemberCount());
            item.put("leagueScore", g.getLeagueScore());
            item.put("leagueRank", g.getLeagueRank());
            User leader = userRepository.findById(g.getLeaderId()).orElse(null);
            item.put("leaderName", leader != null ? leader.getNickname() : "未知");
            standings.add(item);
        }
        return standings;
    }

    /**
     * 公会联赛当前排名
     */
    public Map<String, Object> getGuildLeagueInfo(Long guildId) {
        Guild guild = guildRepository.findById(guildId).orElse(null);
        if (guild == null) return Map.of("inLeague", false);

        var seasonOpt = guildLeagueSeasonRepository.findTopByOrderBySeasonNumberDesc();

        Map<String, Object> result = new HashMap<>();
        result.put("inLeague", true);
        result.put("guildId", guild.getId());
        result.put("guildName", guild.getName());
        result.put("leagueScore", guild.getLeagueScore());
        result.put("leagueRank", guild.getLeagueRank());
        if (seasonOpt.isPresent()) {
            result.put("seasonNumber", seasonOpt.get().getSeasonNumber());
            result.put("seasonStatus", seasonOpt.get().getStatus());
        }
        return result;
    }

    private Map<String, Object> buildGuildData(Guild guild, Long userId) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", guild.getId());
        data.put("name", guild.getName());
        data.put("description", guild.getDescription());
        data.put("leaderId", guild.getLeaderId());
        data.put("memberCount", guild.getMemberCount());
        data.put("maxMembers", guild.getMaxMembers());
        data.put("totalCardsCollected", guild.getTotalCardsCollected());
        data.put("weeklyScore", guild.getWeeklyScore());
        data.put("rankPoints", guild.getRankPoints());
        data.put("createdAt", guild.getCreatedAt());

        User leader = userRepository.findById(guild.getLeaderId()).orElse(null);
        data.put("leaderName", leader != null ? leader.getNickname() : "未知");

        Optional<GuildMember> myMember = guildMemberRepository.findByUserId(userId);
        if (myMember.isPresent() && myMember.get().getGuildId().equals(guild.getId())) {
            data.put("myRole", myMember.get().getRole());
            data.put("myWeeklyCorrect", myMember.get().getWeeklyCorrect());
            data.put("myWeeklyScore", myMember.get().getWeeklyScore());
        }

        return data;
    }
}
