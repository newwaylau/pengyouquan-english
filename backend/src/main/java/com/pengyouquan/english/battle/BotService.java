package com.pengyouquan.english.battle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AI机器人对手。提供换牌、出牌、攻击、结束回合等全自动决策。
 */
@Service
public class BotService {

    private static final Logger log = LoggerFactory.getLogger(BotService.class);
    private static final Random RANDOM = new Random();

    private static final String[] BOT_NAMES = {
            "泰兰德", "阿尔萨斯", "吉安娜", "萨尔",
            "乌瑟尔", "古尔丹", "玛法里奥", "伊利丹"
    };

    public static final long BOT_USER_ID = -1L;

    private final GameEngine gameEngine;
    private final SimpMessagingTemplate messaging;

    public BotService(GameEngine gameEngine, SimpMessagingTemplate messaging) {
        this.gameEngine = gameEngine;
        this.messaging = messaging;
    }

    /** 返回一个随机AI昵称 */
    public String getRandomBotName() {
        return "Bot_" + BOT_NAMES[RANDOM.nextInt(BOT_NAMES.length)];
    }

    // ==================== Mulligan ====================

    /**
     * AI换牌：总是换掉最高费的1-3张牌
     */
    public void performMulligan(String sessionId) {
        GameSession session = gameEngine.getSession(sessionId);
        if (session == null) return;

        PlayerState bot = session.getPlayerState(BOT_USER_ID);
        if (bot == null || bot.getHand() == null || bot.getHand().isEmpty()) return;

        // 按费用从高到低排序
        List<CardState> sorted = new ArrayList<>(bot.getHand());
        sorted.sort((a, b) -> Integer.compare(b.getCost(), a.getCost()));

        // 随机换1-3张最高费牌
        int numToSwap = Math.min(RANDOM.nextInt(3) + 1, sorted.size());
        List<Long> swapCardIds = new ArrayList<>();
        for (int i = 0; i < numToSwap; i++) {
            swapCardIds.add(sorted.get(i).getCardId());
        }

        log.info("[Bot] Mulligan: swapping {} cards (highest cost)", numToSwap);
        gameEngine.processMulligan(sessionId, BOT_USER_ID, swapCardIds);
    }

    // ==================== 回合执行 ====================

    /**
     * AI执行完整回合。先调用startTurn，再在后台线程中依次执行出牌、攻击、结束回合。
     */
    public void executeTurn(String sessionId) {
        GameSession session = gameEngine.getSession(sessionId);
        if (session == null) return;
        if (!session.getCurrentPlayerId().equals(BOT_USER_ID)) return;

        // 开始AI的回合
        GameEngine.TurnStartResult tsr = gameEngine.startTurn(sessionId);
        if (tsr == null) return;

        log.info("[Bot] Turn {} started, mana: {}/{}, hand: {} cards",
                tsr.turnNumber, tsr.mana, tsr.maxMana,
                botHandSize(sessionId));

        // 广播给人类玩家
        broadcastGameStateToHuman(sessionId);

        // 在后台线程中执行AI操作（含延迟模拟）
        new Thread(() -> {
            try {
                executeAITurnActions(sessionId);
            } catch (Exception e) {
                log.error("[Bot] Error during AI turn for session {}: {}", sessionId, e.getMessage(), e);
            }
        }, "bot-turn-" + sessionId).start();
    }

    // ==================== AI 决策私有方法 ====================

    private void executeAITurnActions(String sessionId) {
        // 模拟思考延迟
        sleepRandom(1000, 2000);

        GameSession session = gameEngine.getSession(sessionId);
        if (session == null || session.getPhase() != GamePhase.PLAYING) return;
        if (!session.getCurrentPlayerId().equals(BOT_USER_ID)) return;

        // 1. 出牌
        playCardsPhase(sessionId);

        // 2. 随从攻击
        session = gameEngine.getSession(sessionId);
        if (session == null || session.getPhase() != GamePhase.PLAYING) return;
        attackPhase(sessionId);

        // 3. 英雄武器攻击
        session = gameEngine.getSession(sessionId);
        if (session == null || session.getPhase() != GamePhase.PLAYING) return;
        heroWeaponAttackPhase(sessionId);

        // 4. 结束回合
        session = gameEngine.getSession(sessionId);
        if (session == null || session.getPhase() != GamePhase.PLAYING) return;
        if (!session.getCurrentPlayerId().equals(BOT_USER_ID)) return;

        endBotTurn(sessionId);
    }

    // ==================== 出牌 ====================

    private void playCardsPhase(String sessionId) {
        boolean played = true;
        int maxIterations = 20; // 防止无限循环
        int iterations = 0;

        while (played && iterations < maxIterations) {
            iterations++;
            played = false;

            GameSession session = gameEngine.getSession(sessionId);
            if (session == null || session.getPhase() != GamePhase.PLAYING) break;
            if (!session.getCurrentPlayerId().equals(BOT_USER_ID)) break;

            PlayerState bot = session.getPlayerState(BOT_USER_ID);
            if (bot == null || bot.getHand() == null || bot.getHand().isEmpty()) break;

            // 选出费用≤当前法力的可出牌
            final int mana = bot.getMana();
            List<CardState> playable = bot.getHand().stream()
                    .filter(c -> c.getCost() <= mana)
                    .collect(Collectors.toList());
            if (playable.isEmpty()) break;

            // 按优先级选牌：优先高费，同费则随从>武器>法术>奥秘
            CardState toPlay = selectBestCardToPlay(playable);
            if (toPlay == null) break;

            // 确定目标（法术/奥秘指向敌方英雄）
            Long targetId = determineTarget(session, toPlay);

            log.info("[Bot] Playing card: {} (cost:{}, type:{})",
                    toPlay.getNameCn(), toPlay.getCost(), toPlay.getCardType());

            GameEngine.PlayCardResult result = gameEngine.playCard(
                    sessionId, BOT_USER_ID, toPlay.getCardId(), targetId);

            if (result.success) {
                // 广播给人类玩家看
                broadcastOpponentCardPlay(sessionId, toPlay);
                broadcastGameStateToHuman(sessionId);
                sleepRandom(1000, 1500);
                played = true;

                // 检查是否杀死对手
                if (result.gameOver) {
                    handleAIGameOver(sessionId);
                    return;
                }
            } else {
                log.warn("[Bot] Failed to play card {}: {}", toPlay.getNameCn(), result.errorMessage);
                break;
            }
        }
    }

    private CardState selectBestCardToPlay(List<CardState> playable) {
        // 费用降序，同费按类型优先级
        playable.sort((a, b) -> {
            int costCmp = Integer.compare(b.getCost(), a.getCost());
            if (costCmp != 0) return costCmp;
            return Integer.compare(getTypePriority(a.getCardType()), getTypePriority(b.getCardType()));
        });

        int maxCost = playable.get(0).getCost();
        List<CardState> topCost = playable.stream()
                .filter(c -> c.getCost() == maxCost)
                .collect(Collectors.toList());
        return topCost.get(RANDOM.nextInt(topCost.size()));
    }

    private int getTypePriority(String cardType) {
        switch (cardType != null ? cardType : "") {
            case "minion":  return 0;
            case "weapon":  return 1;
            case "spell":   return 2;
            case "secret":  return 3;
            default:        return 4;
        }
    }

    private Long determineTarget(GameSession session, CardState card) {
        String type = card.getCardType();
        if ("spell".equals(type) || "secret".equals(type)) {
            // 法术/奥秘自动指向敌方英雄
            PlayerState opponent = session.getOpponent(BOT_USER_ID);
            if (opponent != null) return opponent.getUserId();
        }
        return null;
    }

    // ==================== 随从攻击 ====================

    private void attackPhase(String sessionId) {
        boolean attacked = true;
        int maxIterations = 20;

        for (int i = 0; i < maxIterations && attacked; i++) {
            attacked = false;

            GameSession session = gameEngine.getSession(sessionId);
            if (session == null || session.getPhase() != GamePhase.PLAYING) break;
            if (!session.getCurrentPlayerId().equals(BOT_USER_ID)) break;

            PlayerState bot = session.getPlayerState(BOT_USER_ID);
            PlayerState opponent = session.getOpponent(BOT_USER_ID);
            if (bot == null || opponent == null) break;
            if (bot.getBoard() == null || bot.getBoard().isEmpty()) break;

            // 找出可攻击的随从
            List<CardState> attackers = bot.getBoard().stream()
                    .filter(CardState::isCanAttack)
                    .collect(Collectors.toList());
            if (attackers.isEmpty()) break;

            // 优先用高攻随从攻击
            CardState attacker = attackers.stream()
                    .max(Comparator.comparingInt(CardState::getAttack))
                    .orElse(attackers.get(0));

            AttackTarget target = chooseAttackTarget(opponent, attacker);
            if (target == null) break;

            log.info("[Bot] Attacking: {} (atk:{}) -> {}",
                    attacker.getNameCn(), attacker.getAttack(), target.description);

            GameEngine.AttackResult result;
            if (target.isHero) {
                result = gameEngine.declareAttack(sessionId, BOT_USER_ID,
                        attacker.getCardId(), "hero", null);
            } else {
                result = gameEngine.declareAttack(sessionId, BOT_USER_ID,
                        attacker.getCardId(), "minion", target.targetId);
            }

            if (result.success) {
                broadcastAttackResultToHuman(sessionId, result);
                broadcastGameStateToHuman(sessionId);
                sleepRandom(1000, 1500);
                attacked = true;

                if (result.gameOver) {
                    handleAIGameOver(sessionId);
                    return;
                }
            } else {
                log.warn("[Bot] Attack failed: {}", result.errorMessage);
                break;
            }
        }
    }

    private AttackTarget chooseAttackTarget(PlayerState opponent, CardState attacker) {
        List<CardState> opponentBoard = opponent.getBoard();
        if (opponentBoard == null || opponentBoard.isEmpty()) {
            return new AttackTarget(true, null, "敌方英雄");
        }

        // 优先攻击嘲讽随从
        List<CardState> taunts = opponentBoard.stream()
                .filter(CardState::isHasTaunt)
                .collect(Collectors.toList());
        if (!taunts.isEmpty()) {
            CardState target = taunts.get(RANDOM.nextInt(taunts.size()));
            return new AttackTarget(false, target.getCardId(),
                    target.getNameCn() + "(嘲讽)");
        }

        // 攻击力>5 直接攻击英雄
        if (attacker.getAttack() > 5) {
            return new AttackTarget(true, null, "敌方英雄");
        }

        // 优先攻击低血(≤3)随从
        List<CardState> lowHealth = opponentBoard.stream()
                .filter(c -> c.getHealth() <= 3 && c.getHealth() > 0)
                .collect(Collectors.toList());
        if (!lowHealth.isEmpty()) {
            CardState target = lowHealth.get(RANDOM.nextInt(lowHealth.size()));
            return new AttackTarget(false, target.getCardId(),
                    target.getNameCn() + "(低血)");
        }

        // 随机选一个
        CardState target = opponentBoard.get(RANDOM.nextInt(opponentBoard.size()));
        return new AttackTarget(false, target.getCardId(), target.getNameCn());
    }

    // ==================== 英雄武器攻击 ====================

    private void heroWeaponAttackPhase(String sessionId) {
        GameSession session = gameEngine.getSession(sessionId);
        if (session == null || session.getPhase() != GamePhase.PLAYING) return;
        if (!session.getCurrentPlayerId().equals(BOT_USER_ID)) return;

        PlayerState bot = session.getPlayerState(BOT_USER_ID);
        PlayerState opponent = session.getOpponent(BOT_USER_ID);
        if (bot == null || opponent == null) return;
        if (bot.getWeapon() == null) return;
        if (bot.getWeapon().getDurability() <= 0) return;
        if (bot.isHasAttackedThisTurn()) return;

        // 检查嘲讽
        boolean hasTaunt = opponent.getBoard() != null &&
                opponent.getBoard().stream().anyMatch(CardState::isHasTaunt);

        GameEngine.AttackResult result;
        if (hasTaunt) {
            CardState tauntTarget = opponent.getBoard().stream()
                    .filter(CardState::isHasTaunt)
                    .findFirst().orElse(null);
            if (tauntTarget == null) return;
            result = gameEngine.heroAttack(sessionId, BOT_USER_ID,
                    "minion", tauntTarget.getCardId());
        } else {
            result = gameEngine.heroAttack(sessionId, BOT_USER_ID, "hero", null);
        }

        if (result != null && result.success) {
            log.info("[Bot] Hero weapon attack -> {}", result.targetType);
            broadcastAttackResultToHuman(sessionId, result);
            broadcastGameStateToHuman(sessionId);
            sleepRandom(1000, 1500);

            if (result.gameOver) {
                handleAIGameOver(sessionId);
            }
        }
    }

    // ==================== 结束回合 ====================

    private void endBotTurn(String sessionId) {
        GameSession session = gameEngine.getSession(sessionId);
        if (session == null) return;
        if (!session.getCurrentPlayerId().equals(BOT_USER_ID)) return;

        log.info("[Bot] Ending turn");

        // 结束AI回合，切换到人类
        GameEngine.EndTurnResult etr = gameEngine.endTurn(sessionId, BOT_USER_ID);
        if (etr == null) return;

        // 广播当前状态
        broadcastGameStateToHuman(sessionId);
        sleepRandom(500, 1000);

        // 开始人类的回合
        GameEngine.TurnStartResult tsr = gameEngine.startTurn(sessionId);
        if (tsr == null) return;

        Long humanId = getHumanUserId(session);
        if (humanId == null) return;

        // 发送 TURN_START 给人类
        BattleMessage.TurnStart turnStart = new BattleMessage.TurnStart();
        turnStart.turnNumber = tsr.turnNumber;
        turnStart.mana = tsr.mana;
        turnStart.maxMana = tsr.maxMana;
        turnStart.hand = tsr.hand;
        turnStart.drawnCard = tsr.drawnCard;

        messaging.convertAndSendToUser(
                humanId.toString(),
                "/queue/turn-start",
                new BattleMessage(BattleMessage.TYPE_TURN_START, sessionId, humanId, turnStart)
        );

        // 通知人类（对手回合结束，你的回合开始）
        Map<String, Object> opponentTurnNotice = Map.of(
                "turnNumber", tsr.turnNumber,
                "nextPlayerId", humanId,
                "opponentHandCount", tsr.hand != null ? tsr.hand.size() : 0
        );
        messaging.convertAndSendToUser(
                humanId.toString(),
                "/queue/opponent-turn",
                new BattleMessage("OPPONENT_TURN", sessionId, humanId, opponentTurnNotice)
        );

        log.info("[Bot] Turn ended, handed over to human {}", humanId);
    }

    // ==================== 游戏结束 ====================

    private void handleAIGameOver(String sessionId) {
        GameSession session = gameEngine.getSession(sessionId);
        if (session == null) return;

        // 根据当前血量判定胜者（如果还没设置的话）
        Long winnerId = session.getWinnerId();
        if (winnerId == null) {
            PlayerState p1 = session.getPlayer1();
            PlayerState p2 = session.getPlayer2();
            if (p1 != null && p2 != null) {
                if (p1.getHealth() <= 0) {
                    winnerId = session.getPlayer2Id();
                } else if (p2.getHealth() <= 0) {
                    winnerId = session.getPlayer1Id();
                }
            }
            if (winnerId == null) return;
        }

        // 调用GameEngine完成结算（跳过AI奖杯）
        gameEngine.endGame(session, winnerId);

        // 广播GameOver给人类玩家
        broadcastGameOverToHuman(session, winnerId);
    }

    // ==================== 广播方法 ====================

    private void broadcastOpponentCardPlay(String sessionId, CardState card) {
        GameSession session = gameEngine.getSession(sessionId);
        if (session == null) return;
        Long humanId = getHumanUserId(session);
        if (humanId == null) return;

        BattleMessage.AttackDeclared declared = new BattleMessage.AttackDeclared();
        declared.attackerId = card.getCardId();
        declared.attackerName = card.getNameCn();
        messaging.convertAndSendToUser(
                humanId.toString(),
                "/queue/opponent-action",
                new BattleMessage("OPPONENT_PLAY_CARD", sessionId, humanId, declared)
        );
    }

    private void broadcastAttackResultToHuman(String sessionId, GameEngine.AttackResult result) {
        GameSession session = gameEngine.getSession(sessionId);
        if (session == null) return;
        Long humanId = getHumanUserId(session);
        if (humanId == null) return;

        BattleMessage.AttackResult attackMsg = new BattleMessage.AttackResult();
        attackMsg.attackerId = result.attackerId;
        attackMsg.defenderId = result.defenderId;
        attackMsg.damage = result.damage;
        attackMsg.defenderDead = result.defenderDead;
        attackMsg.defenderHealthLeft = result.defenderHealthLeft;

        // 人类是被攻击方，收到 defense-result
        messaging.convertAndSendToUser(
                humanId.toString(),
                "/queue/defense-result",
                new BattleMessage(BattleMessage.TYPE_ATTACK_RESULT, sessionId, humanId, attackMsg)
        );
    }

    private void broadcastGameStateToHuman(String sessionId) {
        GameSession session = gameEngine.getSession(sessionId);
        if (session == null) return;

        Long humanId = getHumanUserId(session);
        if (humanId == null) return;

        PlayerState me = session.getPlayerState(humanId);
        PlayerState opponent = session.getOpponent(humanId);
        if (me == null || opponent == null) return;

        Map<String, Object> state = new HashMap<>();
        state.put("myHealth", me.getHealth());
        state.put("myMana", me.getMana());
        state.put("myMaxMana", me.getMaxMana());
        state.put("myBoard", me.getBoard() != null ? me.getBoard() : List.of());
        state.put("myDeckCount", me.getDeck() != null ? me.getDeck().size() : 0);
        state.put("myHandCount", me.getHand() != null ? me.getHand().size() : 0);
        state.put("opponentHealth", opponent.getHealth());
        state.put("opponentBoard", opponent.getBoard() != null ? opponent.getBoard() : List.of());
        state.put("opponentHandCount", opponent.getHand() != null ? opponent.getHand().size() : 0);
        state.put("opponentDeckCount", opponent.getDeck() != null ? opponent.getDeck().size() : 0);
        state.put("myWeapon", me.getWeapon());
        state.put("opponentWeapon", opponent.getWeapon());
        state.put("mySecretsCount", me.getSecrets() != null ? me.getSecrets().size() : 0);
        state.put("opponentSecretsCount", opponent.getSecrets() != null ? opponent.getSecrets().size() : 0);
        state.put("turnNumber", session.getTurnNumber());
        state.put("currentPlayerId", session.getCurrentPlayerId());
        state.put("isMyTurn", false); // AI正在行动

        messaging.convertAndSendToUser(
                humanId.toString(),
                "/queue/game-state",
                new BattleMessage("GAME_STATE", sessionId, humanId, state)
        );
    }

    private void broadcastGameOverToHuman(GameSession session, Long winnerId) {
        Long humanId = getHumanUserId(session);
        if (humanId == null) return;

        Long loserId = session.getOpponentId(winnerId);
        PlayerState winner = session.getPlayerState(winnerId);
        PlayerState loser = session.getOpponent(winnerId);

        // 只发送给人类玩家（AI不需要）
        boolean isHumanWinner = humanId.equals(winnerId);

        BattleMessage.GameOver gameOver = new BattleMessage.GameOver();
        gameOver.winnerId = winnerId;
        gameOver.loserId = loserId;
        gameOver.result = isHumanWinner ? "win" : "lose";
        gameOver.trophyChange = 0; // AI对战不增减奖杯

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
                humanId.toString(),
                "/queue/game-over",
                new BattleMessage(BattleMessage.TYPE_GAME_OVER, session.getSessionId(), humanId, gameOver)
        );
    }

    // ==================== 工具方法 ====================

    public static boolean isBot(Long userId) {
        return userId != null && userId == BOT_USER_ID;
    }

    private Long getHumanUserId(GameSession session) {
        if (session.getPlayer1Id().equals(BOT_USER_ID)) return session.getPlayer2Id();
        if (session.getPlayer2Id().equals(BOT_USER_ID)) return session.getPlayer1Id();
        return null;
    }

    private int botHandSize(String sessionId) {
        GameSession session = gameEngine.getSession(sessionId);
        if (session == null) return 0;
        PlayerState bot = session.getPlayerState(BOT_USER_ID);
        return bot != null && bot.getHand() != null ? bot.getHand().size() : 0;
    }

    private void sleepRandom(int minMs, int maxMs) {
        try {
            Thread.sleep(minMs + RANDOM.nextInt(maxMs - minMs + 1));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ==================== 内部类 ====================

    private static class AttackTarget {
        final boolean isHero;
        final Long targetId;
        final String description;

        AttackTarget(boolean isHero, Long targetId, String description) {
            this.isHero = isHero;
            this.targetId = targetId;
            this.description = description;
        }
    }
}
