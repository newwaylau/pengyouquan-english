package com.pengyouquan.english.battle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 匹配队列。玩家加入队列后，系统尝试匹配同段位玩家。
 */
@Service
public class MatchmakingService {

    private static final Logger log = LoggerFactory.getLogger(MatchmakingService.class);

    private final ConcurrentLinkedQueue<MatchRequest> queue = new ConcurrentLinkedQueue<>();
    private final Map<Long, MatchRequest> pendingRequests = new ConcurrentHashMap<>();

    private final GameEngine gameEngine;

    private static final int BASE_RANGE = 200;
    private static final int RELAX_RANGE_1 = 400;
    private static final int RELAX_RANGE_2 = 800;

    public MatchmakingService(GameEngine gameEngine) {
        this.gameEngine = gameEngine;
        // 启动定时检查线程（每5秒检查一次，放宽匹配范围）
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "matchmaking-checker");
            t.setDaemon(true);
            return t;
        }).scheduleAtFixedRate(this::checkQueue, 5, 5, TimeUnit.SECONDS);
    }

    public static class MatchRequest {
        private Long userId;
        private String nickname;
        private int trophies;
        private long joinTime;
        private MatchResultCallback callback;

        public MatchRequest(Long userId, String nickname, int trophies, MatchResultCallback callback) {
            this.userId = userId;
            this.nickname = nickname;
            this.trophies = trophies;
            this.joinTime = System.currentTimeMillis();
            this.callback = callback;
        }

        public Long getUserId() { return userId; }
        public String getNickname() { return nickname; }
        public int getTrophies() { return trophies; }
        public long getJoinTime() { return joinTime; }
        public MatchResultCallback getCallback() { return callback; }
    }

    @FunctionalInterface
    public interface MatchResultCallback {
        void onMatched(MatchResult result);
    }

    public static class MatchResult {
        private String sessionId;
        private Long opponentId;
        private String opponentName;
        private int opponentTrophies;

        public MatchResult(String sessionId, Long opponentId, String opponentName, int opponentTrophies) {
            this.sessionId = sessionId;
            this.opponentId = opponentId;
            this.opponentName = opponentName;
            this.opponentTrophies = opponentTrophies;
        }

        public String getSessionId() { return sessionId; }
        public Long getOpponentId() { return opponentId; }
        public String getOpponentName() { return opponentName; }
        public int getOpponentTrophies() { return opponentTrophies; }
    }

    /**
     * 加入匹配队列。如果找到对手则触发回调，否则加入队列等待。
     */
    public synchronized void joinQueue(Long userId, String nickname, int trophies, MatchResultCallback callback) {
        // 如果已在队列中，先移除
        cancelMatch(userId);

        // 尝试匹配已有队列中的对手
        Iterator<MatchRequest> it = queue.iterator();
        while (it.hasNext()) {
            MatchRequest candidate = it.next();
            if (candidate.getUserId().equals(userId)) {
                it.remove();
                continue;
            }
            int range = getMatchRange(candidate);
            if (Math.abs(candidate.getTrophies() - trophies) <= range) {
                // 匹配成功！
                it.remove();
                pendingRequests.remove(candidate.getUserId());
                pendingRequests.remove(userId);

                log.info("Match found: {} (trophies:{}) vs {} (trophies:{})",
                        candidate.getNickname(), candidate.getTrophies(), nickname, trophies);

                // 创建游戏会话
                GameSession session = gameEngine.createGame(
                        candidate.getUserId(), userId,
                        candidate.getNickname(), nickname,
                        candidate.getTrophies(), trophies);

                // 通知双方
                MatchResult result1 = new MatchResult(session.getSessionId(), userId, nickname, trophies);
                MatchResult result2 = new MatchResult(session.getSessionId(), candidate.getUserId(), candidate.getNickname(), candidate.getTrophies());

                callback.onMatched(result2);
                candidate.getCallback().onMatched(result1);
                return;
            }
        }

        // 没匹配到，加入队列
        log.info("No match for user {} (trophies:{}), added to queue", nickname, trophies);
        MatchRequest request = new MatchRequest(userId, nickname, trophies, callback);
        queue.add(request);
        pendingRequests.put(userId, request);
    }

    /**
     * 创建直接对战（好友切磋），跳过匹配队列
     */
    public GameSession createDirectGame(Long userId1, Long userId2, String nickname1, String nickname2,
                                         int trophies1, int trophies2, MatchResultCallback callback1,
                                         MatchResultCallback callback2) {
        // 创建游戏会话
        GameSession session = gameEngine.createGame(userId1, userId2, nickname1, nickname2, trophies1, trophies2);

        // 通知双方
        MatchResult result1 = new MatchResult(session.getSessionId(), userId2, nickname2, trophies2);
        MatchResult result2 = new MatchResult(session.getSessionId(), userId1, nickname1, trophies1);

        if (callback1 != null) callback1.onMatched(result1);
        if (callback2 != null) callback2.onMatched(result2);

        return session;
    }

    /** 主动取消匹配 */
    public synchronized void cancelMatch(Long userId) {
        pendingRequests.remove(userId);
        queue.removeIf(r -> r.getUserId().equals(userId));
    }

    /** 用户是否在匹配队列中 */
    public boolean isInQueue(Long userId) {
        return pendingRequests.containsKey(userId);
    }

    /** 获取队列中等待的玩家数 */
    public int getQueueSize() {
        return queue.size();
    }

    // ---- 私有方法 ----

    private int getMatchRange(MatchRequest request) {
        long elapsed = System.currentTimeMillis() - request.getJoinTime();
        long elapsedSeconds = elapsed / 1000;
        if (elapsedSeconds > 30) return RELAX_RANGE_2;
        if (elapsedSeconds > 15) return RELAX_RANGE_1;
        return BASE_RANGE;
    }

    /** 定时检查：超时等待玩家放宽匹配范围 */
    private synchronized void checkQueue() {
        if (queue.isEmpty()) return;

        // 重新排列，让等待最久的玩家优先匹配
        // 但由于队列是ConcurrentLinkedQueue，我们直接遍历尝试重新匹配
        Iterator<MatchRequest> outer = queue.iterator();
        while (outer.hasNext()) {
            MatchRequest req = outer.next();
            long elapsed = System.currentTimeMillis() - req.getJoinTime();
            long elapsedSeconds = elapsed / 1000;
            if (elapsedSeconds > 30) {
                // 等待超过30秒，尝试用最宽范围匹配
                Iterator<MatchRequest> inner = queue.iterator();
                while (inner.hasNext()) {
                    MatchRequest candidate = inner.next();
                    if (candidate == req || candidate.getUserId().equals(req.getUserId())) continue;
                    if (Math.abs(candidate.getTrophies() - req.getTrophies()) <= RELAX_RANGE_2) {
                        // 匹配成功
                        outer.remove();
                        inner.remove();
                        pendingRequests.remove(candidate.getUserId());
                        pendingRequests.remove(req.getUserId());

                        log.info("Match found (relaxed): {} vs {}",
                                req.getNickname(), candidate.getNickname());

                        GameSession session = gameEngine.createGame(
                                req.getUserId(), candidate.getUserId(),
                                req.getNickname(), candidate.getNickname(),
                                req.getTrophies(), candidate.getTrophies());

                        MatchResult r1 = new MatchResult(session.getSessionId(), candidate.getUserId(), candidate.getNickname(), candidate.getTrophies());
                        MatchResult r2 = new MatchResult(session.getSessionId(), req.getUserId(), req.getNickname(), req.getTrophies());

                        req.getCallback().onMatched(r1);
                        candidate.getCallback().onMatched(r2);
                        return;
                    }
                }
            }
        }
    }
}
