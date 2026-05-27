package com.pengyouquan.english.battle;

import com.pengyouquan.english.model.Card;
import com.pengyouquan.english.model.User;
import com.pengyouquan.english.repository.CardRepository;
import com.pengyouquan.english.repository.UserCardRepository;
import com.pengyouquan.english.repository.UserRepository;
import com.pengyouquan.english.service.TrophyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 对战的回合逻辑引擎。纯卡牌策略操作，无英语答题环节。
 */
@Service
public class GameEngine {

    private static final Logger log = LoggerFactory.getLogger(GameEngine.class);

    private final ConcurrentHashMap<String, GameSession> activeGames = new ConcurrentHashMap<>();
    private final CardRepository cardRepository;
    private final UserCardRepository userCardRepository;
    private final UserRepository userRepository;
    private final TrophyService trophyService;

    private static final int TROPHY_GAIN = 30;
    private static final int TROPHY_LOSS = 25;
    private static final int MAX_HAND_SIZE = 8;
    private static final int INITIAL_HAND_SIZE = 3;
    private static final int DECK_SIZE = 20;
    private static final int TURN_TIME_LIMIT_SECONDS = 60;

    public GameEngine(CardRepository cardRepository,
                      UserCardRepository userCardRepository,
                      UserRepository userRepository,
                      TrophyService trophyService) {
        this.cardRepository = cardRepository;
        this.userCardRepository = userCardRepository;
        this.userRepository = userRepository;
        this.trophyService = trophyService;
    }

    // ==================== 公开方法 ====================

    /**
     * 创建新游戏会话
     */
    public GameSession createGame(Long player1Id, Long player2Id,
                                   String player1Name, String player2Name,
                                   int trophies1, int trophies2) {
        GameSession session = new GameSession();
        session.setSessionId(UUID.randomUUID().toString());
        session.setPlayer1Id(player1Id);
        session.setPlayer2Id(player2Id);
        session.setTurnNumber(0);
        session.setPhase(GamePhase.MULLIGAN);
        session.setStartTime(System.currentTimeMillis());
        session.setLastActionTime(System.currentTimeMillis());

        // 初始化双方牌组
        PlayerState p1 = initPlayer(player1Id, player1Name, trophies1);
        PlayerState p2 = initPlayer(player2Id, player2Name, trophies2);
        session.setPlayer1(p1);
        session.setPlayer2(p2);

        // 决定先手（随机）
        boolean p1First = new Random().nextBoolean();
        session.setCurrentPlayerId(p1First ? player1Id : player2Id);
        p1.setGoingFirst(p1First);
        p2.setGoingFirst(!p1First);

        // 后手：多抽1张牌 + 获得幸运币
        PlayerState second = p1First ? p2 : p1;
        if (second.getDeck() != null && !second.getDeck().isEmpty()) {
            CardState extraCard = second.getDeck().remove(second.getDeck().size() - 1);
            second.getHand().add(extraCard);
        }
        CardState coin = new CardState(0L, "幸运币", "The Coin",
                "spell", "common", 0, 0, 0, null);
        second.getHand().add(coin);

        activeGames.put(session.getSessionId(), session);

        log.info("Game created: {} (p1:{}) vs {} (p2:{}) - {} goes first",
                player1Name, player1Id, player2Name, player2Id,
                p1First ? player1Name : player2Name);

        return session;
    }

    /**
     * 开始新回合
     */
    public TurnStartResult startTurn(String sessionId) {
        GameSession session = activeGames.get(sessionId);
        if (session == null) return null;

        session.setTurnNumber(session.getTurnNumber() + 1);
        session.setLastActionTime(System.currentTimeMillis());

        int turn = session.getTurnNumber();
        int maxMana = Math.min(turn, 10);

        PlayerState current = session.getPlayerState(session.getCurrentPlayerId());
        current.setMaxMana(maxMana);
        current.setMana(maxMana);
        current.setHasPlayedThisTurn(false);
        current.setHasAttackedThisTurn(false);

        // 重置所有随从的攻击状态
        if (current.getBoard() != null) {
            for (CardState minion : current.getBoard()) {
                minion.setCanAttack(true);
            }
        }

        // 抽1张牌
        CardState drawnCard = drawCard(current);
        if (drawnCard != null) {
            if (current.getHand() == null) current.setHand(new ArrayList<>());
            if (current.getHand().size() < MAX_HAND_SIZE) {
                current.getHand().add(drawnCard);
            }
        }

        return new TurnStartResult(turn, current.getMana(), current.getMaxMana(),
                current.getHand(), drawnCard);
    }

    /**
     * 玩家出牌（纯策略操作，无答题）
     */
    public PlayCardResult playCard(String sessionId, Long userId, Long cardId, Long targetId) {
        GameSession session = activeGames.get(sessionId);
        if (session == null) return new PlayCardResult(false, "游戏不存在");

        if (!session.getCurrentPlayerId().equals(userId)) {
            return new PlayCardResult(false, "不是你的回合");
        }

        PlayerState player = session.getPlayerState(userId);
        if (player == null) return new PlayCardResult(false, "玩家不存在");

        // 查找手牌中的卡牌
        CardState card = findCardInHand(player, cardId);
        if (card == null) return new PlayCardResult(false, "手牌中没有这张卡");

        if (player.getMana() < card.getCost()) {
            return new PlayCardResult(false, "费用不足");
        }

        // 特殊：幸运币（cardId=0）—— 获得1点法力水晶
        if (card.getCardId() == 0L) {
            player.setMana(Math.min(player.getMana() + 1, player.getMaxMana()));
            player.setHasPlayedThisTurn(true);
            removeFromHand(player, cardId);
            PlayCardResult result = new PlayCardResult(true, null);
            result.card = card;
            result.manaRemaining = player.getMana();
            session.setLastActionTime(System.currentTimeMillis());
            return result;
        }

        // 扣除费用
        player.setMana(player.getMana() - card.getCost());
        player.setHasPlayedThisTurn(true);
        removeFromHand(player, cardId);

        PlayCardResult result = new PlayCardResult(true, null);
        result.card = card;
        result.manaRemaining = player.getMana();

        if ("spell".equals(card.getCardType())) {
            // 法术牌：直接造成伤害
            int damage = calculateSpellDamage(card);
            result.damageDealt = damage;

            PlayerState opponent = session.getOpponent(userId);
            if (opponent != null) {
                opponent.setHealth(opponent.getHealth() - damage);
                result.targetId = opponent.getUserId();
                result.targetType = "hero";
                if (opponent.getHealth() <= 0) {
                    opponent.setHealth(0);
                    result.gameOver = true;
                }
            }
        } else {
            // 随从牌：上场
            if (player.getBoard() == null) player.setBoard(new ArrayList<>());
            CardState playedCard = card;

            // 突袭(Rush)：出场本回合即可攻击
            if (playedCard.isHasRush()) {
                playedCard.setCanAttack(true);
            } else {
                playedCard.setCanAttack(false);
            }

            player.getBoard().add(playedCard);

            // 战吼(Battlecry)：出牌时触发一次性效果
            if (playedCard.isHasBattlecry() && !playedCard.isBattlecryTriggered()) {
                playedCard.setBattlecryTriggered(true);
                applyBattlecryEffect(player, session.getOpponent(userId), playedCard);
            }
        }

        session.setLastActionTime(System.currentTimeMillis());
        return result;
    }

    /**
     * 随从攻击（直接生效，无防御题）
     */
    public AttackResult declareAttack(String sessionId, Long userId,
                                                   Long attackerCardId, String targetType, Long targetId) {
        GameSession session = activeGames.get(sessionId);
        if (session == null) return new AttackResult(false, "游戏不存在");

        if (!session.getCurrentPlayerId().equals(userId)) {
            return new AttackResult(false, "不是你的回合");
        }

        PlayerState player = session.getPlayerState(userId);
        PlayerState opponent = session.getOpponent(userId);
        if (player == null || opponent == null) return new AttackResult(false, "玩家不存在");

        // 查找攻击者
        CardState attacker = findCardOnBoard(player, attackerCardId);
        if (attacker == null) return new AttackResult(false, "场上没有该随从");
        if (!attacker.isCanAttack()) return new AttackResult(false, "该随从本回合无法攻击");
        if (attacker.getAttack() <= 0) return new AttackResult(false, "该随从攻击力为0");

        // 检查嘲讽
        if (!"hero".equals(targetType)) {
            CardState target = findCardOnBoard(opponent, targetId);
            if (target == null) return new AttackResult(false, "目标不存在");

            // 检查潜行(Stealth)：不能攻击潜行随从
            if (target.isHasStealth() && !target.isStealthRevealed()) {
                return new AttackResult(false, "该随从具有潜行，无法被攻击");
            }

            // 检查对手场上是否有嘲讽随从
            boolean hasTaunt = opponent.getBoard() != null &&
                    opponent.getBoard().stream().anyMatch(CardState::isHasTaunt);
            if (hasTaunt && !target.isHasTaunt()) {
                return new AttackResult(false, "必须先攻击具有嘲讽的随从");
            }
        } else {
            // 攻击英雄：检查是否有嘲讽
            boolean hasTaunt = opponent.getBoard() != null &&
                    opponent.getBoard().stream().anyMatch(CardState::isHasTaunt);
            if (hasTaunt) {
                return new AttackResult(false, "必须先攻击具有嘲讽的随从");
            }
        }

        int baseDamage = attacker.getAttack();

        // 标记攻击者已攻击
        attacker.setCanAttack(false);
        player.setHasAttackedThisTurn(true);

        // 潜行(Stealth)：攻击后暴露，失去潜行效果
        if (attacker.isHasStealth() && !attacker.isStealthRevealed()) {
            attacker.setStealthRevealed(true);
        }

        AttackResult result = new AttackResult(true, null);
        result.attackerId = attackerCardId;
        result.attackerName = attacker.getNameCn();
        result.damage = baseDamage;
        result.targetType = targetType;

        if ("hero".equals(targetType)) {
            // 攻击英雄
            opponent.setHealth(opponent.getHealth() - baseDamage);
            result.defenderId = userId;
            result.defenderHealthLeft = opponent.getHealth();
            result.defenderDead = opponent.getHealth() <= 0;

            if (opponent.getHealth() <= 0) {
                opponent.setHealth(0);
                result.gameOver = true;
            }
        } else {
            // 攻击随从
            CardState targetCard = findCardOnBoard(opponent, targetId);
            if (targetCard != null) {
                int damage = baseDamage;

                // 圣盾(Divine Shield)：抵挡一次伤害后消失
                if (targetCard.isHasDivineShield()) {
                    targetCard.setHasDivineShield(false);
                    damage = 0;
                    result.divineShieldBlocked = true;
                }

                if (damage > 0) {
                    targetCard.setHealth(targetCard.getHealth() - damage);
                }
                result.defenderId = targetId;
                result.defenderHealthLeft = targetCard.getHealth();
                result.defenderDead = targetCard.getHealth() <= 0;

                if (targetCard.getHealth() <= 0) {
                    // 亡语(Deathrattle)：随从死亡时触发效果
                    if (targetCard.isHasDeathrattle()) {
                        triggerDeathrattle(session, opponent, player, targetCard);
                    }
                    opponent.getBoard().remove(targetCard);
                }
            } else {
                result.defenderHealthLeft = 0;
                result.defenderDead = true;
            }
        }

        session.setLastActionTime(System.currentTimeMillis());
        return result;
    }

    /**
     * 结束回合
     */
    public EndTurnResult endTurn(String sessionId, Long userId) {
        GameSession session = activeGames.get(sessionId);
        if (session == null) return null;

        if (!session.getCurrentPlayerId().equals(userId)) {
            return null;
        }

        // 切换当前玩家
        session.setCurrentPlayerId(session.getOpponentId(userId));
        session.setLastActionTime(System.currentTimeMillis());

        return new EndTurnResult(session.getCurrentPlayerId());
    }

    /**
     * 认输
     */
    public GameOverResult concede(String sessionId, Long userId) {
        GameSession session = activeGames.get(sessionId);
        if (session == null) return null;

        Long winnerId = session.getOpponentId(userId);
        return endGame(session, winnerId);
    }

    /**
     * 处理 Mulligan 换牌
     */
    public MulliganResult processMulligan(String sessionId, Long userId, List<Long> cardIds) {
        GameSession session = activeGames.get(sessionId);
        if (session == null) return new MulliganResult(false, "游戏不存在");
        if (session.getPhase() != GamePhase.MULLIGAN) return new MulliganResult(false, "不在换牌阶段");
        if (session.getMulliganSubmitted().contains(userId)) return new MulliganResult(false, "已提交过换牌");

        PlayerState player = session.getPlayerState(userId);
        if (player == null) return new MulliganResult(false, "玩家不存在");

        if (cardIds == null) cardIds = new ArrayList<>();

        // 将选中的牌放回牌堆
        List<CardState> cardsToReturn = new ArrayList<>();
        for (Long cardId : cardIds) {
            CardState card = findCardInHand(player, cardId);
            if (card != null) {
                cardsToReturn.add(card);
            }
        }
        for (CardState c : cardsToReturn) {
            removeFromHand(player, c.getCardId());
        }

        // 放回牌堆并洗牌
        player.getDeck().addAll(cardsToReturn);
        Collections.shuffle(player.getDeck());

        // 抽等量牌
        for (int i = 0; i < cardsToReturn.size() && !player.getDeck().isEmpty(); i++) {
            CardState drawn = player.getDeck().remove(player.getDeck().size() - 1);
            player.getHand().add(drawn);
        }

        // 标记已提交
        session.getMulliganSubmitted().add(userId);

        MulliganResult result = new MulliganResult(true, null);
        result.hand = new ArrayList<>(player.getHand());
        result.bothReady = session.getMulliganSubmitted().size() >= 2;

        if (result.bothReady) {
            session.setPhase(GamePhase.PLAYING);
        }

        session.setLastActionTime(System.currentTimeMillis());
        return result;
    }

    /**
     * 获取游戏会话
     */
    public GameSession getSession(String sessionId) {
        return activeGames.get(sessionId);
    }

    /**
     * 获取用户奖杯数
     */
    public int getTrophiesForUser(Long userId) {
        return trophyService.getTrophies(userId);
    }

    /**
     * 获取活跃游戏列表（用于管理）
     */
    public Map<String, GameSession> getActiveGames() {
        return activeGames;
    }

    // ==================== 定时清理 ====================

    /**
     * 每60秒清理游戏的过期会话（5分钟无操作结束，10分钟移除）
     */
    @Scheduled(fixedRate = 60000)
    public void cleanupStaleGames() {
        long now = System.currentTimeMillis();
        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, GameSession> entry : activeGames.entrySet()) {
            GameSession session = entry.getValue();
            long idle = now - session.getLastActionTime();
            if (idle > 10 * 60 * 1000) {
                // 超过10分钟，直接移除
                toRemove.add(entry.getKey());
            } else if (idle > 5 * 60 * 1000 && session.getPhase() == GamePhase.PLAYING) {
                // 超过5分钟无操作，强制结束平局
                if (session.getWinnerId() == null) {
                    session.setPhase(GamePhase.FINISHED);
                    log.info("Game {} auto-ended due to inactivity ({}ms idle)", entry.getKey(), idle);
                }
            }
        }
        for (String key : toRemove) {
            activeGames.remove(key);
            log.info("Removed stale game session: {}", key);
        }
    }

    // ==================== 私有方法 ====================

    private PlayerState initPlayer(Long userId, String nickname, int trophies) {
        PlayerState state = new PlayerState();
        state.setUserId(userId);
        state.setNickname(nickname);
        state.setHealth(30);
        state.setMana(1);
        state.setMaxMana(1);
        state.setTrophies(trophies);

        // 获取用户拥有的卡牌，构建牌组
        List<com.pengyouquan.english.model.UserCard> userCards = userCardRepository.findByUserId(userId);
        // 构建金卡ID集合
        Set<Long> goldenCardIds = userCards.stream()
                .filter(uc -> Boolean.TRUE.equals(uc.getIsGolden()))
                .map(com.pengyouquan.english.model.UserCard::getCardId)
                .collect(Collectors.toSet());

        if (userCards.isEmpty()) {
            List<Card> allCards = cardRepository.findAll();
            List<Card> shuffled = new ArrayList<>(allCards);
            Collections.shuffle(shuffled);
            List<Card> deckCards = shuffled.subList(0, Math.min(DECK_SIZE, shuffled.size()));
            state.setDeck(applyGoldenBoost(deckCards, goldenCardIds));
        } else {
            List<Card> allOwnedCards = cardRepository.findAllById(
                    userCards.stream().map(uc -> uc.getCardId()).collect(Collectors.toList()));
            Collections.shuffle(allOwnedCards);
            List<Card> deckCards = allOwnedCards.subList(0, Math.min(DECK_SIZE, allOwnedCards.size()));
            state.setDeck(applyGoldenBoost(deckCards, goldenCardIds));
        }

        // 洗牌
        if (state.getDeck() != null) {
            Collections.shuffle(state.getDeck());
        } else {
            state.setDeck(new ArrayList<>());
        }

        // 抽起始手牌
        state.setHand(new ArrayList<>());
        for (int i = 0; i < INITIAL_HAND_SIZE && !state.getDeck().isEmpty(); i++) {
            CardState drawn = state.getDeck().remove(state.getDeck().size() - 1);
            state.getHand().add(drawn);
        }

        state.setBoard(new ArrayList<>());

        return state;
    }

    private List<CardState> cardsToCardStates(List<Card> cards) {
        return cards.stream().map(c -> {
            int baseAttack = c.getAttack() != null ? c.getAttack() : 0;
            int baseHealth = c.getHealth() != null ? c.getHealth() : 0;

            CardState cs = new CardState(
                    c.getId(), c.getNameCn(), c.getNameEn(),
                    c.getCardType(), c.getRarity(),
                    c.getCost() != null ? c.getCost() : 0,
                    baseAttack,
                    baseHealth,
                    c.getEffectJson()
            );
            // 解析关键词
            parseKeywords(cs, c);
            return cs;
        }).collect(Collectors.toList());
    }

    /**
     * 构建金卡增强状态的CardState（金卡+1攻+1血）
     */
    private CardState buildGoldenCardState(Card card) {
        int goldenAttack = (card.getAttack() != null ? card.getAttack() : 0) + 1;
        int goldenHealth = (card.getHealth() != null ? card.getHealth() : 0) + 1;
        CardState cs = new CardState(
                card.getId(), card.getNameCn(), card.getNameEn(),
                card.getCardType(), card.getRarity(),
                card.getCost() != null ? card.getCost() : 0,
                goldenAttack,
                goldenHealth,
                card.getEffectJson()
        );
        parseKeywords(cs, card);
        return cs;
    }

    private List<CardState> applyGoldenBoost(List<Card> cards, Set<Long> goldenCardIds) {
        return cards.stream().map(c -> {
            if (goldenCardIds.contains(c.getId())) {
                return buildGoldenCardState(c);
            }
            return cardsToCardStates(List.of(c)).get(0);
        }).collect(Collectors.toList());
    }

    private void parseKeywords(CardState cs, Card card) {
        String kw = card.getKeywords();
        if (kw == null || kw.isEmpty() || "[]".equals(kw.trim())) {
            String effectJson = card.getEffectJson();
            if (effectJson != null && !effectJson.isEmpty()) {
                try {
                    com.fasterxml.jackson.databind.JsonNode node =
                            new com.fasterxml.jackson.databind.ObjectMapper().readTree(effectJson);
                    com.fasterxml.jackson.databind.JsonNode kws = node.get("keywords");
                    if (kws != null && kws.isArray()) {
                        for (com.fasterxml.jackson.databind.JsonNode k : kws) {
                            applyKeyword(cs, k.asText());
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse effect_json keywords for card {}: {}", card.getId(), e.getMessage());
                }
            }
        } else {
            try {
                com.fasterxml.jackson.databind.JsonNode kws =
                        new com.fasterxml.jackson.databind.ObjectMapper().readTree(kw);
                if (kws.isArray()) {
                    for (com.fasterxml.jackson.databind.JsonNode k : kws) {
                        applyKeyword(cs, k.asText());
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to parse keywords for card {}: {}", card.getId(), e.getMessage());
            }
        }
        cs.setKeywords(kw);
    }

    private void applyKeyword(CardState cs, String keyword) {
        switch (keyword.toLowerCase()) {
            case "taunt":
                cs.setHasTaunt(true);
                break;
            case "divine_shield":
                cs.setHasDivineShield(true);
                break;
            case "deathrattle":
                cs.setHasDeathrattle(true);
                break;
            case "battlecry":
                cs.setHasBattlecry(true);
                break;
            case "stealth":
                cs.setHasStealth(true);
                break;
            case "rush":
                cs.setHasRush(true);
                break;
            case "charge":
                cs.setHasRush(true);
                break;
        }
    }

    private CardState drawCard(PlayerState player) {
        if (player.getDeck() == null || player.getDeck().isEmpty()) {
            player.setHealth(player.getHealth() - 1);
            return null;
        }
        return player.getDeck().remove(player.getDeck().size() - 1);
    }

    private CardState findCardInHand(PlayerState player, Long cardId) {
        if (player.getHand() == null) return null;
        return player.getHand().stream()
                .filter(c -> c.getCardId().equals(cardId))
                .findFirst().orElse(null);
    }

    private CardState findCardOnBoard(PlayerState player, Long cardId) {
        if (player.getBoard() == null) return null;
        return player.getBoard().stream()
                .filter(c -> c.getCardId().equals(cardId))
                .findFirst().orElse(null);
    }

    private void removeFromHand(PlayerState player, Long cardId) {
        if (player.getHand() == null) return;
        player.getHand().removeIf(c -> c.getCardId().equals(cardId));
    }

    private int calculateSpellDamage(CardState card) {
        return Math.max(1, card.getCost());
    }

    private void applyBattlecryEffect(PlayerState player, PlayerState opponent, CardState card) {
        // 基础战吼效果：为当前玩家英雄恢复2点生命
        player.setHealth(Math.min(30, player.getHealth() + 2));
        log.info("Battlecry triggered for {}: heal owner for 2", card.getNameCn());
    }

    private void triggerDeathrattle(GameSession session, PlayerState defender, PlayerState attacker, CardState card) {
        // 基础亡语效果：对敌方英雄造成2点伤害
        attacker.setHealth(attacker.getHealth() - 2);
        if (attacker.getHealth() <= 0) {
            attacker.setHealth(0);
            session.setPhase(GamePhase.FINISHED);
            session.setWinnerId(defender.getUserId());
        }
        log.info("Deathrattle triggered for {}: deal 2 damage to enemy hero", card.getNameCn());
    }

    private GameOverResult endGame(GameSession session, Long winnerId) {
        session.setPhase(GamePhase.FINISHED);
        session.setWinnerId(winnerId);
        session.setLastActionTime(System.currentTimeMillis());

        Long loserId = session.getOpponentId(winnerId);
        PlayerState winner = session.getPlayerState(winnerId);
        PlayerState loser = session.getOpponent(winnerId);

        // 更新奖杯
        try {
            trophyService.updateTrophies(winnerId, TROPHY_GAIN);
            trophyService.updateTrophies(loserId, -TROPHY_LOSS);
        } catch (Exception e) {
            log.error("Failed to update trophies for game {}: {}", session.getSessionId(), e.getMessage());
        }

        GameOverResult result = new GameOverResult();
        result.winnerId = winnerId;
        result.loserId = loserId;
        result.winnerName = winner != null ? winner.getNickname() : "";
        result.loserName = loser != null ? loser.getNickname() : "";
        result.trophyChange = TROPHY_GAIN;
        result.winnerHealth = winner != null ? winner.getHealth() : 0;
        result.loserHealth = loser != null ? loser.getHealth() : 0;

        int winnerTrophies = trophyService.getTrophies(winnerId);
        int loserTrophies = trophyService.getTrophies(loserId);
        result.winnerTrophiesAfter = winnerTrophies;
        result.loserTrophiesAfter = loserTrophies;

        activeGames.remove(session.getSessionId());

        log.info("Game over: {} (winner) vs {} - trophies: {} -> {}, {} -> {}",
                winnerId, loserId,
                winner != null ? winner.getTrophies() : 0, winnerTrophies,
                loser != null ? loser.getTrophies() : 0, loserTrophies);

        return result;
    }

    // ==================== 内部类 ====================

    public static class TurnStartResult {
        public int turnNumber;
        public int mana;
        public int maxMana;
        public List<CardState> hand;
        public CardState drawnCard;

        public TurnStartResult(int turnNumber, int mana, int maxMana, List<CardState> hand, CardState drawnCard) {
            this.turnNumber = turnNumber;
            this.mana = mana;
            this.maxMana = maxMana;
            this.hand = hand;
            this.drawnCard = drawnCard;
        }
    }

    public static class PlayCardResult {
        public boolean success;
        public String errorMessage;
        public CardState card;
        public int manaRemaining;
        public int damageDealt;
        public Long targetId;
        public String targetType;
        public boolean gameOver;

        public PlayCardResult(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
        }
    }

    public static class AttackResult {
        public boolean success;
        public String errorMessage;
        public Long attackerId;
        public String attackerName;
        public int damage;
        public Long defenderId;
        public int defenderHealthLeft;
        public boolean defenderDead;
        public boolean gameOver;
        public String targetType;
        public boolean divineShieldBlocked;

        public AttackResult(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
        }
    }

    public static class EndTurnResult {
        public Long nextPlayerId;
        public EndTurnResult(Long nextPlayerId) {
            this.nextPlayerId = nextPlayerId;
        }
    }

    public static class GameOverResult {
        public Long winnerId;
        public Long loserId;
        public String winnerName;
        public String loserName;
        public int trophyChange;
        public int winnerHealth;
        public int loserHealth;
        public int winnerTrophiesAfter;
        public int loserTrophiesAfter;

        public GameOverResult() {}
    }

    public static class MulliganResult {
        public boolean success;
        public String errorMessage;
        public List<CardState> hand;
        public boolean bothReady;

        public MulliganResult(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
        }
    }
}
