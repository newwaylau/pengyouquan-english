package com.pengyouquan.english.service;

import com.pengyouquan.english.dto.FriendDTO;
import com.pengyouquan.english.model.*;
import com.pengyouquan.english.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class FriendService {

    private final FriendRepository friendRepository;
    private final UserRepository userRepository;
    private final UserStatsRepository userStatsRepository;
    private final BattleHistoryRepository battleHistoryRepository;
    private final TrophyService trophyService;
    private final OnlineUserTracker onlineUserTracker;
    private final com.pengyouquan.english.battle.MatchmakingService matchmakingService;

    public FriendService(FriendRepository friendRepository,
                         UserRepository userRepository,
                         UserStatsRepository userStatsRepository,
                         BattleHistoryRepository battleHistoryRepository,
                         TrophyService trophyService,
                         OnlineUserTracker onlineUserTracker,
                         com.pengyouquan.english.battle.MatchmakingService matchmakingService) {
        this.friendRepository = friendRepository;
        this.userRepository = userRepository;
        this.userStatsRepository = userStatsRepository;
        this.battleHistoryRepository = battleHistoryRepository;
        this.trophyService = trophyService;
        this.onlineUserTracker = onlineUserTracker;
        this.matchmakingService = matchmakingService;
    }

    /** 搜索用户（非管理员，供添加好友用） */
    public List<FriendDTO> searchUsers(Long userId, String query) {
        if (query == null || query.isBlank()) return List.of();
        List<User> users = userRepository.searchByKeyword(query.trim());
        return users.stream()
                .filter(u -> !u.getId().equals(userId))
                .map(u -> new FriendDTO(null, u.getId(),
                        u.getNickname() != null ? u.getNickname() : "",
                        u.getEmail() != null ? u.getEmail() : "",
                        u.getAvatar() != null ? u.getAvatar() : "",
                        null, null))
                .limit(20)
                .collect(Collectors.toList());
    }

    /** 发送好友请求 */
    @Transactional
    public void sendRequest(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            throw new IllegalStateException("不能添加自己为好友");
        }
        // 验证对方存在
        userRepository.findById(friendId).orElseThrow(() -> new IllegalStateException("用户不存在"));

        // 检查是否已是好友
        var existing = friendRepository.findByUserIdAndFriendId(userId, friendId);
        if (existing.isPresent()) {
            Friend f = existing.get();
            if ("accepted".equals(f.getStatus())) {
                throw new IllegalStateException("已是好友");
            }
            if ("pending".equals(f.getStatus())) {
                throw new IllegalStateException("已发送过好友请求");
            }
            // blocked 的情况，重新发送
            f.setStatus("pending");
            friendRepository.save(f);
            return;
        }

        // 检查对方是否已向自己发送请求
        var reverse = friendRepository.findByUserIdAndFriendId(friendId, userId);
        if (reverse.isPresent() && "pending".equals(reverse.get().getStatus())) {
            // 互相为好友，自动接受
            reverse.get().setStatus("accepted");
            friendRepository.save(reverse.get());

            Friend f = new Friend();
            f.setUserId(userId);
            f.setFriendId(friendId);
            f.setStatus("accepted");
            friendRepository.save(f);
            return;
        }

        Friend f = new Friend();
        f.setUserId(userId);
        f.setFriendId(friendId);
        f.setStatus("pending");
        friendRepository.save(f);
    }

    /** 接受好友请求 */
    @Transactional
    public void acceptRequest(Long userId, Long requesterId) {
        Friend f = friendRepository.findByUserIdAndFriendIdAndStatus(requesterId, userId, "pending")
                .orElseThrow(() -> new IllegalStateException("没有待处理的好友请求"));
        f.setStatus("accepted");
        friendRepository.save(f);

        // 建立双向关系
        var reverse = friendRepository.findByUserIdAndFriendId(userId, requesterId);
        if (reverse.isEmpty()) {
            Friend rev = new Friend();
            rev.setUserId(userId);
            rev.setFriendId(requesterId);
            rev.setStatus("accepted");
            friendRepository.save(rev);
        } else {
            reverse.get().setStatus("accepted");
            friendRepository.save(reverse.get());
        }
    }

    /** 获取好友列表 */
    public List<FriendDTO> getFriends(Long userId) {
        List<Friend> outgoing = friendRepository.findByUserId(userId);
        return outgoing.stream()
                .filter(f -> "accepted".equals(f.getStatus()))
                .map(f -> {
                    User friend = userRepository.findById(f.getFriendId()).orElse(null);
                    return new FriendDTO(
                            f.getId(),
                            f.getFriendId(),
                            friend != null ? friend.getNickname() : "已注销",
                            friend != null ? friend.getEmail() : "",
                            friend != null ? friend.getAvatar() : "",
                            f.getStatus(),
                            f.getCreatedAt()
                    );
                })
                .collect(Collectors.toList());
    }

    /** 获取待处理的好友请求（别人向我发起的） */
    public List<FriendDTO> getPendingRequests(Long userId) {
        List<Friend> pending = friendRepository.findByFriendIdAndStatus(userId, "pending");
        return pending.stream().map(f -> {
            User requester = userRepository.findById(f.getUserId()).orElse(null);
            return new FriendDTO(
                    f.getId(),
                    f.getUserId(),
                    requester != null ? requester.getNickname() : "已注销",
                    requester != null ? requester.getEmail() : "",
                    requester != null ? requester.getAvatar() : "",
                    f.getStatus(),
                    f.getCreatedAt()
            );
        }).collect(Collectors.toList());
    }

    // ==================== 好友列表（含段位、在线状态） ====================

    public List<Map<String, Object>> getFriendListWithDetails(Long userId) {
        List<Friend> outgoing = friendRepository.findByUserId(userId);
        return outgoing.stream()
                .filter(f -> "accepted".equals(f.getStatus()))
                .map(f -> {
                    User friend = userRepository.findById(f.getFriendId()).orElse(null);
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", f.getId());
                    item.put("friendId", f.getFriendId());
                    item.put("nickname", friend != null ? friend.getNickname() : "已注销");
                    item.put("email", friend != null ? friend.getEmail() : "");
                    item.put("avatar", friend != null ? friend.getAvatar() : "");
                    item.put("status", f.getStatus());
                    item.put("createdAt", f.getCreatedAt());

                    // 段位信息
                    if (friend != null) {
                        UserStats stats = trophyService.getOrCreateUserStats(f.getFriendId());
                        item.put("trophies", stats.getTrophies());
                        com.pengyouquan.english.model.TrophyTier tier = trophyService.getTier(stats.getTrophies());
                        item.put("tierName", tier != null ? tier.getNameCn() : "未排名");
                        item.put("tierIcon", tier != null ? tier.getIcon() : "❓");
                        item.put("winStreak", stats.getWinStreak());
                    }

                    // 在线状态
                    item.put("online", friend != null && onlineUserTracker.getOnlineCount() > 0);
                    return item;
                })
                .collect(Collectors.toList());
    }

    // ==================== 好友切磋 ====================

    private final Map<Long, FriendChallenge> pendingChallenges = new java.util.concurrent.ConcurrentHashMap<>();
    private int nextChallengeId = 1;

    private static class FriendChallenge {
        long id;
        long challengerId;
        long defenderId;
        String status; // pending / accepted / rejected
        String sessionId; // 游戏会话ID（accepted后）
        long createdAt;

        FriendChallenge(long id, long challengerId, long defenderId) {
            this.id = id;
            this.challengerId = challengerId;
            this.defenderId = defenderId;
            this.status = "pending";
            this.createdAt = System.currentTimeMillis();
        }
    }

    /** 发送好友切磋邀请 */
    @Transactional
    public Map<String, Object> sendFriendChallenge(Long userId, Long friendId) {
        // 检查是否是好友
        var friendship = friendRepository.findByUserIdAndFriendId(userId, friendId);
        if (friendship.isEmpty() || !"accepted".equals(friendship.get().getStatus())) {
            throw new IllegalStateException("你们还不是好友");
        }

        // 检查是否有未处理的挑战
        boolean hasPending = pendingChallenges.values().stream()
                .anyMatch(c -> c.challengerId.equals(userId) && c.defenderId.equals(friendId)
                        && "pending".equals(c.status));
        if (hasPending) {
            throw new IllegalStateException("已向该好友发送过切磋邀请");
        }

        FriendChallenge challenge = new FriendChallenge(nextChallengeId++, userId, friendId);
        pendingChallenges.put(challenge.id, challenge);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("challengeId", challenge.id);
        result.put("message", "切磋邀请已发送");
        return result;
    }

    /** 接受好友切磋 */
    @Transactional
    public Map<String, Object> acceptFriendChallenge(Long challengeId, Long userId) {
        FriendChallenge challenge = pendingChallenges.get(challengeId);
        if (challenge == null) {
            throw new IllegalStateException("切磋邀请不存在或已过期");
        }
        if (!challenge.defenderId.equals(userId)) {
            throw new IllegalStateException("你不是被挑战者");
        }
        if (!"pending".equals(challenge.status)) {
            throw new IllegalStateException("该邀请已处理");
        }

        // 获取双方信息
        User challenger = userRepository.findById(challenge.challengerId)
                .orElseThrow(() -> new IllegalStateException("挑战者不存在"));
        User defender = userRepository.findById(challenge.defenderId)
                .orElseThrow(() -> new IllegalStateException("被挑战者不存在"));

        UserStats challengerStats = trophyService.getOrCreateUserStats(challenge.challengerId);
        UserStats defenderStats = trophyService.getOrCreateUserStats(challenge.defenderId);

        // 创建游戏会话
        matchmakingService.createDirectGame(
                challenge.challengerId,
                challenge.defenderId,
                challenger.getNickname(),
                defender.getNickname(),
                challengerStats.getTrophies(),
                defenderStats.getTrophies(),
                result -> {
                    // 通知挑战者
                    challenge.status = "accepted";
                    challenge.sessionId = result.getSessionId();
                },
                result -> {
                    // 通知被挑战者
                    challenge.status = "accepted";
                    challenge.sessionId = result.getSessionId();
                }
        );

        String sessionId = challenge.sessionId;

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("sessionId", sessionId);
        result.put("opponentName", challenger.getNickname());
        result.put("message", "切磋已接受，进入对战！");
        return result;
    }

    /** 拒绝好友切磋 */
    public Map<String, Object> rejectFriendChallenge(Long challengeId, Long userId) {
        FriendChallenge challenge = pendingChallenges.get(challengeId);
        if (challenge == null) {
            throw new IllegalStateException("切磋邀请不存在或已过期");
        }
        if (!challenge.defenderId.equals(userId) && !challenge.challengerId.equals(userId)) {
            throw new IllegalStateException("无权操作");
        }
        challenge.status = "rejected";
        pendingChallenges.remove(challengeId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return result;
    }

    /** 获取待处理的切磋邀请 */
    public List<Map<String, Object>> getPendingFriendChallenges(Long userId) {
        return pendingChallenges.values().stream()
                .filter(c -> c.defenderId.equals(userId) && "pending".equals(c.status))
                .map(c -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("challengeId", c.id);
                    item.put("challengerId", c.challengerId);
                    User challenger = userRepository.findById(c.challengerId).orElse(null);
                    item.put("challengerName", challenger != null ? challenger.getNickname() : "未知");
                    item.put("createdAt", c.createdAt);
                    item.put("status", c.status);
                    return item;
                })
                .collect(Collectors.toList());
    }

    /** 获取已接受的切磋 */
    public List<Map<String, Object>> getAcceptedFriendChallenges(Long userId) {
        return pendingChallenges.values().stream()
                .filter(c -> "accepted".equals(c.status) &&
                        (c.challengerId.equals(userId) || c.defenderId.equals(userId)))
                .map(c -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("challengeId", c.id);
                    item.put("sessionId", c.sessionId);
                    item.put("challengerId", c.challengerId);
                    item.put("defenderId", c.defenderId);
                    User challenger = userRepository.findById(c.challengerId).orElse(null);
                    User defender = userRepository.findById(c.defenderId).orElse(null);
                    item.put("challengerName", challenger != null ? challenger.getNickname() : "未知");
                    item.put("defenderName", defender != null ? defender.getNickname() : "未知");
                    return item;
                })
                .collect(Collectors.toList());
    }
}
