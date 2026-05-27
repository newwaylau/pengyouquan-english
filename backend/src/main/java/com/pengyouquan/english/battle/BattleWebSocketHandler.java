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
 * 实时对战 WebSocket 消息处理器（纯卡牌策略操作，无答题）
 */
@Controller
public class BattleWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(BattleWebSocketHandler.class);

    private final SimpMessagingTemplate messaging;
    private final MatchmakingService matchmaking;
    private final GameEngine gameEngine;
    private final com.pengyouquan.english.repository.DeckRepository deckRepository;
    private final BotService botService;

    public BattleWebSocketHandler(SimpMessagingTemplate messaging,
                                  MatchmakingService matchmaking,
                                  GameEngine gameEngine,
                                  com.pengyouquan.english.repository.DeckRepository deckRepository,
                                  BotService botService) {
        this.messaging = messaging;
        this.matchmaking = matchmaking;
        this.gameEngine = gameEngine;
        this.deckRepository = deckRepository;
        this.botService = botService;
    }

    // ==================== 匹配 ====================

    @MessageMapping("/battle/join-queue")
    public void joinQueue(@Payload Map<String, Object> payload, SimpMessageHeaderAccessor headerAccessor) {
        Long userId = getUserId(headerAccessor);
        if (userId == null) {
            sendError(null, null, "未认证");
            return;
        }

        // 检查是否有牌组
        java.util.Optional<com.pengyouquan.english.model.Deck> activeDeck = deckRepository.findByUserIdAndIsActiveTrue(userId);
        if (activeDeck.isEmpty()) {
            boolean hasAnyDeck = !deckRepository.findByUserId(userId).isEmpty();
            if (!hasAnyDeck) {
                sendError(null, userId, "请先在牌组编辑器中保存一套20张的牌组");
                log.warn("User {} tried to queue without a deck", userId);
                return;
            }
            // 有牌组但没有激活的，自动激活第一个
            com.pengyouquan.english.model.Deck first = deckRepository.findByUserId(userId).get(0);
            first.setIsActive(true);
            deckRepository.save(first);
        }

        String nickname = getStringHeader(headerAccessor, "nickname");
        if (nickname == null) nickname = "Player " + userId;

        int trophies = 0;
        try {
            trophies = gameEngine.getTrophiesForUser(userId);
        } catch (Exception ignored) {}

        log.info("User {} ({}) joining match queue, trophies: {}", userId, nickname, trophies);

        matchmaking.joinQueue(userId, nickname, trophies, result -> {
            log.info("Match found for user {}, opponent: {}", userId, result.getOpponentName());

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

    /** 加入指定的对战会话（好友切磋） */
    @MessageMapping("/battle/join-session")
    public void joinSession(@Payload Map<String, Object> payload, SimpMessageHeaderAccessor headerAccessor) {
        Long userId = getUserId(headerAccessor);
        if (userId == null) { sendError(null, null, "未认证"); return; }

        String sessionId = getString(payload, "sessionId");
        if (sessionId == null) { sendError(null, userId, "缺少sessionId"); return; }

        GameSession session = gameEngine.getSession(sessionId);
        if (session == null) {
            sendError(sessionId, userId, "对战会话不存在或已结束");
            return;
        }

        if (!session.getPlayer1Id().equals(userId) && !session.getPlayer2Id().equals(userId)) {
            sendError(sessionId, userId, "无权加入此对战");
            return;
        }

        log.info("User {} joining session {}", userId, sessionId);
        sendGameStart(sessionId, userId);
    }

    // ==================== 出牌 ====================

    @MessageMapping("/battle/play-card")
    public void playCard(@Payload Map<String, Object> payload, SimpMessageHeaderAccessor headerAccessor) {
        Long userId = getUserId(headerAccessor);
        if (userId == null) { sendError(null, null, "未认证"); return; }

        String sessionId = getString(payload, "sessionId");
        Long cardId = getLong(payload, "cardId");
        Long targetId = payload.containsKey("targetId") ? getLong(payload, "targetId") : null;

        if (sessionId == null || cardId == null) {
            sendError(sessionId, userId, "缺少参数");
            return;
        }

        GameEngine.PlayCardResult pr = gameEngine.playCard(sessionId, userId, cardId, targetId);

        if (!pr.success) {
            sendError(sessionId, userId, pr.errorMessage);
            return;
        }

        // 通知出牌者
        BattleMessage.CardPlayResult playResult = new BattleMessage.CardPlayResult();
        playResult.cardId = cardId;
        playResult.success = true;
        playResult.playedCard = pr.card;
        playResult.manaRemaining = pr.manaRemaining;
        playResult.damageDealt = pr.damageDealt;

        messaging.convertAndSendToUser(
                userId.toString(),
                "/queue/card-result",
                new BattleMessage(BattleMessage.TYPE_CARD_PLAY_RESULT, sessionId, userId, playResult)
        );

        // 通知对手
        GameSession session = gameEngine.getSession(sessionId);
        if (session != null) {
            Long opponentId = session.getOpponentId(userId);
            BattleMessage.AttackDeclared declared = new BattleMessage.AttackDeclared();
            declared.attackerId = cardId;
            declared.attackerName = pr.card != null ? pr.card.getNameCn() : "";
            messaging.convertAndSendToUser(
                    opponentId.toString(),
                    "/queue/opponent-action",
                    new BattleMessage("OPPONENT_PLAY_CARD", sessionId, opponentId, declared)
            );
        }

        // 检查游戏结束
        if (pr.gameOver && session != null) {
            endGame(session);
            return;
        }

        // 广播当前状态
        broadcastGameState(sessionId);
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

        GameEngine.AttackResult ar = gameEngine.declareAttack(sessionId, userId, attackerCardId, targetType, targetId);

        if (!ar.success) {
            sendError(sessionId, userId, ar.errorMessage);
            return;
        }

        // 通知攻击方
        BattleMessage.AttackResult attackResult = new BattleMessage.AttackResult();
        attackResult.attackerId = attackerCardId;
        attackResult.defenderId = ar.defenderId;
        attackResult.damage = ar.damage;
        attackResult.defenderDead = ar.defenderDead;
        attackResult.defenderHealthLeft = ar.defenderHealthLeft;

        messaging.convertAndSendToUser(
                userId.toString(),
                "/queue/attack-result",
                new BattleMessage(BattleMessage.TYPE_ATTACK_RESULT, sessionId, userId, attackResult)
        );

        // 通知对手
        GameSession session = gameEngine.getSession(sessionId);
        if (session != null) {
            Long opponentId = session.getOpponentId(userId);
            messaging.convertAndSendToUser(
                    opponentId.toString(),
                    "/queue/defense-result",
                    new BattleMessage(BattleMessage.TYPE_ATTACK_RESULT, sessionId, opponentId, attackResult)
            );
        }

        // 检查游戏结束
        if (ar.gameOver && session != null) {
            endGame(session);
            return;
        }

        // 广播当前状态
        broadcastGameState(sessionId);
    }

    // ==================== 英雄/武器攻击 ====================

    @MessageMapping("/battle/hero-attack")
    public void heroAttack(@Payload Map<String, Object> payload, SimpMessageHeaderAccessor headerAccessor) {
        Long userId = getUserId(headerAccessor);
        if (userId == null) { sendError(null, null, "未认证"); return; }

        String sessionId = getString(payload, "sessionId");
        String targetType = getString(payload, "targetType");
        Long targetId = targetType != null && "minion".equals(targetType) ? getLong(payload, "targetId") : null;

        if (sessionId == null || targetType == null) {
            sendError(sessionId, userId, "缺少参数");
            return;
        }

        GameEngine.AttackResult ar = gameEngine.heroAttack(sessionId, userId, targetType, targetId);

        if (!ar.success) {
            sendError(sessionId, userId, ar.errorMessage);
            return;
        }

        // 通知攻击方
        BattleMessage.AttackResult attackResult = new BattleMessage.AttackResult();
        attackResult.attackerId = ar.attackerId;
        attackResult.defenderId = ar.defenderId;
        attackResult.damage = ar.damage;
        attackResult.defenderDead = ar.defenderDead;
        attackResult.defenderHealthLeft = ar.defenderHealthLeft;

        messaging.convertAndSendToUser(
                userId.toString(),
                "/queue/attack-result",
                new BattleMessage(BattleMessage.TYPE_ATTACK_RESULT, sessionId, userId, attackResult)
        );

        // 通知对手
        GameSession session = gameEngine.getSession(sessionId);
        if (session != null) {
            Long opponentId = session.getOpponentId(userId);
            messaging.convertAndSendToUser(
                    opponentId.toString(),
                    "/queue/defense-result",
                    new BattleMessage(BattleMessage.TYPE_ATTACK_RESULT, sessionId, opponentId, attackResult)
            );
        }

        if (ar.gameOver && session != null) {
            endGame(session);
            return;
        }

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

        GameEngine.EndTurnResult etr = gameEngine.endTurn(sessionId, userId);
        if (etr == null) { sendError(sessionId, userId, "无法结束回合"); return; }

        // 广播当前状态
        broadcastGameState(sessionId);

        // 如果下一个玩家是AI，触发BotService接管回合
        if (BotService.isBot(etr.nextPlayerId)) {
            Long humanId = session.getOpponentId(etr.nextPlayerId);
            Map<String, Object> opponentTurnNotice = Map.of(
                    "turnNumber", session.getTurnNumber() + 1,
                    "nextPlayerId", etr.nextPlayerId
            );
            messaging.convertAndSendToUser(
                    humanId.toString(),
                    "/queue/opponent-turn",
                    new BattleMessage("OPPONENT_TURN", sessionId, humanId, opponentTurnNotice)
            );
            botService.executeTurn(sessionId);
            return;
        }

        // 正常流程：通知当前回合的玩家
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

    // ==================== Mulligan 换牌 ====================

    @MessageMapping("/battle/mulligan")
    public void mulligan(@Payload Map<String, Object> payload, SimpMessageHeaderAccessor headerAccessor) {
        Long userId = getUserId(headerAccessor);
        if (userId == null) { sendError(null, null, "未认证"); return; }

        String sessionId = getString(payload, "sessionId");
        if (sessionId == null) { sendError(null, userId, "缺少sessionId"); return; }

        GameSession session = gameEngine.getSession(sessionId);
        if (session == null) { sendError(sessionId, userId, "游戏不存在"); return; }

        if (session.getPhase() != GamePhase.MULLIGAN) {
            sendError(sessionId, userId, "不在换牌阶段"); return;
        }

        // 解析要换掉的牌ID列表
        List<Long> cardIds = new java.util.ArrayList<>();
        Object rawIds = payload.get("cardIds");
        if (rawIds instanceof List) {
            for (Object id : (List<?>) rawIds) {
                if (id instanceof Number) cardIds.add(((Number) id).longValue());
            }
        }

        GameEngine.MulliganResult mr = gameEngine.processMulligan(sessionId, userId, cardIds);
        if (!mr.success) {
            sendError(sessionId, userId, mr.errorMessage);
            return;
        }

        // 如果双方未都提交且对手是AI，自动执行AI的换牌
        if (!mr.bothReady) {
            Long botId = BotService.isBot(session.getPlayer1Id()) ? session.getPlayer1Id()
                    : (BotService.isBot(session.getPlayer2Id()) ? session.getPlayer2Id() : null);
            if (botId != null && !session.getMulliganSubmitted().contains(botId)) {
                botService.performMulligan(sessionId);
                mr = gameEngine.processMulligan(sessionId, botId, null);
            }
        }

        // 通知换牌结果
        messaging.convertAndSendToUser(
                userId.toString(),
                "/queue/mulligan-result",
                new BattleMessage("MULLIGAN_RESULT", sessionId, userId, mr)
        );

        // 双方都提交后，通知开始游戏
        if (mr.bothReady) {
            // 通知双方游戏开始
            for (Long pid : List.of(session.getPlayer1Id(), session.getPlayer2Id())) {
                sendGameStart(sessionId, pid);
            }
            // 如果AI先手，触发AI回合
            if (BotService.isBot(session.getCurrentPlayerId())) {
                botService.executeTurn(sessionId);
            }
        }
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

        PlayerState me = session.getPlayerState(userId);
        PlayerState opponent = session.getOpponent(userId);

        if (me == null || opponent == null) return;

        // 如果处于 Mulligan 阶段，发送 MulliganStart
        if (session.getPhase() == GamePhase.MULLIGAN) {
            BattleMessage.MulliganStart mulliganMsg = new BattleMessage.MulliganStart();
            mulliganMsg.hand = me.getHand();
            mulliganMsg.goingFirst = me.isGoingFirst();

            messaging.convertAndSendToUser(
                    userId.toString(),
                    "/queue/mulligan-start",
                    new BattleMessage(BattleMessage.TYPE_MULLIGAN_START, sessionId, userId, mulliganMsg)
            );
            return;
        }

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

    private void broadcastGameState(String sessionId) {
        GameSession session = gameEngine.getSession(sessionId);
        if (session == null) return;

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
            state.put("myWeapon", me.getWeapon());
            state.put("opponentWeapon", opponent.getWeapon());
            state.put("mySecretsCount", me.getSecrets() != null ? me.getSecrets().size() : 0);
            state.put("opponentSecretsCount", opponent.getSecrets() != null ? opponent.getSecrets().size() : 0);
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
            return;
        }

        Long loserId = session.getOpponentId(winnerId);
        PlayerState winner = session.getPlayerState(winnerId);
        PlayerState loser = session.getOpponent(winnerId);

        boolean isBotGame = BotService.isBot(winnerId) || BotService.isBot(loserId);

        for (Long playerId : List.of(session.getPlayer1Id(), session.getPlayer2Id())) {
            // 跳过AI玩家的通知
            if (BotService.isBot(playerId)) continue;

            boolean isWinner = playerId.equals(winnerId);
            BattleMessage.GameOver gameOver = new BattleMessage.GameOver();
            gameOver.winnerId = winnerId;
            gameOver.loserId = loserId;
            gameOver.result = isWinner ? "win" : "lose";

            PlayerState me = session.getPlayerState(playerId);

            gameOver.trophyChange = isBotGame ? 0 : (isWinner ? 30 : -25);

            gameOver.winnerStats = new BattleMessage.PlayerFinalStats();
            gameOver.winnerStats.userId = winnerId;
            gameOver.winnerStats.nickname = winner != null ? winner.getNickname() : "";
            gameOver.winnerStats.healthRemaining = winner != null ? winner.getHealth() : 0;
            gameOver.winnerStats.cardsPlayed = 0;
            gameOver.winnerStats.accuracy = 0;

            gameOver.loserStats = new BattleMessage.PlayerFinalStats();
            gameOver.loserStats.userId = loserId;
            gameOver.loserStats.nickname = loser != null ? loser.getNickname() : "";
            gameOver.loserStats.healthRemaining = loser != null ? loser.getHealth() : 0;
            gameOver.loserStats.cardsPlayed = 0;
            gameOver.loserStats.accuracy = 0;

            messaging.convertAndSendToUser(
                    playerId.toString(),
                    "/queue/game-over",
                    new BattleMessage(BattleMessage.TYPE_GAME_OVER, session.getSessionId(), playerId, gameOver)
            );
        }
    }

    private GameSession findSessionByPlayer(Long userId) {
        for (GameSession session : gameEngine.getActiveGames().values()) {
            if ((session.getPhase() == GamePhase.PLAYING || session.getPhase() == GamePhase.MULLIGAN) &&
                    (session.getPlayer1Id().equals(userId) || session.getPlayer2Id().equals(userId))) {
                return session;
            }
        }
        return null;
    }
}
