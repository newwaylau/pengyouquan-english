package com.pengyouquan.english.battle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * 实时对战 WebSocket 消息处理器
 */
@Controller
public class BattleWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(BattleWebSocketHandler.class);

    private final SimpMessagingTemplate messaging;
    private final MatchmakingService matchmaking;
    private final GameEngine gameEngine;

    public BattleWebSocketHandler(SimpMessagingTemplate messaging,
                                  MatchmakingService matchmaking,
                                  GameEngine gameEngine) {
        this.messaging = messaging;
        this.matchmaking = matchmaking;
        this.gameEngine = gameEngine;
    }

    // ==================== 匹配 ====================

    @MessageMapping("/battle/join-queue")
    public void joinQueue(@Payload Map<String, Object> payload, SimpMessageHeaderAccessor headerAccessor) {
        Long userId = getUserId(headerAccessor);
        if (userId == null) {
            sendError(null, null, "未认证");
            return;
        }

        String nickname = getStringHeader(headerAccessor, "nickname");
        if (nickname == null) nickname = "Player " + userId;

        // 获取奖杯数
        int trophies = 0;
        try {
            trophies = gameEngine.getTrophiesForUser(userId);
        } catch (Exception ignored) {}

        log.info("User {} ({}) joining match queue, trophies: {}", userId, nickname, trophies);

        matchmaking.joinQueue(userId, nickname, trophies, result -> {
            // 匹配成功回调 - 通知双方
            log.info("Match found for user {}, opponent: {}", userId, result.getOpponentName());

            // 通知当前玩家
            BattleMessage.MatchFound matchFound = new BattleMessage.MatchFound(
                    result.getSessionId(),
                    result.getOpponentId(),
                    result.getOpponentName(),
                    result.getOpponentTrophies()
            );

            messaging.convertAndSendToUser(
                    userId.toString(),
                    "/queue/match-found",
                    new BattleMessage(BattleMessage.TYPE_MATCH_FOUND, result.getSessionId(), userId, matchFound)
            );

            // 通知两个玩家游戏开始（由匹配回调的另一端触发，但我们在两个回调里都发GAME_START）
            sendGameStart(result.getSessionId(), userId);
        });
    }

    @MessageMapping("/battle/cancel-queue")
    public void cancelQueue(SimpMessageHeaderAccessor headerAccessor) {
        Long userId = getUserId(headerAccessor);
        if (userId == null) return;
        matchmaking.cancelMatch(userId);
        log.info("User {} cancelled matchmaking", userId);
    }

    // ==================== 出牌 ====================

    @MessageMapping("/battle/play-card")
    public void playCard(@Payload Map<String, Object> payload, SimpMessageHeaderAccessor headerAccessor) {
        Long userId = getUserId(headerAccessor);
        if (userId == null) { sendError(null, null, "未认证"); return; }

        String sessionId = getString(payload, "sessionId");
        Long cardId = getLong(payload, "cardId");
        if (sessionId == null || cardId == null) {
            sendError(sessionId, userId, "缺少参数");
            return;
        }

        GameEngine.QuestionResult qr = gameEngine.playCard(sessionId, userId, cardId);

        if (!qr.success) {
            sendError(sessionId, userId, qr.errorMessage);
            return;
        }

        // 存储pending动作
        gameEngine.setPendingAction(sessionId, userId,
                new GameEngine.PendingAction(qr.card));

        // 给对手发送出牌通知
        GameSession session = gameEngine.getSession(sessionId);
        if (session != null) {
            Long opponentId = session.getOpponentId(userId);
            BattleMessage.AttackDeclared declared = new BattleMessage.AttackDeclared();
            declared.attackerId = cardId;
            declared.attackerName = qr.card != null ? qr.card.getNameCn() : "";
            messaging.convertAndSendToUser(
                    opponentId.toString(),
                    "/queue/opponent-action",
                    new BattleMessage("OPPONENT_PLAY_CARD", sessionId, opponentId, declared)
            );
        }

        // 非法术牌：发送考题
        if (qr.card != null && !"spell".equals(qr.card.getCardType()) && qr.questionData != null) {
            BattleMessage.Question question = new BattleMessage.Question();
            question.cardId = qr.card.getCardId();
            question.cardNameCn = qr.card.getNameCn();
            question.questionType = qr.questionType;
            question.questionData = qr.questionData;
            question.timeLimit = qr.timeLimit;

            messaging.convertAndSendToUser(
                    userId.toString(),
                    "/queue/question",
                    new BattleMessage(BattleMessage.TYPE_QUESTION, sessionId, userId, question)
            );
        } else {
            // 法术牌直接成功
            BattleMessage.CardPlayResult playResult = new BattleMessage.CardPlayResult();
            playResult.cardId = cardId;
            playResult.success = true;
            playResult.playedCard = qr.card;
            playResult.manaRemaining = qr.manaRemaining;

            messaging.convertAndSendToUser(
                    userId.toString(),
                    "/queue/card-result",
                    new BattleMessage(BattleMessage.TYPE_CARD_PLAY_RESULT, sessionId, userId, playResult)
            );

            // 广播给全房间
            broadcastGameState(sessionId);
        }
    }

    // ==================== 答题 ====================

    @MessageMapping("/battle/submit-answer")
    public void submitAnswer(@Payload Map<String, Object> payload, SimpMessageHeaderAccessor headerAccessor) {
        Long userId = getUserId(headerAccessor);
        if (userId == null) { sendError(null, null, "未认证"); return; }

        String sessionId = getString(payload, "sessionId");
        boolean correct = getBoolean(payload, "correct");

        if (sessionId == null) {
            sendError(null, userId, "缺少sessionId");
            return;
        }

        GameEngine.AnswerResult ar = gameEngine.submitAnswer(sessionId, userId, correct);

        BattleMessage.CardPlayResult playResult = new BattleMessage.CardPlayResult();
        playResult.cardId = ar.card != null ? ar.card.getCardId() : null;
        playResult.success = ar.success;
        playResult.playedCard = ar.card;
        playResult.manaRemaining = ar.manaRemaining;
        playResult.damageDealt = ar.damageDealt;

        if (!ar.success) {
            playResult.errorMessage = ar.errorMessage;
        }

        messaging.convertAndSendToUser(
                userId.toString(),
                "/queue/card-result",
                new BattleMessage(BattleMessage.TYPE_CARD_PLAY_RESULT, sessionId, userId, playResult)
        );

        // 连击奖励
        if (ar.comboCount > 0 && ar.comboCount % 3 == 0) {
            BattleMessage.ComboBonus comboBonus = new BattleMessage.ComboBonus();
            comboBonus.comboCount = ar.comboCount;
            comboBonus.bonus = "下一张卡费用-1";
            messaging.convertAndSendToUser(
                    userId.toString(),
                    "/queue/combo",
                    new BattleMessage(BattleMessage.TYPE_COMBO_BONUS, sessionId, userId, comboBonus)
            );
        }

        // 检查游戏结束
        if (ar.gameOver) {
            GameSession session = gameEngine.getSession(sessionId);
            if (session != null) {
                endGame(session);
            }
            return;
        }

        // 广播当前状态
        broadcastGameState(sessionId);

        // 如果是对手的答对补偿，通知对手
        if (ar.opponentGainedCard) {
            GameSession session = gameEngine.getSession(sessionId);
            if (session != null) {
                Long opponentId = session.getOpponentId(userId);
                messaging.convertAndSendToUser(
                        opponentId.toString(),
                        "/queue/opponent-action",
                        new BattleMessage("COMPENSATION", sessionId, opponentId,
                                Map.of("gainedCard", true, "healed", ar.opponentHealed))
                );
            }
        }
    }

    // ==================== 攻击 ====================

    @MessageMapping("/battle/attack")
    public void attack(@Payload Map<String, Object> payload, SimpMessageHeaderAccessor headerAccessor) {
        Long userId = getUserId(headerAccessor);
        if (userId == null) { sendError(null, null, "未认证"); return; }

        String sessionId = getString(payload, "sessionId");
        Long attackerCardId = getLong(payload, "attackerCardId");
        String targetType = getString(payload, "targetType");
        Long targetId = targetType != null && "minion".equals(targetType) ? getLong(payload, "targetId") : null;

        if (sessionId == null || attackerCardId == null || targetType == null) {
            sendError(sessionId, userId, "缺少参数");
            return;
        }

        GameEngine.AttackDeclarationResult adr = gameEngine.declareAttack(sessionId, userId, attackerCardId, targetType, targetId);

        if (!adr.success) {
            sendError(sessionId, userId, adr.errorMessage);
            return;
        }

        // 通知攻击方：攻击已发起
        BattleMessage.AttackResult attackNotice = new BattleMessage.AttackResult();
        attackNotice.attackerId = attackerCardId;
        attackNotice.defenderId = targetId;
        messaging.convertAndSendToUser(
                userId.toString(),
                "/queue/attack-declared",
                new BattleMessage(BattleMessage.TYPE_ATTACK_DECLARED, sessionId, userId, attackNotice)
        );

        // 通知防御方：需要答题防御
        BattleMessage.DefenseQuestion defenseQ = new BattleMessage.DefenseQuestion();
        defenseQ.attackerId = attackerCardId;
        defenseQ.attackerName = adr.attackerName;
        defenseQ.attackPower = adr.attackerAttack;
        defenseQ.targetType = targetType;
        defenseQ.targetId = targetId;
        BattleMessage.Question defQ = new BattleMessage.Question();
        defQ.questionData = adr.defenseQuestionData;
        defQ.questionType = "listening";
        defQ.timeLimit = 30;
        defenseQ.question = defQ;

        GameSession session = gameEngine.getSession(sessionId);
        if (session != null) {
            Long opponentId = session.getOpponentId(userId);
            messaging.convertAndSendToUser(
                    opponentId.toString(),
                    "/queue/defense-question",
                    new BattleMessage(BattleMessage.TYPE_DEFENSE_QUESTION, sessionId, opponentId, defenseQ)
            );
        }
    }

    // ==================== 防御 ====================

    @MessageMapping("/battle/submit-defense")
    public void submitDefense(@Payload Map<String, Object> payload, SimpMessageHeaderAccessor headerAccessor) {
        Long userId = getUserId(headerAccessor);
        if (userId == null) { sendError(null, null, "未认证"); return; }

        String sessionId = getString(payload, "sessionId");
        boolean correct = getBoolean(payload, "correct");

        if (sessionId == null) {
            sendError(null, userId, "缺少sessionId");
            return;
        }

        GameEngine.DefenseResult dr = gameEngine.submitDefense(sessionId, userId, correct);

        if (!dr.success) {
            sendError(sessionId, userId, dr.errorMessage);
            return;
        }

        // 通知攻击方
        BattleMessage.AttackResult attackResult = new BattleMessage.AttackResult();
        attackResult.attackerId = dr.attackerId;
        attackResult.defenderId = dr.defenderId;
        attackResult.damage = dr.damage;
        attackResult.defenderDead = dr.defenderDead;
        attackResult.defenderHealthLeft = dr.defenderHealthLeft;
        attackResult.defenderCorrect = dr.defenderCorrect;

        GameSession session = gameEngine.getSession(sessionId);
        if (session != null) {
            Long attackerId = session.getOpponentId(userId);
            messaging.convertAndSendToUser(
                    attackerId.toString(),
                    "/queue/attack-result",
                    new BattleMessage(BattleMessage.TYPE_ATTACK_RESULT, sessionId, attackerId, attackResult)
            );

            // 通知防御方
            messaging.convertAndSendToUser(
                    userId.toString(),
                    "/queue/defense-result",
                    new BattleMessage(BattleMessage.TYPE_ATTACK_RESULT, sessionId, userId, attackResult)
            );
        }

        // 检查游戏结束
        if (dr.gameOver && session != null) {
            endGame(session);
            return;
        }

        // 广播当前状态
        broadcastGameState(sessionId);
    }

    // ==================== 回合控制 ====================

    @MessageMapping("/battle/end-turn")
    public void endTurn(SimpMessageHeaderAccessor headerAccessor) {
        Long userId = getUserId(headerAccessor);
        if (userId == null) { sendError(null, null, "未认证"); return; }

        GameSession session = findSessionByPlayer(userId);
        if (session == null) { sendError(null, userId, "没有活跃的对战"); return; }

        String sessionId = session.getSessionId();

        // 获取下一回合的数据
        GameEngine.EndTurnResult etr = gameEngine.endTurn(sessionId, userId);
        if (etr == null) { sendError(sessionId, userId, "无法结束回合"); return; }

        // 广播当前状态
        broadcastGameState(sessionId);

        // 通知当前回合的玩家
        GameEngine.TurnStartResult tsr = gameEngine.startTurn(sessionId);
        if (tsr != null) {
            BattleMessage.TurnStart turnStart = new BattleMessage.TurnStart();
            turnStart.turnNumber = tsr.turnNumber;
            turnStart.mana = tsr.mana;
            turnStart.maxMana = tsr.maxMana;
            turnStart.hand = tsr.hand;
            turnStart.drawnCard = tsr.drawnCard;

            messaging.convertAndSendToUser(
                    etr.nextPlayerId.toString(),
                    "/queue/turn-start",
                    new BattleMessage(BattleMessage.TYPE_TURN_START, sessionId, etr.nextPlayerId, turnStart)
            );

            // 通知对手（不暴露手牌细节）
            Long opponentId = session.getOpponentId(etr.nextPlayerId);
            Map<String, Object> opponentTurnNotice = Map.of(
                    "turnNumber", tsr.turnNumber,
                    "nextPlayerId", etr.nextPlayerId,
                    "opponentHandCount", tsr.hand != null ? tsr.hand.size() : 0
            );
            messaging.convertAndSendToUser(
                    opponentId.toString(),
                    "/queue/opponent-turn",
                    new BattleMessage("OPPONENT_TURN", sessionId, opponentId, opponentTurnNotice)
            );
        }
    }

    // ==================== 认输 ====================

    @MessageMapping("/battle/concede")
    public void concede(SimpMessageHeaderAccessor headerAccessor) {
        Long userId = getUserId(headerAccessor);
        if (userId == null) { sendError(null, null, "未认证"); return; }

        GameSession session = findSessionByPlayer(userId);
        if (session == null) { sendError(null, userId, "没有活跃的对战"); return; }

        gameEngine.concede(session.getSessionId(), userId);
        endGame(session);
    }

    // ==================== 私有方法 ====================

    private Long getUserId(SimpMessageHeaderAccessor headerAccessor) {
        Principal principal = headerAccessor.getUser();
        if (principal != null) {
            try {
                return Long.parseLong(principal.getName());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        // 从session attributes中获取
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        if (sessionAttributes != null) {
            Object userIdAttr = sessionAttributes.get("userId");
            if (userIdAttr instanceof Long) return (Long) userIdAttr;
            if (userIdAttr instanceof String) {
                try { return Long.parseLong((String) userIdAttr); } catch (NumberFormatException ignored) {}
            }
        }
        return null;
    }

    private String getStringHeader(SimpMessageHeaderAccessor headerAccessor, String key) {
        Map<String, List<String>> nativeHeaders = (Map<String, List<String>>) headerAccessor.getHeader("nativeHeaders");
        if (nativeHeaders != null && nativeHeaders.containsKey(key)) {
            List<String> values = nativeHeaders.get(key);
            if (values != null && !values.isEmpty()) return values.get(0);
        }
        return null;
    }

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val instanceof String ? (String) val : null;
    }

    private Long getLong(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).longValue();
        if (val instanceof String) {
            try { return Long.parseLong((String) val); } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private boolean getBoolean(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Boolean) return (Boolean) val;
        if (val instanceof String) return "true".equalsIgnoreCase((String) val);
        return false;
    }

    private void sendError(String sessionId, Long userId, String message) {
        BattleMessage.ErrorMessage error = new BattleMessage.ErrorMessage("ERROR", message);
        if (userId != null && sessionId != null) {
            messaging.convertAndSendToUser(
                    userId.toString(),
                    "/queue/errors",
                    new BattleMessage(BattleMessage.TYPE_ERROR, sessionId, userId, error)
            );
        }
    }

    private void sendGameStart(String sessionId, Long userId) {
        GameSession session = gameEngine.getSession(sessionId);
        if (session == null) return;

        // 发给自己
        PlayerState me = session.getPlayerState(userId);
        PlayerState opponent = session.getOpponent(userId);

        if (me == null || opponent == null) return;

        BattleMessage.GameStart myStart = new BattleMessage.GameStart();
        myStart.sessionId = sessionId;
        myStart.you = new BattleMessage.PlayerInfo(
                me.getUserId(), me.getNickname(), me.getHealth(),
                me.getTrophies(), me.getDeck() != null ? me.getDeck().size() : 0);
        myStart.opponent = new BattleMessage.PlayerInfo(
                opponent.getUserId(), opponent.getNickname(), opponent.getHealth(),
                opponent.getTrophies(), opponent.getDeck() != null ? opponent.getDeck().size() : 0);
        myStart.hand = me.getHand();
        myStart.startingMana = me.getMana();
        myStart.goingFirst = session.getCurrentPlayerId().equals(userId);

        messaging.convertAndSendToUser(
                userId.toString(),
                "/queue/game-start",
                new BattleMessage(BattleMessage.TYPE_GAME_START, sessionId, userId, myStart)
        );

        // 如果是先手，发送回合开始
        if (myStart.goingFirst) {
            BattleMessage.TurnStart turnStart = new BattleMessage.TurnStart();
            turnStart.turnNumber = 0;
            turnStart.mana = me.getMana();
            turnStart.maxMana = me.getMaxMana();
            turnStart.hand = me.getHand();
            messaging.convertAndSendToUser(
                    userId.toString(),
                    "/queue/turn-start",
                    new BattleMessage(BattleMessage.TYPE_TURN_START, sessionId, userId, turnStart)
            );
        }
    }

    private void sendGameStartToOpponent(String sessionId, Long userId) {
        // 由匹配回调的另一端触发
        GameSession session = gameEngine.getSession(sessionId);
        if (session == null) return;
        Long opponentId = session.getOpponentId(userId);
        sendGameStart(sessionId, opponentId);
    }

    private void broadcastGameState(String sessionId) {
        GameSession session = gameEngine.getSession(sessionId);
        if (session == null) return;

        // 发送精简版面状态给每个玩家
        for (Long playerId : List.of(session.getPlayer1Id(), session.getPlayer2Id())) {
            PlayerState me = session.getPlayerState(playerId);
            PlayerState opponent = session.getOpponent(playerId);
            if (me == null || opponent == null) continue;

            Map<String, Object> state = new java.util.HashMap<>();
            state.put("myHealth", me.getHealth());
            state.put("myMana", me.getMana());
            state.put("myMaxMana", me.getMaxMana());
            state.put("myBoard", me.getBoard() != null ? me.getBoard() : java.util.List.of());
            state.put("myDeckCount", me.getDeck() != null ? me.getDeck().size() : 0);
            state.put("myHandCount", me.getHand() != null ? me.getHand().size() : 0);
            state.put("opponentHealth", opponent.getHealth());
            state.put("opponentBoard", opponent.getBoard() != null ? opponent.getBoard() : java.util.List.of());
            state.put("opponentHandCount", opponent.getHand() != null ? opponent.getHand().size() : 0);
            state.put("opponentDeckCount", opponent.getDeck() != null ? opponent.getDeck().size() : 0);
            state.put("turnNumber", session.getTurnNumber());
            state.put("currentPlayerId", session.getCurrentPlayerId());
            state.put("isMyTurn", session.getCurrentPlayerId().equals(playerId));

            messaging.convertAndSendToUser(
                    playerId.toString(),
                    "/queue/game-state",
                    new BattleMessage("GAME_STATE", sessionId, playerId, state)
            );
        }
    }

    private void endGame(GameSession session) {
        Long winnerId = session.getWinnerId();
        if (winnerId == null) {
            // 如果还没设置winner，用concede逻辑
            return;
        }

        Long loserId = session.getOpponentId(winnerId);
        PlayerState winner = session.getPlayerState(winnerId);
        PlayerState loser = session.getOpponent(winnerId);

        for (Long playerId : List.of(session.getPlayer1Id(), session.getPlayer2Id())) {
            boolean isWinner = playerId.equals(winnerId);
            BattleMessage.GameOver gameOver = new BattleMessage.GameOver();
            gameOver.winnerId = winnerId;
            gameOver.loserId = loserId;
            gameOver.result = isWinner ? "win" : "lose";

            PlayerState me = session.getPlayerState(playerId);

            gameOver.trophyChange = isWinner ? 30 : -25;

            gameOver.winnerStats = new BattleMessage.PlayerFinalStats();
            gameOver.winnerStats.userId = winnerId;
            gameOver.winnerStats.nickname = winner != null ? winner.getNickname() : "";
            gameOver.winnerStats.healthRemaining = winner != null ? winner.getHealth() : 0;
            gameOver.winnerStats.cardsPlayed = winner != null ? winner.getAnsweredCorrectly() : 0;
            gameOver.winnerStats.accuracy = me != null && me.getAnsweredTotal() > 0
                    ? me.getAnsweredCorrectly() * 100 / me.getAnsweredTotal() : 0;

            gameOver.loserStats = new BattleMessage.PlayerFinalStats();
            gameOver.loserStats.userId = loserId;
            gameOver.loserStats.nickname = loser != null ? loser.getNickname() : "";
            gameOver.loserStats.healthRemaining = loser != null ? loser.getHealth() : 0;
            gameOver.loserStats.cardsPlayed = loser != null ? loser.getAnsweredCorrectly() : 0;
            gameOver.loserStats.accuracy = loser != null && loser.getAnsweredTotal() > 0
                    ? loser.getAnsweredCorrectly() * 100 / loser.getAnsweredTotal() : 0;

            messaging.convertAndSendToUser(
                    playerId.toString(),
                    "/queue/game-over",
                    new BattleMessage(BattleMessage.TYPE_GAME_OVER, session.getSessionId(), playerId, gameOver)
            );
        }
    }

    private BattleMessage.DefenseQuestion createDefenseQuestion(GameEngine.AttackDeclarationResult adr) {
        BattleMessage.DefenseQuestion dq = new BattleMessage.DefenseQuestion();
        dq.attackerId = adr.attackerId;
        dq.attackerName = adr.attackerName;
        dq.attackPower = adr.attackerAttack;
        dq.targetType = adr.targetType;
        dq.targetId = adr.targetId;

        BattleMessage.Question q = new BattleMessage.Question();
        q.questionType = "defense";
        q.questionData = adr.defenseQuestionData;
        q.timeLimit = 30;
        dq.question = q;

        return dq;
    }

    private GameSession findSessionByPlayer(Long userId) {
        for (GameSession session : gameEngine.getActiveGames().values()) {
            if (session.getPhase() == GamePhase.PLAYING &&
                    (session.getPlayer1Id().equals(userId) || session.getPlayer2Id().equals(userId))) {
                return session;
            }
        }
        return null;
    }
}
