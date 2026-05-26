package com.pengyouquan.english.service;

import com.pengyouquan.english.dto.*;
import com.pengyouquan.english.model.*;
import com.pengyouquan.english.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GamificationService {

    private final UserRepository userRepository;
    private final SentenceRepository sentenceRepository;
    private final DailyChallengeRepository dailyChallengeRepository;
    private final DailyChallengeQuestionRepository dailyChallengeQuestionRepository;
    private final StreakRewardRepository streakRewardRepository;
    private final UserStreakRewardRepository userStreakRewardRepository;

    public GamificationService(UserRepository userRepository,
                               SentenceRepository sentenceRepository,
                               DailyChallengeRepository dailyChallengeRepository,
                               DailyChallengeQuestionRepository dailyChallengeQuestionRepository,
                               StreakRewardRepository streakRewardRepository,
                               UserStreakRewardRepository userStreakRewardRepository) {
        this.userRepository = userRepository;
        this.sentenceRepository = sentenceRepository;
        this.dailyChallengeRepository = dailyChallengeRepository;
        this.dailyChallengeQuestionRepository = dailyChallengeQuestionRepository;
        this.streakRewardRepository = streakRewardRepository;
        this.userStreakRewardRepository = userStreakRewardRepository;
    }

    public PrestigeResponse getPrestige(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        int prestige = user.getPrestige() != null ? user.getPrestige() : 0;
        int rankTierVal = user.getRankTier() != null ? user.getRankTier() : 1;
        RankTier rank = RankTier.fromTier(rankTierVal);
        int consecutiveDays = user.getConsecutiveDays() != null ? user.getConsecutiveDays() : 0;

        return new PrestigeResponse(
            prestige,
            rank.getTier(),
            rank.getTitleCn(),
            rank.getTitleEn(),
            rank.getNextRequiredPrestige(),
            rank.getProgress(prestige),
            consecutiveDays
        );
    }

    @Transactional
    public DailyChallengeResponse getOrCreateDailyChallenge(Long userId) {
        LocalDate today = LocalDate.now();

        // 查询今天有无挑战
        var existing = dailyChallengeRepository.findByUserIdAndChallengeDate(userId, today);
        if (existing.isPresent()) {
            return buildChallengeResponse(existing.get());
        }

        // 无挑战，创建新的
        List<Sentence> sentences = sentenceRepository.findRandom(10);
        if (sentences.isEmpty()) {
            throw new IllegalStateException("没有可用的句子，请管理员先导入数据");
        }

        DailyChallenge challenge = new DailyChallenge();
        challenge.setUserId(userId);
        challenge.setChallengeDate(today);
        challenge.setTotalQuestions(sentences.size());
        challenge = dailyChallengeRepository.save(challenge);

        Long challengeId = challenge.getId();
        List<DailyChallengeQuestion> questions = new ArrayList<>();
        for (Sentence s : sentences) {
            DailyChallengeQuestion q = new DailyChallengeQuestion();
            q.setChallengeId(challengeId);
            q.setSentenceId(s.getId());
            questions.add(q);
        }
        questions = dailyChallengeQuestionRepository.saveAll(questions);

        return buildChallengeResponse(challenge, questions, sentences);
    }

    @Transactional
    public SubmitAnswerResult submitAnswer(Long userId, Long challengeId, Long questionId, String answer) {
        DailyChallenge challenge = dailyChallengeRepository.findById(challengeId)
            .orElseThrow(() -> new IllegalStateException("挑战不存在"));

        if (!challenge.getUserId().equals(userId)) {
            throw new IllegalStateException("无权操作此挑战");
        }
        if (challenge.getCompleted()) {
            throw new IllegalStateException("挑战已结束");
        }

        DailyChallengeQuestion question = dailyChallengeQuestionRepository.findById(questionId)
            .orElseThrow(() -> new IllegalStateException("题目不存在"));

        if (!question.getChallengeId().equals(challengeId)) {
            throw new IllegalStateException("题目不属于此挑战");
        }
        if (question.getIsCorrect() != null) {
            throw new IllegalStateException("此题已作答");
        }

        // 获取句子原文
        Sentence sentence = sentenceRepository.findById(question.getSentenceId())
            .orElseThrow(() -> new IllegalStateException("句子不存在"));

        // 校验答案
        boolean correct = answer != null
            && answer.trim().toLowerCase().equals(sentence.getText().trim().toLowerCase());

        question.setUserAnswer(answer);
        question.setIsCorrect(correct);
        question.setAnsweredAt(LocalDateTime.now());
        dailyChallengeQuestionRepository.save(question);

        // 更新挑战的 correct_count
        if (correct) {
            challenge.setCorrectCount(challenge.getCorrectCount() + 1);
            dailyChallengeRepository.save(challenge);
        }

        return new SubmitAnswerResult(questionId, correct, correct ? "回答正确!" : "回答错误");
    }

    @Transactional
    public CompleteChallengeResult completeChallenge(Long userId, Long challengeId) {
        DailyChallenge challenge = dailyChallengeRepository.findById(challengeId)
            .orElseThrow(() -> new IllegalStateException("挑战不存在"));

        if (!challenge.getUserId().equals(userId)) {
            throw new IllegalStateException("无权操作此挑战");
        }
        if (challenge.getCompleted()) {
            throw new IllegalStateException("挑战已完成");
        }

        // 统计已答和正确数
        List<DailyChallengeQuestion> questions = dailyChallengeQuestionRepository.findByChallengeIdOrderById(challengeId);
        long answered = questions.stream().filter(q -> q.getIsCorrect() != null).count();
        long correct = questions.stream().filter(q -> Boolean.TRUE.equals(q.getIsCorrect())).count();

        if (answered < challenge.getTotalQuestions()) {
            throw new IllegalStateException("还有题目未作答，请先完成所有题目");
        }

        // 基础奖励: 每题+10威望
        int baseReward = (int) correct * 10;
        // 连击奖励: 全对额外+50
        int comboBonus = (correct == challenge.getTotalQuestions()) ? 50 : 0;
        int totalPrestigeEarned = baseReward + comboBonus;

        // 更新挑战
        challenge.setCorrectCount((int) correct);
        challenge.setPrestigeEarned(totalPrestigeEarned);
        challenge.setComboCount((int) (correct == challenge.getTotalQuestions() ? 1 : 0));
        challenge.setCompleted(true);
        dailyChallengeRepository.save(challenge);

        // 更新用户
        LocalDate today = LocalDate.now();
        User user = userRepository.findById(userId).orElseThrow();
        int oldRankTierVal = user.getRankTier() != null ? user.getRankTier() : 1;
        user.setPrestige(user.getPrestige() + totalPrestigeEarned);
        user.setRankTier(RankTier.fromPrestige(user.getPrestige()).getTier());

        // 续期打卡
        if (user.getLastDailyDate() == null || user.getLastDailyDate().equals(today.minusDays(1))) {
            user.setConsecutiveDays(user.getConsecutiveDays() + 1);
        } else if (!user.getLastDailyDate().equals(today)) {
            user.setConsecutiveDays(1);
        }
        user.setLastDailyDate(today);
        userRepository.save(user);

        // Check rank tier change
        RankTier oldRank = RankTier.fromTier(oldRankTierVal);
        int newRankTierVal = RankTier.fromPrestige(user.getPrestige()).getTier();
        RankTier newRank = RankTier.fromTier(newRankTierVal);

        return new CompleteChallengeResult((int) correct, challenge.getTotalQuestions(), baseReward,
            comboBonus, totalPrestigeEarned, oldRankTierVal, newRankTierVal,
            oldRank.getTitleCn(), oldRank.getTitleEn(),
            newRank.getTitleCn(), newRank.getTitleEn());
    }

    public List<ChallengeHistoryEntry> getChallengeHistory(Long userId) {
        var challenges = dailyChallengeRepository
            .findByUserIdAndCompletedTrueOrderByCreatedAtDesc(userId, PageRequest.of(0, 5));
        return challenges.stream()
            .map(c -> new ChallengeHistoryEntry(c.getChallengeDate(), c.getCorrectCount(), c.getTotalQuestions(), c.getPrestigeEarned()))
            .collect(Collectors.toList());
    }

    public List<LeaderboardEntry> getLeaderboard(Long userId, String period) {
        LocalDate today = LocalDate.now();
        LocalDate startDate;

        switch (period) {
            case "week":
                startDate = today.minusDays(today.getDayOfWeek().getValue() - 1);
                break;
            case "month":
                startDate = today.withDayOfMonth(1);
                break;
            case "friends":
                // 由于目前无好友系统，回退到今日排行
                startDate = today;
                period = "today";
                break;
            default: // today
                startDate = today;
                break;
        }

        // 查询该时间段内完成挑战的记录
        List<DailyChallenge> challenges = dailyChallengeRepository
            .findByCompletedAndChallengeDateBetween(true, startDate, today);

        // 按用户汇总 correct_count
        var userStats = challenges.stream()
            .collect(Collectors.groupingBy(
                DailyChallenge::getUserId,
                Collectors.summingInt(DailyChallenge::getCorrectCount)
            ));

        // 获取用户信息并排序
        List<LeaderboardEntry> entries = new ArrayList<>();
        for (var entry : userStats.entrySet()) {
            User u = userRepository.findById(entry.getKey()).orElse(null);
            if (u == null) continue;
            RankTier rank = RankTier.fromPrestige(u.getPrestige() != null ? u.getPrestige() : 0);
            entries.add(new LeaderboardEntry(
                0,
                u.getId(),
                u.getNickname() != null ? u.getNickname() : "",
                u.getAvatar() != null ? u.getAvatar() : "",
                u.getPrestige() != null ? u.getPrestige() : 0,
                rank.getTier(),
                rank.getTitleCn(),
                entry.getValue()
            ));
        }

        // 按 correct_count 降序排列
        entries.sort(Comparator.comparingInt(LeaderboardEntry::getCorrectCount).reversed());

        // 设置排名
        for (int i = 0; i < entries.size(); i++) {
            entries.get(i).setRank(i + 1);
        }

        return entries;
    }

    // ---- 邀请封臣系统 ----

    @Transactional
    public Map<String, String> getInviteCode(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        if (user.getInviteCode() == null || user.getInviteCode().isEmpty()) {
            String code = generateInviteCode(user);
            user.setInviteCode(code);
            userRepository.save(user);
        }
        return Collections.singletonMap("inviteCode", user.getInviteCode());
    }

    private String generateInviteCode(User user) {
        String idPart = String.format("%04d", user.getId());
        String randPart = UUID.randomUUID().toString().substring(0, 2).toUpperCase();
        return "PYQ-" + idPart + randPart;
    }

    @Transactional
    public Map<String, String> recruit(Long userId, String inviteCode) {
        if (inviteCode == null || inviteCode.isBlank()) {
            throw new IllegalStateException("邀请码不能为空");
        }
        User inviter = userRepository.findByInviteCode(inviteCode)
            .orElseThrow(() -> new IllegalStateException("邀请码无效"));
        if (inviter.getId().equals(userId)) {
            throw new IllegalStateException("不能招募自己");
        }
        User user = userRepository.findById(userId).orElseThrow();
        if (user.getInvitedBy() != null && !user.getInvitedBy().isEmpty()) {
            throw new IllegalStateException("已被招募过");
        }
        user.setInvitedBy(inviteCode);
        userRepository.save(user);
        return Collections.singletonMap("recruited", user.getNickname() != null ? user.getNickname() : "");
    }

    public List<ClanMemberVO> getClan(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        String myInviteCode = user.getInviteCode();
        if (myInviteCode == null || myInviteCode.isEmpty()) {
            return Collections.emptyList();
        }
        List<User> clanMembers = userRepository.findByInvitedBy(myInviteCode);
        return clanMembers.stream().map(u -> {
            RankTier rank = RankTier.fromPrestige(u.getPrestige() != null ? u.getPrestige() : 0);
            return new ClanMemberVO(
                u.getId(),
                u.getNickname() != null ? u.getNickname() : "",
                u.getPrestige() != null ? u.getPrestige() : 0,
                rank.getTier(),
                rank.getTitleCn(),
                u.getCreatedAt() != null ? u.getCreatedAt().toLocalDate().toString() : ""
            );
        }).collect(Collectors.toList());
    }

    // ---- 封号路线 ----

    public List<RankTierVO> getRankTiers(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        int currentTier = user.getRankTier() != null ? user.getRankTier() : 1;
        List<RankTierVO> result = new ArrayList<>();
        for (RankTier rt : RankTier.values()) {
            result.add(new RankTierVO(rt.getTier(), rt.getTitleCn(), rt.getTitleEn(),
                rt.getRequiredPrestige(), rt.getTier() == currentTier));
        }
        return result;
    }

    // ---- 连续统治奖励 ----

    public List<StreakRewardVO> getStreakRewards(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        int consecutiveDays = user.getConsecutiveDays() != null ? user.getConsecutiveDays() : 0;
        List<StreakReward> allRewards = streakRewardRepository.findAllByOrderByDaysRequiredAsc();
        List<UserStreakReward> claimed = userStreakRewardRepository.findByUserId(userId);
        Set<Long> claimedIds = claimed.stream().map(UserStreakReward::getRewardId).collect(Collectors.toSet());

        return allRewards.stream().map(r -> new StreakRewardVO(
            r.getId(), r.getDaysRequired(), r.getRewardType(), r.getRewardName(),
            r.getRewardIcon(), r.getDescription(), consecutiveDays, claimedIds.contains(r.getId())
        )).collect(Collectors.toList());
    }

    @Transactional
    public void claimStreakReward(Long userId, Long rewardId) {
        User user = userRepository.findById(userId).orElseThrow();
        StreakReward reward = streakRewardRepository.findById(rewardId)
            .orElseThrow(() -> new IllegalStateException("奖励不存在"));
        int consecutiveDays = user.getConsecutiveDays() != null ? user.getConsecutiveDays() : 0;
        if (consecutiveDays < reward.getDaysRequired()) {
            throw new IllegalStateException("未达到领取条件，需要连续" + reward.getDaysRequired() + "天");
        }
        if (userStreakRewardRepository.findByUserIdAndRewardId(userId, rewardId).isPresent()) {
            throw new IllegalStateException("已领取过该奖励");
        }
        UserStreakReward usr = new UserStreakReward();
        usr.setUserId(userId);
        usr.setRewardId(rewardId);
        userStreakRewardRepository.save(usr);
    }

    // ---- 内部方法 ----

    private DailyChallengeResponse buildChallengeResponse(DailyChallenge challenge) {
        List<DailyChallengeQuestion> questions = dailyChallengeQuestionRepository.findByChallengeIdOrderById(challenge.getId());
        List<Sentence> sentences = questions.stream()
            .map(q -> sentenceRepository.findById(q.getSentenceId()).orElse(null))
            .collect(Collectors.toList());
        return buildChallengeResponse(challenge, questions, sentences);
    }

    private DailyChallengeResponse buildChallengeResponse(DailyChallenge challenge,
                                                           List<DailyChallengeQuestion> questions,
                                                           List<Sentence> sentences) {
        List<ChallengeQuestionDTO> dtos = new ArrayList<>();
        int answeredCount = 0;
        int correctCount = 0;

        for (int i = 0; i < questions.size(); i++) {
            DailyChallengeQuestion q = questions.get(i);
            Sentence s = (i < sentences.size()) ? sentences.get(i) : null;

            boolean answered = q.getIsCorrect() != null;
            if (answered) {
                answeredCount++;
                if (Boolean.TRUE.equals(q.getIsCorrect())) correctCount++;
            }

            dtos.add(new ChallengeQuestionDTO(
                q.getId(),
                q.getSentenceId(),
                s != null ? s.getText() : "",
                "",
                s != null ? s.getAudioFile() : "",
                answered,
                q.getIsCorrect(),
                q.getUserAnswer()
            ));
        }

        return new DailyChallengeResponse(
            challenge.getId(),
            dtos,
            answeredCount,
            correctCount,
            challenge.getCompleted(),
            challenge.getComboCount()
        );
    }

    // ---- 内部结果类 ----

    public static class SubmitAnswerResult {
        private Long questionId;
        private boolean correct;
        private String message;

        public SubmitAnswerResult(Long questionId, boolean correct, String message) {
            this.questionId = questionId;
            this.correct = correct;
            this.message = message;
        }

        public Long getQuestionId() { return questionId; }
        public boolean isCorrect() { return correct; }
        public String getMessage() { return message; }
    }

    public static class CompleteChallengeResult {
        private int correctCount;
        private int totalQuestions;
        private int baseReward;
        private int comboBonus;
        private int totalReward;
        private int oldRankTier;
        private int newRankTier;
        private String oldTitleCn;
        private String oldTitleEn;
        private String newTitleCn;
        private String newTitleEn;

        public CompleteChallengeResult(int correctCount, int totalQuestions, int baseReward, int comboBonus, int totalReward,
                                       int oldRankTier, int newRankTier,
                                       String oldTitleCn, String oldTitleEn, String newTitleCn, String newTitleEn) {
            this.correctCount = correctCount;
            this.totalQuestions = totalQuestions;
            this.baseReward = baseReward;
            this.comboBonus = comboBonus;
            this.totalReward = totalReward;
            this.oldRankTier = oldRankTier;
            this.newRankTier = newRankTier;
            this.oldTitleCn = oldTitleCn;
            this.oldTitleEn = oldTitleEn;
            this.newTitleCn = newTitleCn;
            this.newTitleEn = newTitleEn;
        }

        public int getCorrectCount() { return correctCount; }
        public int getTotalQuestions() { return totalQuestions; }
        public int getBaseReward() { return baseReward; }
        public int getComboBonus() { return comboBonus; }
        public int getTotalReward() { return totalReward; }
        public int getOldRankTier() { return oldRankTier; }
        public int getNewRankTier() { return newRankTier; }
        public String getOldTitleCn() { return oldTitleCn; }
        public String getOldTitleEn() { return oldTitleEn; }
        public String getNewTitleCn() { return newTitleCn; }
        public String getNewTitleEn() { return newTitleEn; }
    }
}
