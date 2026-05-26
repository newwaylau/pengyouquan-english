package com.pengyouquan.english.battle;

import com.pengyouquan.english.model.Card;
import com.pengyouquan.english.model.Sentence;
import com.pengyouquan.english.model.User;
import com.pengyouquan.english.repository.CardRepository;
import com.pengyouquan.english.repository.SentenceRepository;
import com.pengyouquan.english.repository.UserCardRepository;
import com.pengyouquan.english.repository.UserRepository;
import com.pengyouquan.english.service.TrophyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 对战的回合逻辑引擎。管理所有活跃的游戏会话。
 */
@Service
public class GameEngine {

    private static final Logger log = LoggerFactory.getLogger(GameEngine.class);

    private final ConcurrentHashMap<String, GameSession> activeGames = new ConcurrentHashMap<>();
    private final CardRepository cardRepository;
    private final SentenceRepository sentenceRepository;
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
                      SentenceRepository sentenceRepository,
                      UserCardRepository userCardRepository,
                      UserRepository userRepository,
                      TrophyService trophyService) {
        this.cardRepository = cardRepository;
        this.sentenceRepository = sentenceRepository;
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
        session.setPhase(GamePhase.PLAYING);
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
        int maxMana = Math.min(turn + 2, 10);

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
     * 玩家出牌，生成考题
     */
    public QuestionResult playCard(String sessionId, Long userId, Long cardId) {
        GameSession session = activeGames.get(sessionId);
        if (session == null) return new QuestionResult(false, "游戏不存在");

        if (!session.getCurrentPlayerId().equals(userId)) {
            return new QuestionResult(false, "不是你的回合");
        }

        PlayerState player = session.getPlayerState(userId);
        if (player == null) return new QuestionResult(false, "玩家不存在");

        // 查找手牌中的卡牌
        CardState card = findCardInHand(player, cardId);
        if (card == null) return new QuestionResult(false, "手牌中没有这张卡");

        if (player.getMana() < card.getCost()) {
            return new QuestionResult(false, "费用不足");
        }

        // 法术卡无需上板，直接确认答题
        if ("spell".equals(card.getCardType())) {
            // 直接扣除费用
            player.setMana(player.getMana() - card.getCost());
            player.setHasPlayedThisTurn(true);
            removeFromHand(player, cardId);

            // 生成回复
            QuestionResult result = new QuestionResult(true, null);
            result.card = card;
            result.manaRemaining = player.getMana();
            return result;
        }

        // 生成考题（费用决定难度）
        String questionJson = generateQuestionJson(userId, card, session);

        QuestionResult result = new QuestionResult(true, null);
        result.card = card;
        result.manaRemaining = player.getMana();
        result.questionData = questionJson;
        result.questionType = getQuestionType(card.getCost());
        result.timeLimit = TURN_TIME_LIMIT_SECONDS;

        return result;
    }

    /**
     * 玩家答题结果
     */
    public AnswerResult submitAnswer(String sessionId, Long userId, boolean correct) {
        GameSession session = activeGames.get(sessionId);
        if (session == null) return new AnswerResult(false, "游戏不存在");

        PlayerState player = session.getPlayerState(userId);
        if (player == null) return new AnswerResult(false, "玩家不存在");

        // 从session的pending动作中获取
        PendingAction action = pendingActions.get(sessionId + ":" + userId);
        if (action == null) return new AnswerResult(false, "没有待处理的出牌");

        player.setAnsweredTotal(player.getAnsweredTotal() + 1);

        if (correct) {
            player.setAnsweredCorrectly(player.getAnsweredCorrectly() + 1);
            player.setConsecutiveCorrect(player.getConsecutiveCorrect() + 1);
            player.setConsecutiveWrong(0);

            // 扣除费用
            player.setMana(player.getMana() - action.getCard().getCost());
            player.setHasPlayedThisTurn(true);

            // 卡牌上场
            if (!"spell".equals(action.getCard().getCardType())) {
                if (player.getBoard() == null) player.setBoard(new ArrayList<>());
                CardState playedCard = action.getCard();

                // 突袭(Rush)：出场本回合即可攻击
                if (playedCard.isHasRush()) {
                    playedCard.setCanAttack(true);
                } else {
                    playedCard.setCanAttack(false); // 刚上场的随从本回合不能攻击（除非有突袭）
                }

                // 潜行(Stealth)：刚出场不可被攻击
                // stealthRevealed starts as false, opponent can't target

                player.getBoard().add(playedCard);

                // 战吼(Battlecry)：出牌时触发一次性效果
                if (playedCard.isHasBattlecry() && !playedCard.isBattlecryTriggered()) {
                    playedCard.setBattlecryTriggered(true);
                    applyBattlecryEffect(player, session.getOpponent(userId), playedCard);
                }
            }

            // 检查连击（3连正确 → 下一张免费）
            int comboBonus = 0;
            if (player.getConsecutiveCorrect() == 3) {
                comboBonus = 1; // 下一张减1费
                player.setConsecutiveCorrect(0);
            }

            pendingActions.remove(sessionId + ":" + userId);

            AnswerResult result = new AnswerResult(true, null);
            result.card = action.getCard();
            result.manaRemaining = player.getMana();
            result.comboCount = player.getConsecutiveCorrect();
            result.comboBonus = comboBonus;

            // 法术牌直接造成伤害
            if ("spell".equals(action.getCard().getCardType())) {
                int damage = calculateSpellDamage(action.getCard());
                result.damageDealt = damage;

                // 伤害对手英雄
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
            }

            session.setLastActionTime(System.currentTimeMillis());
            return result;

        } else {
            // 答错
            player.setConsecutiveCorrect(0);
            player.setConsecutiveWrong(player.getConsecutiveWrong() + 1);
            // 费用不退
            player.setHasPlayedThisTurn(true);

            // 对手获得补偿：抽1张+回2血
            PlayerState opponent = session.getOpponent(userId);
            if (opponent != null) {
                CardState compensationCard = drawCard(opponent);
                if (compensationCard != null && opponent.getHand() != null && opponent.getHand().size() < MAX_HAND_SIZE) {
                    opponent.getHand().add(compensationCard);
                }
                opponent.setHealth(Math.min(30, opponent.getHealth() + 2));
            }

            pendingActions.remove(sessionId + ":" + userId);

            AnswerResult result = new AnswerResult(false, "答题错误");
            result.card = action.getCard();
            result.manaRemaining = player.getMana();
            result.opponentGainedCard = true;
            result.opponentHealed = 2;

            session.setLastActionTime(System.currentTimeMillis());
            return result;
        }
    }

    /**
     * 随从攻击声明
     */
    public AttackDeclarationResult declareAttack(String sessionId, Long userId,
                                                   Long attackerCardId, String targetType, Long targetId) {
        GameSession session = activeGames.get(sessionId);
        if (session == null) return new AttackDeclarationResult(false, "游戏不存在");

        if (!session.getCurrentPlayerId().equals(userId)) {
            return new AttackDeclarationResult(false, "不是你的回合");
        }

        PlayerState player = session.getPlayerState(userId);
        PlayerState opponent = session.getOpponent(userId);
        if (player == null || opponent == null) return new AttackDeclarationResult(false, "玩家不存在");

        // 查找攻击者
        CardState attacker = findCardOnBoard(player, attackerCardId);
        if (attacker == null) return new AttackDeclarationResult(false, "场上没有该随从");
        if (!attacker.isCanAttack()) return new AttackDeclarationResult(false, "该随从本回合无法攻击");
        if (attacker.getAttack() <= 0) return new AttackDeclarationResult(false, "该随从攻击力为0");

        // 检查嘲讽
        if (!"hero".equals(targetType)) {
            CardState target = findCardOnBoard(opponent, targetId);
            if (target == null) return new AttackDeclarationResult(false, "目标不存在");

            // 检查潜行(Stealth)：不能攻击潜行随从
            if (target.isHasStealth() && !target.isStealthRevealed()) {
                return new AttackDeclarationResult(false, "该随从具有潜行，无法被攻击");
            }

            // 检查对手场上是否有嘲讽随从
            boolean hasTaunt = opponent.getBoard() != null &&
                    opponent.getBoard().stream().anyMatch(CardState::isHasTaunt);
            if (hasTaunt && !target.isHasTaunt()) {
                return new AttackDeclarationResult(false, "必须先攻击具有嘲讽的随从");
            }
        } else {
            // 攻击英雄：检查是否有嘲讽
            boolean hasTaunt = opponent.getBoard() != null &&
                    opponent.getBoard().stream().anyMatch(CardState::isHasTaunt);
            if (hasTaunt) {
                return new AttackDeclarationResult(false, "必须先攻击具有嘲讽的随从");
            }
        }

        // 标记攻击者已攻击
        attacker.setCanAttack(false);
        player.setHasAttackedThisTurn(true);
        session.setLastActionTime(System.currentTimeMillis());

        // 潜行(Stealth)：攻击后暴露，失去潜行效果
        if (attacker.isHasStealth() && !attacker.isStealthRevealed()) {
            attacker.setStealthRevealed(true);
        }

        // 生成防御题给对手
        String defenseQuestionJson = generateDefenseQuestionJson(session, opponent, attacker);

        AttackDeclarationResult result = new AttackDeclarationResult(true, null);
        result.attackerId = attackerCardId;
        result.attackerName = attacker.getNameCn();
        result.attackerAttack = attacker.getAttack();
        result.targetId = targetId;
        result.targetType = targetType;
        result.defenseQuestionData = defenseQuestionJson;

        // 存储pending攻击
        String pendingKey = sessionId + ":defense:" + opponent.getUserId();
        pendingAttacks.put(pendingKey, new PendingAttack(attackerCardId, targetId, targetType, userId));

        return result;
    }

    /**
     * 防御题结果
     */
    public DefenseResult submitDefense(String sessionId, Long userId, boolean correct) {
        GameSession session = activeGames.get(sessionId);
        if (session == null) return new DefenseResult(false, "游戏不存在");

        PlayerState defender = session.getPlayerState(userId);
        PlayerState attacker = session.getOpponent(userId);
        if (defender == null || attacker == null) return new DefenseResult(false, "玩家不存在");

        String pendingKey = sessionId + ":defense:" + userId;
        PendingAttack pending = pendingAttacks.get(pendingKey);
        if (pending == null) return new DefenseResult(false, "没有待处理的防御");

        // 查找攻击者
        CardState attackerCard = findCardOnBoard(attacker, pending.getAttackerCardId());
        if (attackerCard == null) return new DefenseResult(false, "攻击者已不在场上");

        int baseDamage = attackerCard.getAttack();
        int actualDamage = correct ? baseDamage / 2 : baseDamage;
        if (actualDamage < 1) actualDamage = 1;

        pendingAttacks.remove(pendingKey);

        DefenseResult result = new DefenseResult(true, null);
        result.attackerId = pending.getAttackerCardId();
        result.damage = actualDamage;
        result.defenderCorrect = correct;
        result.targetType = pending.getTargetType();

        if ("hero".equals(pending.getTargetType())) {
            // 攻击英雄
            defender.setHealth(defender.getHealth() - actualDamage);
            result.defenderId = userId;
            result.defenderHealthLeft = defender.getHealth();
            result.defenderDead = defender.getHealth() <= 0;

            if (defender.getHealth() <= 0) {
                defender.setHealth(0);
                result.gameOver = true;
            }
        } else {
            // 攻击随从
            CardState targetCard = findCardOnBoard(defender, pending.getTargetId());
            if (targetCard != null) {
                int damage = actualDamage;

                // 圣盾(Divine Shield)：抵挡一次伤害后消失
                if (targetCard.isHasDivineShield()) {
                    targetCard.setHasDivineShield(false);
                    damage = 0;
                    result.divineShieldBlocked = true;
                }

                if (damage > 0) {
                    targetCard.setHealth(targetCard.getHealth() - damage);
                }
                result.defenderId = pending.getTargetId();
                result.defenderHealthLeft = targetCard.getHealth();
                result.defenderDead = targetCard.getHealth() <= 0;

                if (targetCard.getHealth() <= 0) {
                    // 亡语(Deathrattle)：随从死亡时触发效果
                    if (targetCard.isHasDeathrattle()) {
                        triggerDeathrattle(session, defender, attacker, targetCard);
                    }
                    defender.getBoard().remove(targetCard);
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

    // ==================== 私有方法 ====================

    private PlayerState initPlayer(Long userId, String nickname, int trophies) {
        PlayerState state = new PlayerState();
        state.setUserId(userId);
        state.setNickname(nickname);
        state.setHealth(30);
        state.setMana(3);
        state.setMaxMana(3);
        state.setTrophies(trophies);

        // 获取用户拥有的卡牌，构建牌组
        List<com.pengyouquan.english.model.UserCard> userCards = userCardRepository.findByUserId(userId);
        // 构建金卡ID集合
        Set<Long> goldenCardIds = userCards.stream()
                .filter(uc -> Boolean.TRUE.equals(uc.getIsGolden()))
                .map(com.pengyouquan.english.model.UserCard::getCardId)
                .collect(Collectors.toSet());

        if (userCards.isEmpty()) {
            // 没有收藏的卡，用全部卡牌
            List<Card> allCards = cardRepository.findAll();
            List<Card> shuffled = new ArrayList<>(allCards);
            Collections.shuffle(shuffled);
            List<Card> deckCards = shuffled.subList(0, Math.min(DECK_SIZE, shuffled.size()));
            state.setDeck(applyGoldenBoost(deckCards, goldenCardIds));
        } else {
            // 从收藏中构建牌组（按稀有度和随机混合）
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
                    c.getEffectJson(), c.getChallengeSentenceId()
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
                card.getEffectJson(), card.getChallengeSentenceId()
        );
        parseKeywords(cs, card);
        return cs;
    }

    /**
     * 对牌组应用金卡加成：金卡版本的卡牌+1攻击、+1生命
     */
    private List<CardState> applyGoldenBoost(List<Card> cards, Set<Long> goldenCardIds) {
        return cards.stream().map(c -> {
            if (goldenCardIds.contains(c.getId())) {
                return buildGoldenCardState(c);
            }
            return cardsToCardStates(List.of(c)).get(0);
        }).collect(Collectors.toList());
    }

    /**
     * 从卡牌数据解析关键词，设置 CardState 的标志位
     */
    private void parseKeywords(CardState cs, Card card) {
        // 优先使用独立的 keywords 字段
        String kw = card.getKeywords();
        if (kw == null || kw.isEmpty() || "[]".equals(kw.trim())) {
            // 回退到 effect_json 中的 keywords 数组
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

    /**
     * 应用单个关键词到 CardState
     */
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
                cs.setHasRush(true); // 兼容旧数据中的 "charge" 关键词
                break;
        }
    }

    private CardState drawCard(PlayerState player) {
        if (player.getDeck() == null || player.getDeck().isEmpty()) {
            // 牌库为空，疲劳伤害
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

    /**
     * 应用战吼效果（基础实现：恢复2点生命给英雄）
     */
    private void applyBattlecryEffect(PlayerState player, PlayerState opponent, CardState card) {
        // 基础战吼效果：为当前玩家英雄恢复2点生命
        player.setHealth(Math.min(30, player.getHealth() + 2));
        log.info("Battlecry triggered for {}: heal owner for 2", card.getNameCn());
    }

    /**
     * 触发亡语效果（基础实现：对敌方英雄造成2点伤害）
     */
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

    private String getQuestionType(int cost) {
        if (cost <= 2) return "listening";
        if (cost <= 4) return "spelling";
        if (cost <= 6) return "dictation";
        if (cost <= 8) return "ordering";
        return "comprehension";
    }

    /**
     * 生成考题内容
     */
    private String generateQuestionJson(Long userId, CardState card, GameSession session) {
        // 尝试使用卡牌绑定的句子
        if (card.getChallengeSentenceId() != null) {
            Optional<Sentence> optSentence = sentenceRepository.findById(card.getChallengeSentenceId());
            if (optSentence.isPresent() && !optSentence.get().getIsDisabled()) {
                return buildQuestionFromSentence(optSentence.get(), card.getCost());
            }
        }

        // 随机选一个句子
        List<Sentence> randomSentences = sentenceRepository.findRandom(1);
        if (!randomSentences.isEmpty()) {
            return buildQuestionFromSentence(randomSentences.get(0), card.getCost());
        }

        // 兜底：没有句子可用
        return "{\"type\":\"spelling\",\"prompt\":\"" + card.getNameEn() + "\",\"answer\":\"" + card.getNameEn() + "\"}";
    }

    private String buildQuestionFromSentence(Sentence sentence, int cost) {
        String text = sentence.getText();
        // 移除标点，分割单词
        String cleanText = text.replaceAll("[^a-zA-Z\\s']", " ").trim();
        String[] words = cleanText.split("\\s+");
        if (words.length == 0) words = new String[]{"word"};

        String questionType = getQuestionType(cost);
        StringBuilder json = new StringBuilder();

        switch (questionType) {
            case "listening": {
                // 听音频选词：选一个关键词，提供4个选项
                String keyword = words[new Random().nextInt(words.length)];
                if (keyword.isEmpty()) keyword = words[0];
                String[] distractors = generateDistractors(keyword);
                json.append("{\"type\":\"listening\",");
                json.append("\"sentence\":\"").append(escapeJson(sentence.getText())).append("\",");
                json.append("\"audioFile\":\"").append(escapeJson(sentence.getAudioFile() != null ? sentence.getAudioFile() : "")).append("\",");
                json.append("\"keyword\":\"").append(escapeJson(keyword)).append("\",");
                json.append("\"options\":[");
                for (int i = 0; i < distractors.length; i++) {
                    if (i > 0) json.append(",");
                    json.append("\"").append(escapeJson(distractors[i])).append("\"");
                }
                json.append("],");
                json.append("\"correctIndex\":").append(new Random().nextInt(4));
                json.append("}");
                break;
            }
            case "spelling": {
                // 拼写：展示中文（如果有），拼写英文
                String targetWord = words[new Random().nextInt(words.length)];
                if (targetWord.isEmpty()) targetWord = words[0];
                json.append("{\"type\":\"spelling\",");
                json.append("\"prompt\":\"").append(escapeJson(targetWord)).append("\",");
                json.append("\"answer\":\"").append(escapeJson(targetWord)).append("\"}");
                break;
            }
            case "dictation": {
                // 听写：展示部分句子，填完整
                String partial = words.length > 4 ? String.join(" ", Arrays.copyOf(words, words.length / 2)) + "..." : text;
                json.append("{\"type\":\"dictation\",");
                json.append("\"prompt\":\"").append(escapeJson(partial)).append("\",");
                json.append("\"answer\":\"").append(escapeJson(text)).append("\"}");
                break;
            }
            case "ordering": {
                // 排序：打乱单词顺序
                List<String> wordList = new ArrayList<>(Arrays.asList(words));
                Collections.shuffle(wordList);
                StringBuilder shuffled = new StringBuilder();
                for (String w : wordList) {
                    if (shuffled.length() > 0) shuffled.append(" ");
                    shuffled.append(w);
                }
                json.append("{\"type\":\"ordering\",");
                json.append("\"shuffled\":\"").append(escapeJson(shuffled.toString())).append("\",");
                json.append("\"answer\":\"").append(escapeJson(cleanText)).append("\"}");
                break;
            }
            default: {
                // comprehension: 简单问答
                json.append("{\"type\":\"spelling\",");
                json.append("\"prompt\":\"").append(escapeJson(text)).append("\",");
                json.append("\"answer\":\"").append(escapeJson(text)).append("\"}");
                break;
            }
        }

        return json.toString();
    }

    /**
     * 生成防御题
     */
    private String generateDefenseQuestionJson(GameSession session, PlayerState defender, CardState attacker) {
        // 随机选一个句子做防御题
        List<Sentence> randomSentences = sentenceRepository.findRandom(1);
        if (!randomSentences.isEmpty()) {
            Sentence s = randomSentences.get(0);
            String text = s.getText().replaceAll("[^a-zA-Z\\s']", " ").trim();
            String[] words = text.split("\\s+");
            if (words.length == 0) words = new String[]{"defense"};

            String keyword = words[new Random().nextInt(words.length)];
            if (keyword.isEmpty()) keyword = words[0];

            StringBuilder json = new StringBuilder();
            json.append("{\"type\":\"defense\",");
            json.append("\"sentence\":\"").append(escapeJson(s.getText())).append("\",");
            json.append("\"keyword\":\"").append(escapeJson(keyword)).append("\"}");
            return json.toString();
        }

        return "{\"type\":\"defense\",\"sentence\":\"The winter is coming.\",\"keyword\":\"winter\"}";
    }

    private String[] generateDistractors(String keyword) {
        // 生成干扰项
        String[] distractors = new String[4];
        distractors[0] = keyword;

        String[] commonWords = {"the", "and", "for", "are", "but", "not", "you", "all", "any", "can",
                "had", "her", "was", "one", "our", "out", "has", "have", "been", "cold",
                "dark", "king", "lord", "wolf", "star", "fire", "land", "winter", "summer", "night"};

        Set<String> used = new HashSet<>();
        used.add(keyword.toLowerCase());
        String keywordLower = keyword.toLowerCase();

        for (int i = 1; i < 4; i++) {
            String distractor;
            int attempts = 0;
            do {
                if (keyword.length() > 3 && attempts < 5) {
                    // 生成一个和关键词相似的词（换一个字母）
                    distractor = modifyWord(keyword);
                } else {
                    distractor = commonWords[new Random().nextInt(commonWords.length)];
                }
                attempts++;
            } while (used.contains(distractor.toLowerCase()) && attempts < 20);
            used.add(distractor.toLowerCase());
            distractors[i] = distractor;
        }

        // 打乱
        List<String> list = Arrays.asList(distractors);
        Collections.shuffle(list);
        return list.toArray(new String[0]);
    }

    private String modifyWord(String word) {
        if (word.length() <= 2) return word;
        char[] chars = word.toCharArray();
        int pos = new Random().nextInt(chars.length);
        char original = chars[pos];
        // 替换为邻近字母
        char[] replacements = "abcdefghijklmnopqrstuvwxyz".toCharArray();
        for (int i = 0; i < replacements.length; i++) {
            if (replacements[i] == Character.toLowerCase(original)) {
                int idx = (i + 1 + new Random().nextInt(3)) % replacements.length;
                chars[pos] = Character.isUpperCase(original) ?
                        Character.toUpperCase(replacements[idx]) : replacements[idx];
                break;
            }
        }
        return new String(chars);
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * 游戏结束
     */
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

        // 构建结果
        GameOverResult result = new GameOverResult();
        result.winnerId = winnerId;
        result.loserId = loserId;
        result.winnerName = winner != null ? winner.getNickname() : "";
        result.loserName = loser != null ? loser.getNickname() : "";
        result.trophyChange = TROPHY_GAIN;
        result.winnerHealth = winner != null ? winner.getHealth() : 0;
        result.loserHealth = loser != null ? loser.getHealth() : 0;

        int winnerCorrect = winner != null ? winner.getAnsweredCorrectly() : 0;
        int winnerTotal = winner != null ? Math.max(1, winner.getAnsweredTotal()) : 1;
        result.winnerAccuracy = winnerTotal > 0 ? winnerCorrect * 100 / winnerTotal : 0;

        int loserCorrect = loser != null ? loser.getAnsweredCorrectly() : 0;
        int loserTotal = loser != null ? Math.max(1, loser.getAnsweredTotal()) : 1;
        result.loserAccuracy = loserTotal > 0 ? loserCorrect * 100 / loserTotal : 0;

        int winnerTrophies = trophyService.getTrophies(winnerId);
        int loserTrophies = trophyService.getTrophies(loserId);
        result.winnerTrophiesAfter = winnerTrophies;
        result.loserTrophiesAfter = loserTrophies;

        // 从活跃游戏列表中移除
        activeGames.remove(session.getSessionId());

        log.info("Game over: {} (winner) vs {} - trophies: {} -> {}, {} -> {}",
                winnerId, loserId,
                winner != null ? winner.getTrophies() : 0, winnerTrophies,
                loser != null ? loser.getTrophies() : 0, loserTrophies);

        return result;
    }

    // ==================== Pending Actions（内存状态） ====================

    // 待处理的出牌动作 <sessionId:userId, PendingAction>
    private final Map<String, PendingAction> pendingActions = new ConcurrentHashMap<>();
    // 待处理的攻击 <sessionId:defense:userId, PendingAttack>
    private final Map<String, PendingAttack> pendingAttacks = new ConcurrentHashMap<>();

    public void setPendingAction(String sessionId, Long userId, PendingAction action) {
        pendingActions.put(sessionId + ":" + userId, action);
    }

    public PendingAction getPendingAction(String sessionId, Long userId) {
        return pendingActions.get(sessionId + ":" + userId);
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

    public static class QuestionResult {
        public boolean success;
        public String errorMessage;
        public CardState card;
        public int manaRemaining;
        public String questionData;
        public String questionType;
        public int timeLimit;
        public String cardNameCn;
        public Long cardId;
        public String sentenceText;

        public QuestionResult(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
        }
    }

    public static class AnswerResult {
        public boolean success;
        public String errorMessage;
        public CardState card;
        public int manaRemaining;
        public int comboCount;
        public int comboBonus;
        public int damageDealt;
        public Long targetId;
        public String targetType;
        public boolean gameOver;
        public boolean opponentGainedCard;
        public int opponentHealed;

        public AnswerResult(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
        }
    }

    public static class AttackDeclarationResult {
        public boolean success;
        public String errorMessage;
        public Long attackerId;
        public String attackerName;
        public int attackerAttack;
        public Long targetId;
        public String targetType;
        public String defenseQuestionData;

        public AttackDeclarationResult(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
        }
    }

    public static class DefenseResult {
        public boolean success;
        public String errorMessage;
        public Long attackerId;
        public int damage;
        public boolean defenderCorrect;
        public Long defenderId;
        public int defenderHealthLeft;
        public boolean defenderDead;
        public boolean gameOver;
        public String targetType;
        public boolean divineShieldBlocked;

        public DefenseResult(boolean success, String errorMessage) {
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
        public int winnerAccuracy;
        public int loserAccuracy;
        public int winnerTrophiesAfter;
        public int loserTrophiesAfter;

        public GameOverResult() {}
    }

    public static class PendingAction {
        private CardState card;
        private long timestamp;

        public PendingAction(CardState card) {
            this.card = card;
            this.timestamp = System.currentTimeMillis();
        }

        public CardState getCard() { return card; }
        public long getTimestamp() { return timestamp; }
    }

    public static class PendingAttack {
        private Long attackerCardId;
        private Long targetId;
        private String targetType;
        private Long attackerUserId;

        public PendingAttack(Long attackerCardId, Long targetId, String targetType, Long attackerUserId) {
            this.attackerCardId = attackerCardId;
            this.targetId = targetId;
            this.targetType = targetType;
            this.attackerUserId = attackerUserId;
        }

        public Long getAttackerCardId() { return attackerCardId; }
        public Long getTargetId() { return targetId; }
        public String getTargetType() { return targetType; }
        public Long getAttackerUserId() { return attackerUserId; }
    }
}
