package com.pengyouquan.english.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pengyouquan.english.model.*;
import com.pengyouquan.english.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExpeditionService {

    private final ExpeditionRepository expeditionRepository;
    private final RelicRepository relicRepository;
    private final ExpeditionRelicRepository expeditionRelicRepository;
    private final PlayerRelicRepository playerRelicRepository;
    private final ExpeditionEnemyRepository expeditionEnemyRepository;
    private final ExpeditionEventRepository expeditionEventRepository;
    private final CardRepository cardRepository;
    private final UserCardRepository userCardRepository;
    private final UserRepository userRepository;
    private final SentenceRepository sentenceRepository;
    private final ObjectMapper objectMapper;
    private final AchievementService achievementService;

    // 每层节点序列模板（内部数组表示分支选项，多元素表示岔路）
    private static final Map<Integer, List<List<String>>> ACT_NODE_TEMPLATES = new LinkedHashMap<>();
    static {
        // Act 1: 第2个节点为岔路（事件/战斗），第4个为岔路（战斗/商店）
        ACT_NODE_TEMPLATES.put(1, List.of(
            List.of("combat"),
            List.of("event", "combat"),   // 岔路
            List.of("rest"),
            List.of("combat", "shop"),    // 岔路
            List.of("combat"),
            List.of("boss")
        ));
        ACT_NODE_TEMPLATES.put(2, List.of(
            List.of("combat"),
            List.of("event", "combat"),
            List.of("rest"),
            List.of("shop", "event"),
            List.of("combat"),
            List.of("combat"),
            List.of("boss")
        ));
        ACT_NODE_TEMPLATES.put(3, List.of(
            List.of("combat"),
            List.of("event", "combat"),
            List.of("rest"),
            List.of("shop", "event"),
            List.of("combat"),
            List.of("event", "combat"),
            List.of("boss")
        ));
    }

    // 基础敌人攻击力
    private static final int BASE_ENEMY_ATTACK = 3;
    private static final int STARTING_GOLD = 50;
    private static final int HEAL_PERCENT = 30;

    public ExpeditionService(ExpeditionRepository expeditionRepository,
                             RelicRepository relicRepository,
                             ExpeditionRelicRepository expeditionRelicRepository,
                             PlayerRelicRepository playerRelicRepository,
                             ExpeditionEnemyRepository expeditionEnemyRepository,
                             ExpeditionEventRepository expeditionEventRepository,
                             CardRepository cardRepository,
                             UserCardRepository userCardRepository,
                             UserRepository userRepository,
                             SentenceRepository sentenceRepository,
                             ObjectMapper objectMapper,
                             AchievementService achievementService) {
        this.expeditionRepository = expeditionRepository;
        this.relicRepository = relicRepository;
        this.expeditionRelicRepository = expeditionRelicRepository;
        this.playerRelicRepository = playerRelicRepository;
        this.expeditionEnemyRepository = expeditionEnemyRepository;
        this.expeditionEventRepository = expeditionEventRepository;
        this.cardRepository = cardRepository;
        this.userCardRepository = userCardRepository;
        this.userRepository = userRepository;
        this.sentenceRepository = sentenceRepository;
        this.objectMapper = objectMapper;
        this.achievementService = achievementService;
    }

    // ==================== 1. 启动远征 ====================

    @Transactional
    public Map<String, Object> startExpedition(Long userId, Long showId, List<Long> deckCardIds) {
        // 检查是否有进行中的远征
        Optional<Expedition> existing = expeditionRepository.findByUserIdAndStatus(userId, "in_progress");
        if (existing.isPresent()) {
            throw new IllegalStateException("已有进行中的远征，请先完成或放弃");
        }

        // 验证卡牌存在且属于用户
        List<Long> validCardIds = new ArrayList<>();
        for (Long cardId : deckCardIds) {
            Optional<UserCard> uc = userCardRepository.findByUserIdAndCardId(userId, cardId);
            if (uc.isPresent() && uc.get().getQuantity() > 0) {
                validCardIds.add(cardId);
            }
        }
        if (validCardIds.size() < 5) {
            throw new IllegalStateException("至少需要5张有效卡牌");
        }
        // 限制最多10张且使用实际有效的
        if (validCardIds.size() > 10) {
            validCardIds = validCardIds.subList(0, 10);
        }

        // 生成地图节点（分层结构支持岔路）
        List<List<String>> template = ACT_NODE_TEMPLATES.get(1);
        String mapNodesJson = toJson(template);

        // 创建远征
        Expedition exp = new Expedition();
        exp.setUserId(userId);
        exp.setShowId(showId);
        exp.setAct(1);
        exp.setNode(1);
        exp.setMaxAct(1);
        exp.setPlayerHp(30);
        exp.setMaxHp(30);
        exp.setStartingDeck(toJson(validCardIds));
        exp.setCurrentDeck(toJson(validCardIds));
        exp.setRelics(toJson(new ArrayList<>()));
        exp.setGold(STARTING_GOLD);
        exp.setStatus("in_progress");
        exp.setMapNodes(mapNodesJson);
        exp.setBattleState("{}");
        expeditionRepository.save(exp);

        Map<String, Object> result = new HashMap<>();
        result.put("expedition", buildExpeditionData(exp));
        return result;
    }

    // ==================== 2. 获取远征状态 ====================

    public Map<String, Object> getExpedition(Long userId) {
        Optional<Expedition> expOpt = expeditionRepository.findByUserIdAndStatus(userId, "in_progress");
        if (expOpt.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("hasExpedition", false);
            // 获取历史记录
            result.put("history", getHistory(userId));
            return result;
        }

        Expedition exp = expOpt.get();
        Map<String, Object> result = new HashMap<>();
        result.put("hasExpedition", true);
        result.put("expedition", buildExpeditionData(exp));

        // 根据当前节点类型提供额外信息
        String nodeType = getCurrentNodeType(exp);
        result.put("currentNodeType", nodeType);

        if ("branch".equals(nodeType)) {
            result.put("nodeOptions", getCurrentNodeOptions(exp));
        } else if ("combat".equals(nodeType) || "boss".equals(nodeType)) {
            result.put("enemy", getCurrentEnemyData(exp));
        } else if ("event".equals(nodeType)) {
            result.put("events", getCurrentEventData(exp));
        }

        return result;
    }

    // ==================== 3. 进入战斗节点 ====================

    @Transactional
    public Map<String, Object> enterCombat(Long userId) {
        Expedition exp = getActiveExpedition(userId);
        String nodeType = getCurrentNodeType(exp);

        if (!"combat".equals(nodeType) && !"boss".equals(nodeType)) {
            throw new IllegalStateException("当前节点不是战斗节点");
        }

        // 获取当前层的敌人列表（先按具体 showId 查找，未找到则按系列 showId=1 回退）
        Long lookupShowId = exp.getShowId();
        List<ExpeditionEnemy> enemies = expeditionEnemyRepository.findByShowIdAndAct(lookupShowId, exp.getAct());
        if (enemies.isEmpty() && !lookupShowId.equals(1L)) {
            enemies = expeditionEnemyRepository.findByShowIdAndAct(1L, exp.getAct());
        }
        if (enemies.isEmpty()) {
            throw new IllegalStateException("未找到敌人配置");
        }

        // 计算当前应该使用哪个敌人
        int enemyIndex = getEnemyIndex(exp, enemies);
        if (enemyIndex >= enemies.size()) {
            enemyIndex = enemies.size() - 1;
        }

        ExpeditionEnemy enemy = enemies.get(enemyIndex);
        exp.setCurrentEnemyId(enemy.getId());
        exp.setCurrentEnemyHp(enemy.getHp());
        exp.setBattleState("{}");
        expeditionRepository.save(exp);

        Map<String, Object> result = new HashMap<>();
        result.put("enemy", buildEnemyData(enemy, exp.getCurrentEnemyHp()));
        result.put("expedition", buildExpeditionData(exp));

        // 抽手牌
        List<Map<String, Object>> hand = drawHand(exp);
        result.put("hand", hand);

        return result;
    }

    // ==================== 4. 答题 ====================

    @SuppressWarnings("unchecked")
    @Transactional
    public Map<String, Object> answerQuestion(Long userId, Long sentenceId, String answer, boolean correct) {
        Expedition exp = getActiveExpedition(userId);

        if (exp.getCurrentEnemyId() == null) {
            throw new IllegalStateException("当前没有进行中的战斗");
        }

        // 获取敌人
        Optional<ExpeditionEnemy> enemyOpt = expeditionEnemyRepository.findById(exp.getCurrentEnemyId());
        if (enemyOpt.isEmpty()) {
            throw new IllegalStateException("敌人不存在");
        }
        ExpeditionEnemy enemy = enemyOpt.get();

        // 更新答题统计
        exp.setQuestionsTotal(exp.getQuestionsTotal() + 1);

        // 获取当前手牌中的一张卡
        List<Long> deck = parseJsonList(exp.getCurrentDeck());
        if (deck.isEmpty()) {
            deck = parseJsonList(exp.getStartingDeck());
        }

        // 随机选一张牌做本次攻击
        Random rand = new Random();
        Long cardId = deck.get(rand.nextInt(deck.size()));
        Optional<Card> cardOpt = cardRepository.findById(cardId);

        // 读取升级信息
        Map<Long, Map<String, Integer>> cardUpgrades = getCardUpgrades(exp);

        int cardAttack = cardOpt.map(c -> c.getAttack() != null ? c.getAttack() : 2).orElse(2);
        // 应用篝火升级
        if (cardUpgrades.containsKey(cardId)) {
            cardAttack += cardUpgrades.get(cardId).getOrDefault("attackBonus", 0);
        }

        int damageDealt = 0;
        int damageTaken = 0;
        String resultText = "";

        // 检查遗物效果
        List<Long> relicIds = parseJsonList(exp.getRelics());
        Map<String, Object> relicEffects = getRelicEffects(relicIds);
        // 获取新遗物系统的效果
        Map<String, Integer> newRelicEffects = getNewRelicEffects(exp.getId());

        if (correct) {
            // 答对：造成伤害
            exp.setQuestionsAnswered(exp.getQuestionsAnswered() + 1);
            damageDealt = cardAttack;

            // 新遗物系统：COMBAT_DAMAGE_BOOST
            damageDealt += newRelicEffects.getOrDefault("COMBAT_DAMAGE_BOOST", 0);
            // 新遗物系统：BOSS_DAMAGE_BONUS
            boolean isBossNode = "boss".equals(getCurrentNodeType(exp));
            if (isBossNode) {
                int bossBonus = newRelicEffects.getOrDefault("BOSS_DAMAGE_BONUS", 0);
                if (bossBonus > 0) {
                    damageDealt += (int) Math.round(damageDealt * bossBonus / 100.0);
                }
            }
            // 新遗物系统：DOUBLE_EDGED
            if (newRelicEffects.containsKey("DOUBLE_EDGED")) {
                damageDealt *= newRelicEffects.get("DOUBLE_EDGED");
            }

            // 遗物：龙焰宝珠 额外+2伤害
            if (hasRelicEffect(relicEffects, "extra_damage_on_correct")) {
                damageDealt += getRelicValue(relicEffects, "extra_damage_on_correct", 2);
            }

            // 遗物：无面者面具 上次答错则此伤害×2
            if (hasRelicEffect(relicEffects, "consecutive_damage_boost")) {
                String battleStateStr = exp.getBattleState();
                if (battleStateStr != null) {
                    try {
                        Map<String, Object> state = objectMapper.readValue(battleStateStr, Map.class);
                        if (Boolean.TRUE.equals(state.get("lastWrong"))) {
                            damageDealt *= 2;
                            state.put("lastWrong", false);
                            exp.setBattleState(objectMapper.writeValueAsString(state));
                        }
                    } catch (Exception ignored) {}
                }
            }

            // 应用伤害
            int newHp = exp.getCurrentEnemyHp() - damageDealt;
            exp.setCurrentEnemyHp(Math.max(0, newHp));
            resultText = "答对了！造成 " + damageDealt + " 点伤害！";

            // 新遗物系统：VAMPIRIC - 造成伤害的20%回血
            int vampPct = newRelicEffects.getOrDefault("VAMPIRIC", 0);
            if (vampPct > 0) {
                int vampHeal = Math.max(1, damageDealt * vampPct / 100);
                exp.setPlayerHp(Math.min(exp.getMaxHp(), exp.getPlayerHp() + vampHeal));
                resultText += " 汲取了 " + vampHeal + " 点生命！";
            }

            // 检测敌人是否死亡
            if (exp.getCurrentEnemyHp() <= 0) {
                exp.setEnemiesKilled(exp.getEnemiesKilled() + 1);
                // 掉落金币（新遗物系统：GOLD_BONUS）
                int goldBonus = newRelicEffects.getOrDefault("GOLD_BONUS", 0);
                int goldReward = 5 + rand.nextInt(11) + goldBonus; // 5-15 + bonus
                exp.setGold(exp.getGold() + goldReward);

                String nodeType = getCurrentNodeType(exp);
                boolean isBoss = "boss".equals(nodeType);

                exp.setCurrentEnemyId(null);
                exp.setCurrentEnemyHp(null);
                expeditionRepository.save(exp);

                // 新遗物系统：HEAL_ON_COMBAT_WIN
                int healOnWin = newRelicEffects.getOrDefault("HEAL_ON_COMBAT_WIN", 0);
                if (healOnWin > 0) {
                    exp.setPlayerHp(Math.min(exp.getMaxHp(), exp.getPlayerHp() + healOnWin));
                }

                Map<String, Object> result = new HashMap<>();
                result.put("correct", true);
                result.put("damageDealt", damageDealt);
                result.put("damageTaken", 0);
                result.put("enemyDefeated", true);
                result.put("isBoss", isBoss);
                result.put("goldReward", goldReward);
                result.put("resultText", resultText + " 击败了敌人！获得 " + goldReward + " 金币。");
                result.put("expedition", buildExpeditionData(exp));

                // 如果是Boss战胜利
                if (isBoss) {
                    result.put("bossDefeated", true);
                    result.put("rewards", generateBossRewards(exp));
                    // 成就检查
                    int totalBossKills = expeditionRepository.sumBossKillsByUserId(userId);
                    achievementService.checkByConditionType(userId, "boss_kills", totalBossKills);
                } else {
                    result.put("rewards", generateCombatRewards(exp));
                }
                return result;
            }
        } else {
            // 答错：受到伤害
            damageTaken = BASE_ENEMY_ATTACK;

            // 新遗物系统：DAMAGE_REDUCTION
            int dmgReduction = newRelicEffects.getOrDefault("DAMAGE_REDUCTION", 0);
            damageTaken = Math.max(0, damageTaken - dmgReduction);

            // 新遗物系统：WRONG_PENALTY_REDUCE
            int penaltyReduce = newRelicEffects.getOrDefault("WRONG_PENALTY_REDUCE", 0);
            if (penaltyReduce > 0) {
                damageTaken = Math.max(0, damageTaken * (100 - penaltyReduce) / 100);
            }

            // 新遗物系统：DOUBLE_EDGED - 答错自伤翻倍
            if (newRelicEffects.containsKey("DOUBLE_EDGED")) {
                damageTaken *= newRelicEffects.get("DOUBLE_EDGED");
            }

            // 检查遗物：渡鸦之眼 首次答错不扣血
            if (hasRelicEffect(relicEffects, "first_mistake_no_damage")) {
                String battleStateStr = exp.getBattleState();
                try {
                    Map<String, Object> state = battleStateStr != null ?
                            objectMapper.readValue(battleStateStr, Map.class) : new HashMap<>();
                    if (!Boolean.TRUE.equals(state.get("firstMistakeUsed"))) {
                        damageTaken = 0;
                        state.put("firstMistakeUsed", true);
                        exp.setBattleState(objectMapper.writeValueAsString(state));
                    }
                } catch (Exception ignored) {}
            }

            // 遗物：无面者面具 - 记录答错
            if (hasRelicEffect(relicEffects, "consecutive_damage_boost")) {
                try {
                    Map<String, Object> state = objectMapper.readValue(exp.getBattleState(), Map.class);
                    state.put("lastWrong", true);
                    exp.setBattleState(objectMapper.writeValueAsString(state));
                } catch (Exception ignored) {}
            }

            int newHp = exp.getPlayerHp() - damageTaken;
            exp.setPlayerHp(Math.max(0, newHp));
            resultText = "答错了！受到 " + damageTaken + " 点伤害！";
        }

        // 检查玩家是否死亡
        if (exp.getPlayerHp() <= 0) {
            exp.setStatus("dead");
            clearPlayerRelics(exp.getId());
            expeditionRepository.save(exp);

            Map<String, Object> result = new HashMap<>();
            result.put("correct", correct);
            result.put("damageDealt", damageDealt);
            result.put("damageTaken", damageTaken);
            result.put("enemyDefeated", false);
            result.put("playerDead", true);
            result.put("resultText", "远征结束！你已阵亡...");
            result.put("expedition", buildExpeditionData(exp));
            return result;
        }

        expeditionRepository.save(exp);

        Map<String, Object> result = new HashMap<>();
        result.put("correct", correct);
        result.put("damageDealt", damageDealt);
        result.put("damageTaken", damageTaken);
        result.put("enemyDefeated", false);
        result.put("playerDead", false);
        result.put("enemyRemainingHp", exp.getCurrentEnemyHp());
        result.put("enemyMaxHp", enemy.getHp());
        result.put("resultText", resultText);
        result.put("cardUsed", cardOpt.map(c -> Map.of("id", c.getId(), "nameCn", c.getNameCn(), "attack", c.getAttack())).orElse(null));
        result.put("expedition", buildExpeditionData(exp));
        return result;
    }

    // ==================== 5. 奖励选择 ====================

    @Transactional
    public Map<String, Object> chooseReward(Long userId, int choiceIndex) {
        Expedition exp = getActiveExpedition(userId);
        // 这里选择奖励类型，由前端在战斗胜利后调用
        // 实际处理在应用奖励时做
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("expedition", buildExpeditionData(exp));
        return result;
    }

    // ==================== 6. 进入事件 ====================

    @SuppressWarnings("unchecked")
    @Transactional
    public Map<String, Object> enterEvent(Long userId, int choiceIndex) {
        Expedition exp = getActiveExpedition(userId);
        String nodeType = getCurrentNodeType(exp);

        if (!"event".equals(nodeType)) {
            throw new IllegalStateException("当前节点不是事件节点");
        }

        List<ExpeditionEvent> events = expeditionEventRepository.findByShowIdAndAct(exp.getShowId(), exp.getAct());
        // 按具体 showId 未找到，回退到系列 showId
        if (events.isEmpty() && !exp.getShowId().equals(1L)) {
            events = expeditionEventRepository.findByShowIdAndAct(1L, exp.getAct());
        }
        if (events.isEmpty()) {
            throw new IllegalStateException("未找到事件配置");
        }

        // 根据节点位置选择事件
        int eventIndex = (exp.getNode() - 1) % events.size();
        if (eventIndex >= events.size()) eventIndex = 0;
        ExpeditionEvent event = events.get(eventIndex);

        // 解析选项
        List<Map<String, Object>> choices;
        try {
            choices = objectMapper.readValue(event.getChoices(), List.class);
        } catch (Exception e) {
            throw new IllegalStateException("事件配置解析失败");
        }

        if (choiceIndex < 0 || choiceIndex >= choices.size()) {
            throw new IllegalStateException("无效的选择");
        }

        Map<String, Object> chosen = choices.get(choiceIndex);
        Map<String, Object> effect = (Map<String, Object>) chosen.get("effect");
        String type = (String) effect.get("type");
        String resultText = (String) chosen.get("text");

        // 执行效果
        Map<String, Object> effectResult = applyEventEffect(exp, type, effect);

        expeditionRepository.save(exp);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("resultText", resultText);
        result.put("effectResult", effectResult);
        result.put("expedition", buildExpeditionData(exp));
        return result;
    }

    // ==================== 7. 休息 ====================

    @Transactional
    public Map<String, Object> enterRest(Long userId, String action, Long cardId) {
        Expedition exp = getActiveExpedition(userId);
        String nodeType = getCurrentNodeType(exp);

        if (!"rest".equals(nodeType)) {
            throw new IllegalStateException("当前节点不是休息节点");
        }

        // 检查遗物：蜂蜜酒 回血加倍
        List<Long> relicIds = parseJsonList(exp.getRelics());
        Map<String, Object> relicEffects = getRelicEffects(relicIds);
        int healMultiplier = hasRelicEffect(relicEffects, "rest_heal_bonus") ?
                getRelicValue(relicEffects, "rest_heal_bonus", 2) : 1;

        int healAmount = (int) Math.ceil(exp.getMaxHp() * HEAL_PERCENT / 100.0 * healMultiplier);

        if ("heal".equals(action)) {
            int newHp = Math.min(exp.getMaxHp(), exp.getPlayerHp() + healAmount);
            exp.setPlayerHp(newHp);
        } else if ("upgrade".equals(action) && cardId != null) {
            // 升级一张卡牌：+1攻击力或+1生命值（由upgradeType决定，默认攻击）
            List<Long> deck = parseJsonList(exp.getCurrentDeck());
            boolean found = false;
            for (int i = 0; i < deck.size(); i++) {
                if (deck.get(i).equals(cardId)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new IllegalStateException("卡牌不在当前牌组中");
            }
            // 找原卡升级
            Optional<Card> cardOpt = cardRepository.findById(cardId);
            if (cardOpt.isPresent()) {
                // 在 battle_state 中记录升级信息
                try {
                    Map<String, Object> upgrades = new HashMap<>();
                    if (exp.getBattleState() != null && !"{}".equals(exp.getBattleState())) {
                        upgrades = objectMapper.readValue(exp.getBattleState(), Map.class);
                    }
                    if (!upgrades.containsKey("upgradedCards")) {
                        upgrades.put("upgradedCards", new HashMap<String, Map<String, Integer>>());
                    }
                    Map<String, Map<String, Integer>> upgradedCards = (Map<String, Map<String, Integer>>) upgrades.get("upgradedCards");
                    String cardKey = String.valueOf(cardId);
                    Map<String, Integer> cardUpgrade = upgradedCards.getOrDefault(cardKey, new HashMap<>());
                    cardUpgrade.put("attackBonus", cardUpgrade.getOrDefault("attackBonus", 0) + 1);
                    cardUpgrade.put("healthBonus", cardUpgrade.getOrDefault("healthBonus", 0) + 1);
                    upgradedCards.put(cardKey, cardUpgrade);
                    exp.setBattleState(objectMapper.writeValueAsString(upgrades));
                } catch (Exception ignored) {}
            }
        }

        expeditionRepository.save(exp);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        if ("heal".equals(action)) {
            result.put("healAmount", healAmount);
        } else if ("upgrade".equals(action)) {
            result.put("upgradedCardId", cardId);
            result.put("attackBonus", 1);
            result.put("healthBonus", 1);
            result.put("upgradeMessage", "卡牌已强化！攻击+1，生命+1");
        }
        result.put("expedition", buildExpeditionData(exp));
        return result;
    }

    /**
     * 获取卡牌升级信息（从 battle_state 读取）
     */
    @SuppressWarnings("unchecked")
    private Map<Long, Map<String, Integer>> getCardUpgrades(Expedition exp) {
        Map<Long, Map<String, Integer>> upgrades = new HashMap<>();
        try {
            String bs = exp.getBattleState();
            if (bs == null || bs.isBlank() || "{}".equals(bs)) return upgrades;
            Map<String, Object> state = objectMapper.readValue(bs, Map.class);
            Object upgradedObj = state.get("upgradedCards");
            if (upgradedObj instanceof Map) {
                Map<String, Map<String, Integer>> upgradedCards = (Map<String, Map<String, Integer>>) upgradedObj;
                for (Map.Entry<String, Map<String, Integer>> entry : upgradedCards.entrySet()) {
                    Long cardId = Long.parseLong(entry.getKey());
                    upgrades.put(cardId, entry.getValue());
                }
            } else if (upgradedObj instanceof List) {
                // 向后兼容旧格式（List<Long>）
                List<Object> oldList = (List<Object>) upgradedObj;
                for (Object o : oldList) {
                    if (o instanceof Number) {
                        Long cid = ((Number) o).longValue();
                        upgrades.put(cid, new HashMap<>(Map.of("attackBonus", 2, "healthBonus", 0)));
                    }
                }
            }
        } catch (Exception ignored) {}
        return upgrades;
    }

    // ==================== 8. 商店 ====================

    public Map<String, Object> enterShop(Long userId) {
        Expedition exp = getActiveExpedition(userId);
        String nodeType = getCurrentNodeType(exp);

        if (!"shop".equals(nodeType)) {
            throw new IllegalStateException("当前节点不是商店");
        }

        // 生成可购买物品
        Random rand = new Random();
        List<Map<String, Object>> items = new ArrayList<>();

        // 卡牌（3张随机卡）
        List<Card> allCards = cardRepository.findByShowId(exp.getShowId());
        if (!allCards.isEmpty()) {
            List<Card> shuffled = new ArrayList<>(allCards);
            Collections.shuffle(shuffled, rand);
            for (int i = 0; i < Math.min(3, shuffled.size()); i++) {
                Card c = shuffled.get(i);
                Map<String, Object> item = new HashMap<>();
                item.put("type", "card");
                item.put("id", c.getId());
                item.put("nameCn", c.getNameCn());
                item.put("nameEn", c.getNameEn());
                item.put("rarity", c.getRarity());
                item.put("attack", c.getAttack());
                item.put("cost", c.getCost() != null ? c.getCost() * 5 + 10 : 20);
                items.add(item);
            }
        }

        // 遗物（1个随机）
        List<Relic> relics = relicRepository.findBySource("shop");
        if (!relics.isEmpty()) {
            Relic r = relics.get(rand.nextInt(relics.size()));
            Map<String, Object> item = new HashMap<>();
            item.put("type", "relic");
            item.put("id", r.getId());
            item.put("nameCn", r.getNameCn());
            item.put("nameEn", r.getNameEn());
            item.put("rarity", r.getRarity());
            item.put("effectCn", r.getEffectCn());
            item.put("cost", "common".equals(r.getRarity()) ? 30 :
                    "rare".equals(r.getRarity()) ? 60 : 100);
            items.add(item);
        }

        // 回血
        Map<String, Object> healItem = new HashMap<>();
        healItem.put("type", "heal");
        healItem.put("id", 0);
        healItem.put("nameCn", "治疗药水");
        healItem.put("healAmount", 15);
        healItem.put("cost", 25);
        items.add(healItem);

        // 删卡选项：花金币移除一张手牌
        List<Long> deck = parseJsonList(exp.getCurrentDeck());
        if (deck.size() > 3) {
            Map<String, Object> removeItem = new HashMap<>();
            removeItem.put("type", "remove");
            removeItem.put("id", 1);
            removeItem.put("nameCn", "删卡服务");
            removeItem.put("description", "移除一张卡牌（压缩牌组）");
            removeItem.put("cost", 30);
            items.add(removeItem);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("items", items);
        result.put("gold", exp.getGold());
        return result;
    }

    @Transactional
    public Map<String, Object> buyItem(Long userId, String type, Long itemId) {
        Expedition exp = getActiveExpedition(userId);
        int cost = 0;

        if ("card".equals(type)) {
            Optional<Card> cardOpt = cardRepository.findById(itemId);
            if (cardOpt.isEmpty()) throw new IllegalStateException("卡牌不存在");
            Card card = cardOpt.get();
            cost = card.getCost() != null ? card.getCost() * 5 + 10 : 20;

            if (exp.getGold() < cost) throw new IllegalStateException("金币不足");
            exp.setGold(exp.getGold() - cost);

            List<Long> deck = parseJsonList(exp.getCurrentDeck());
            deck.add(itemId);
            exp.setCurrentDeck(toJson(deck));

        } else if ("remove".equals(type)) {
            cost = 30;
            if (exp.getGold() < cost) throw new IllegalStateException("金币不足");

            List<Long> deck = parseJsonList(exp.getCurrentDeck());
            if (deck.isEmpty()) throw new IllegalStateException("牌组已空");
            // itemId 为要移除的卡牌ID，如果为0或null则移除最后一张
            Long removeCardId = itemId;
            if (removeCardId == null || removeCardId == 0) {
                deck.remove(deck.size() - 1);
            } else {
                boolean removed = deck.remove(removeCardId);
                if (!removed) throw new IllegalStateException("卡牌不在牌组中");
            }
            exp.setGold(exp.getGold() - cost);
            exp.setCurrentDeck(toJson(deck));

        } else if ("relic".equals(type)) {
            Optional<Relic> relicOpt = relicRepository.findById(itemId);
            if (relicOpt.isEmpty()) throw new IllegalStateException("遗物不存在");
            Relic relic = relicOpt.get();
            cost = "common".equals(relic.getRarity()) ? 30 :
                    "rare".equals(relic.getRarity()) ? 60 : 100;

            if (exp.getGold() < cost) throw new IllegalStateException("金币不足");
            exp.setGold(exp.getGold() - cost);

            List<Long> relics = parseJsonList(exp.getRelics());
            relics.add(itemId);
            exp.setRelics(toJson(relics));

        } else if ("heal".equals(type)) {
            cost = 25;
            if (exp.getGold() < cost) throw new IllegalStateException("金币不足");
            exp.setGold(exp.getGold() - cost);
            int newHp = Math.min(exp.getMaxHp(), exp.getPlayerHp() + 15);
            exp.setPlayerHp(newHp);
        } else {
            throw new IllegalStateException("无效的商品类型");
        }

        expeditionRepository.save(exp);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("gold", exp.getGold());
        result.put("expedition", buildExpeditionData(exp));
        return result;
    }

    // ==================== 9. 选择岔路 ====================

    @Transactional
    public Map<String, Object> choosePath(Long userId, int choiceIndex) {
        Expedition exp = getActiveExpedition(userId);
        List<List<String>> rows = parseJsonNodeRows(exp.getMapNodes());
        int idx = exp.getNode() - 1;
        if (idx >= rows.size()) {
            throw new IllegalStateException("当前没有岔路可选");
        }
        List<String> row = rows.get(idx);
        if (row.size() <= 1) {
            throw new IllegalStateException("当前节点不是岔路");
        }
        if (choiceIndex < 0 || choiceIndex >= row.size()) {
            throw new IllegalStateException("无效的选择");
        }
        setNodeChoice(exp, idx, choiceIndex);
        expeditionRepository.save(exp);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("expedition", buildExpeditionData(exp));
        result.put("nodeType", getCurrentNodeType(exp));
        return result;
    }

    // ==================== 10. 下一个节点 ====================

    @Transactional
    public Map<String, Object> nextNode(Long userId) {
        Expedition exp = getActiveExpedition(userId);
        List<List<String>> rows = parseJsonNodeRows(exp.getMapNodes());

        int nextNode = exp.getNode() + 1;
        if (nextNode > rows.size()) {
            // 当前层完成，进入下一层
            if (exp.getAct() >= 3) {
                // 通关！
                exp.setStatus("cleared");
                // 结算奖励
                applyClearRewards(exp);
                expeditionRepository.save(exp);

                // 成就检查
                long clearCount = expeditionRepository.countByUserIdAndStatus(userId, "cleared");
                achievementService.checkByConditionType(userId, "expedition_clear", (int) clearCount);

                Map<String, Object> result = new HashMap<>();
                result.put("expedition", buildExpeditionData(exp));
                result.put("cleared", true);
                return result;
            }

            // 进入下一层
            int nextAct = exp.getAct() + 1;
            exp.setAct(nextAct);
            exp.setMaxAct(nextAct);
            exp.setNode(1);
            exp.setMapNodes(toJson(ACT_NODE_TEMPLATES.get(nextAct)));

            // 检查遗物：铁王座碎片 Boss战后回满血
            List<Long> relicIds = parseJsonList(exp.getRelics());
            Map<String, Object> relicEffects = getRelicEffects(relicIds);
            if (hasRelicEffect(relicEffects, "full_heal_after_boss")) {
                exp.setPlayerHp(exp.getMaxHp());
            }
        } else {
            exp.setNode(nextNode);
        }

        expeditionRepository.save(exp);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("expedition", buildExpeditionData(exp));
        result.put("nodeType", getCurrentNodeType(exp));
        return result;
    }

    // ==================== 10. 获取奖励选项 ====================

    public Map<String, Object> getRewardChoices(Long userId) {
        Expedition exp = getActiveExpedition(userId);
        // 获取该show的可用卡牌
        List<Card> availableCards = cardRepository.findByShowId(exp.getShowId());
        if (availableCards.isEmpty()) {
            availableCards = cardRepository.findAll();
        }

        Random rand = new Random();
        List<Map<String, Object>> choices = new ArrayList<>();

        // 选项1: 获得一张新卡
        List<Card> shuffled = new ArrayList<>(availableCards);
        Collections.shuffle(shuffled, rand);
        Map<String, Object> cardChoice = new HashMap<>();
        cardChoice.put("type", "card");
        Card rewardCard = shuffled.get(0);
        cardChoice.put("card", Map.of(
                "id", rewardCard.getId(),
                "nameCn", rewardCard.getNameCn(),
                "nameEn", rewardCard.getNameEn(),
                "rarity", rewardCard.getRarity(),
                "attack", rewardCard.getAttack()
        ));
        cardChoice.put("label", "获得卡牌：" + rewardCard.getNameCn());
        choices.add(cardChoice);

        // 选项2: 回血
        Map<String, Object> healChoice = new HashMap<>();
        healChoice.put("type", "heal");
        healChoice.put("healAmount", 8);
        healChoice.put("label", "回复8点生命值");
        choices.add(healChoice);

        // 选项3: 移除一张牌
        List<Long> deck = parseJsonList(exp.getCurrentDeck());
        if (deck.size() > 3) {
            Map<String, Object> removeChoice = new HashMap<>();
            removeChoice.put("type", "remove");
            removeChoice.put("label", "从牌组中移除一张牌（精简牌组）");
            choices.add(removeChoice);
        } else {
            // 替代：获得金币
            Map<String, Object> goldChoice = new HashMap<>();
            goldChoice.put("type", "gold");
            goldChoice.put("value", 15);
            goldChoice.put("label", "获得15金币");
            choices.add(goldChoice);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("choices", choices);
        return result;
    }

    // ==================== 11. 应用奖励 ====================

    @SuppressWarnings("unchecked")
    @Transactional
    public Map<String, Object> applyReward(Long userId, Map<String, Object> rewardData) {
        Expedition exp = getActiveExpedition(userId);
        String type = (String) rewardData.get("type");

        if ("card".equals(type)) {
            Map<String, Object> cardData = (Map<String, Object>) rewardData.get("card");
            if (cardData != null && cardData.get("id") instanceof Number) {
                Long cardId = ((Number) cardData.get("id")).longValue();
                List<Long> deck = parseJsonList(exp.getCurrentDeck());
                deck.add(cardId);
                exp.setCurrentDeck(toJson(deck));
            }
        } else if ("new_relic".equals(type)) {
            Number relicIdNum = (Number) rewardData.get("relicId");
            if (relicIdNum != null) {
                Long relicId = relicIdNum.longValue();
                addRelicToPlayer(exp, relicId);
            }
        } else if ("heal".equals(type)) {
            int healAmount = rewardData.get("healAmount") instanceof Number ?
                    ((Number) rewardData.get("healAmount")).intValue() : 8;
            exp.setPlayerHp(Math.min(exp.getMaxHp(), exp.getPlayerHp() + healAmount));
        } else if ("remove".equals(type)) {
            List<Long> deck = parseJsonList(exp.getCurrentDeck());
            if (!deck.isEmpty()) {
                deck.remove(deck.size() - 1);
                exp.setCurrentDeck(toJson(deck));
            }
        } else if ("gold".equals(type)) {
            int goldVal = rewardData.get("value") instanceof Number ?
                    ((Number) rewardData.get("value")).intValue() : 15;
            exp.setGold(exp.getGold() + goldVal);
        }

        expeditionRepository.save(exp);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("expedition", buildExpeditionData(exp));
        return result;
    }

    // ==================== 12. 放弃远征 ====================

    @Transactional
    public Map<String, Object> abandonExpedition(Long userId) {
        Expedition exp = getActiveExpedition(userId);
        exp.setStatus("dead");
        // 清除远征的遗物
        clearPlayerRelics(exp.getId());
        expeditionRepository.save(exp);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return result;
    }

    // ==================== 获取随机句子 ====================

    public Map<String, Object> getRandomSentence(Long userId) {
        Expedition exp = getActiveExpedition(userId);
        List<Sentence> sentences = sentenceRepository.findRandomByShowId(exp.getShowId(), 1);
        if (sentences.isEmpty()) {
            sentences = sentenceRepository.findRandom(1);
        }

        if (sentences.isEmpty()) {
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("text", "Nothing is everything.");
            fallback.put("translation", "一切皆有可能。");
            return fallback;
        }

        Sentence s = sentences.get(0);
        Map<String, Object> result = new HashMap<>();
        result.put("id", s.getId());
        result.put("text", s.getText());
        result.put("showId", s.getShowId());
        return result;
    }

    // ==================== 内部方法 ====================

    private Expedition getActiveExpedition(Long userId) {
        return expeditionRepository.findByUserIdAndStatus(userId, "in_progress")
                .orElseThrow(() -> new IllegalStateException("没有进行中的远征"));
    }

    /**
     * 获取当前行的节点类型（考虑了分支选择）
     * 如果当前行有多个节点（岔路），从 battle_state 中读取已选分支
     */
    private String getCurrentNodeType(Expedition exp) {
        List<List<String>> rows = parseJsonNodeRows(exp.getMapNodes());
        if (rows.isEmpty()) return "combat";
        int idx = exp.getNode() - 1;
        if (idx >= rows.size()) return "boss";
        List<String> row = rows.get(idx);
        if (row.isEmpty()) return "combat";
        if (row.size() == 1) {
            return row.get(0);
        }
        // 岔路：从 battle_state 读取分支选择
        int choice = getNodeChoice(exp, idx);
        if (choice < 0 || choice >= row.size()) {
            // 未选择时返回 "branch" 让前端展示选项
            return "branch";
        }
        return row.get(choice);
    }

    /**
     * 获取当前行的所有可选节点（岔路选项）
     */
    private List<String> getCurrentNodeOptions(Expedition exp) {
        List<List<String>> rows = parseJsonNodeRows(exp.getMapNodes());
        if (rows.isEmpty()) return List.of();
        int idx = exp.getNode() - 1;
        if (idx >= rows.size()) return List.of();
        return rows.get(idx);
    }

    /**
     * 从 battle_state 中读取指定行的分支选择
     */
    private int getNodeChoice(Expedition exp, int rowIndex) {
        try {
            String bs = exp.getBattleState();
            if (bs == null || bs.isBlank() || "{}".equals(bs)) return -1;
            Map<String, Object> state = objectMapper.readValue(bs, Map.class);
            Object choices = state.get("nodeChoices");
            if (choices instanceof Map) {
                Number choice = (Number) ((Map) choices).get(String.valueOf(rowIndex));
                return choice != null ? choice.intValue() : -1;
            }
        } catch (Exception ignored) {}
        return -1;
    }

    /**
     * 记录分支选择到 battle_state
     */
    private void setNodeChoice(Expedition exp, int rowIndex, int choice) {
        try {
            String bs = exp.getBattleState();
            Map<String, Object> state;
            if (bs == null || bs.isBlank() || "{}".equals(bs)) {
                state = new HashMap<>();
            } else {
                state = objectMapper.readValue(bs, Map.class);
            }
            Map<String, Object> choices = (Map<String, Object>) state.computeIfAbsent("nodeChoices", k -> new HashMap<String, Object>());
            choices.put(String.valueOf(rowIndex), choice);
            exp.setBattleState(objectMapper.writeValueAsString(state));
        } catch (Exception ignored) {}
    }

    private int getEnemyIndex(Expedition exp, List<ExpeditionEnemy> enemies) {
        List<List<String>> rows = parseJsonNodeRows(exp.getMapNodes());
        int combatIndex = 0;
        for (int i = 0; i < exp.getNode() - 1 && i < rows.size(); i++) {
            List<String> row = rows.get(i);
            String nt = row.isEmpty() ? "combat" : row.get(0);
            if (row.size() > 1) {
                // 岔路：使用已选分支
                int choice = getNodeChoice(exp, i);
                if (choice >= 0 && choice < row.size()) {
                    nt = row.get(choice);
                }
            }
            if ("combat".equals(nt)) combatIndex++;
        }
        // 确保不会超出非Boss敌人数量
        long nonBossCount = enemies.stream().filter(e -> !e.getIsBoss()).count();
        if (combatIndex >= nonBossCount) combatIndex = (int) nonBossCount - 1;
        return Math.max(0, combatIndex);
    }

    private Map<String, Object> getCurrentEnemyData(Expedition exp) {
        if (exp.getCurrentEnemyId() == null) return null;
        Optional<ExpeditionEnemy> enemyOpt = expeditionEnemyRepository.findById(exp.getCurrentEnemyId());
        if (enemyOpt.isEmpty()) return null;
        return buildEnemyData(enemyOpt.get(), exp.getCurrentEnemyHp());
    }

    private List<Map<String, Object>> getCurrentEventData(Expedition exp) {
        List<ExpeditionEvent> events = expeditionEventRepository.findByShowIdAndAct(exp.getShowId(), exp.getAct());
        // 按具体 showId 未找到，回退到系列 showId
        if (events.isEmpty() && !exp.getShowId().equals(1L)) {
            events = expeditionEventRepository.findByShowIdAndAct(1L, exp.getAct());
        }
        if (events.isEmpty()) return List.of();

        int eventIndex = (exp.getNode() - 1) % events.size();
        if (eventIndex >= events.size()) eventIndex = 0;
        ExpeditionEvent event = events.get(eventIndex);

        try {
            return objectMapper.readValue(event.getChoices(), List.class);
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<Map<String, Object>> drawHand(Expedition exp) {
        List<Long> deck = parseJsonList(exp.getCurrentDeck());
        if (deck.isEmpty()) return List.of();

        Random rand = new Random();
        // 新遗物系统：EXTRA_DRAW
        Map<String, Integer> newRelicEffects = getNewRelicEffects(exp.getId());
        int extraDraw = newRelicEffects.getOrDefault("EXTRA_DRAW", 0);
        int baseHandSize = 4 + extraDraw;
        int handSize = Math.min(baseHandSize, deck.size());

        // 读取卡牌升级信息
        Map<Long, Map<String, Integer>> cardUpgrades = getCardUpgrades(exp);

        // 手牌：随机抽取
        List<Long> drawn = new ArrayList<>(deck);
        Collections.shuffle(drawn, rand);
        drawn = drawn.subList(0, handSize);

        List<Map<String, Object>> hand = new ArrayList<>();
        for (Long cardId : drawn) {
            Optional<Card> cardOpt = cardRepository.findById(cardId);
            if (cardOpt.isPresent()) {
                Card c = cardOpt.get();
                Map<String, Object> cardData = new HashMap<>();
                cardData.put("id", c.getId());
                cardData.put("nameCn", c.getNameCn());
                cardData.put("nameEn", c.getNameEn());

                int baseAttack = c.getAttack() != null ? c.getAttack() : 2;
                int baseCost = c.getCost() != null ? c.getCost() : 1;

                // 应用升级
                if (cardUpgrades.containsKey(cardId)) {
                    Map<String, Integer> upgrade = cardUpgrades.get(cardId);
                    baseAttack += upgrade.getOrDefault("attackBonus", 0);
                }

                cardData.put("attack", baseAttack);
                cardData.put("cost", baseCost);
                cardData.put("rarity", c.getRarity());
                cardData.put("cardType", c.getCardType());
                hand.add(cardData);
            }
        }

        // 存入battle_state
        try {
            Map<String, Object> state = new HashMap<>();
            state.put("hand", hand);
            exp.setBattleState(objectMapper.writeValueAsString(state));
            expeditionRepository.save(exp);
        } catch (Exception ignored) {}

        return hand;
    }

    private Map<String, Object> buildEnemyData(ExpeditionEnemy enemy, Integer currentHp) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", enemy.getId());
        data.put("nameCn", enemy.getNameCn());
        data.put("nameEn", enemy.getNameEn());
        data.put("maxHp", enemy.getHp());
        data.put("currentHp", currentHp != null ? currentHp : enemy.getHp());
        data.put("isBoss", enemy.getIsBoss());
        try {
            if (enemy.getSpecialRules() != null && !"{}".equals(enemy.getSpecialRules())) {
                data.put("specialRules", objectMapper.readValue(enemy.getSpecialRules(), Map.class));
            }
        } catch (Exception ignored) {}
        return data;
    }

    private List<Map<String, Object>> generateCombatRewards(Expedition exp) {
        List<Map<String, Object>> rewards = new ArrayList<>();
        Random rand = new Random();

        // 基于show_id获取可用卡牌
        List<Card> cards = cardRepository.findByShowId(exp.getShowId());
        if (cards.isEmpty()) {
            cards = cardRepository.findAll();
        }
        List<Card> shuffled = new ArrayList<>(cards);
        Collections.shuffle(shuffled, rand);

        // 三选一
        // 1. 卡牌
        if (!shuffled.isEmpty()) {
            Map<String, Object> cardReward = new HashMap<>();
            cardReward.put("type", "card");
            Card c = shuffled.get(0);
            cardReward.put("card", cardToMap(c));
            cardReward.put("label", "获得卡牌：" + c.getNameCn());
            rewards.add(cardReward);
        }

        // 2. 回血
        Map<String, Object> heal = new HashMap<>();
        heal.put("type", "heal");
        heal.put("healAmount", 8);
        heal.put("label", "回复8点生命值");
        rewards.add(heal);

        // 3. 移除卡牌或金币 或 遗物（随机）
        int roll = rand.nextInt(10);
        if (roll < 3) {
            // 30% 几率掉落遗物
            List<ExpeditionRelic> allRelics = expeditionRelicRepository.findByShowIdOrShowIdIsNull(exp.getShowId());
            if (!allRelics.isEmpty()) {
                // 过滤掉已拥有的遗物
                List<PlayerRelic> owned = playerRelicRepository.findByExpeditionId(exp.getId());
                Set<Long> ownedIds = owned.stream().map(PlayerRelic::getRelicId).collect(Collectors.toSet());
                List<ExpeditionRelic> available = allRelics.stream()
                        .filter(r -> !ownedIds.contains(r.getId())).collect(Collectors.toList());
                if (!available.isEmpty()) {
                    ExpeditionRelic relic = available.get(rand.nextInt(available.size()));
                    Map<String, Object> relicReward = new HashMap<>();
                    relicReward.put("type", "new_relic");
                    relicReward.put("relicId", relic.getId());
                    relicReward.put("relic", relicToNewMap(relic));
                    relicReward.put("label", relic.getIcon() + " " + relic.getNameCn());
                    rewards.add(relicReward);
                } else {
                    // 已拥有全部遗物，改为金币
                    Map<String, Object> gold = new HashMap<>();
                    gold.put("type", "gold");
                    gold.put("value", 15);
                    gold.put("label", "获得15金币");
                    rewards.add(gold);
                }
            } else {
                Map<String, Object> gold = new HashMap<>();
                gold.put("type", "gold");
                gold.put("value", 15);
                gold.put("label", "获得15金币");
                rewards.add(gold);
            }
        } else {
            List<Long> deck = parseJsonList(exp.getCurrentDeck());
            if (deck.size() > 3) {
                Map<String, Object> remove = new HashMap<>();
                remove.put("type", "remove");
                remove.put("label", "从牌组移除一张牌");
                rewards.add(remove);
            } else {
                Map<String, Object> gold = new HashMap<>();
                gold.put("type", "gold");
                gold.put("value", 15);
                gold.put("label", "获得15金币");
                rewards.add(gold);
            }
        }

        return rewards;
    }

    private List<Map<String, Object>> generateBossRewards(Expedition exp) {
        List<Map<String, Object>> rewards = new ArrayList<>();
        Random rand = new Random();

        // Boss固定奖励：稀有卡牌 + 遗物 + 大量金币
        List<Card> cards = cardRepository.findByShowId(exp.getShowId());
        if (!cards.isEmpty()) {
            List<Card> shuffled = new ArrayList<>(cards);
            Collections.shuffle(shuffled, rand);
            Map<String, Object> cardReward = new HashMap<>();
            cardReward.put("type", "card");
            Card c = shuffled.get(0);
            cardReward.put("card", cardToMap(c));
            cardReward.put("label", "获得稀有卡牌：" + c.getNameCn());
            rewards.add(cardReward);
        }

        // 遗物（从旧表 relics）
        List<Relic> bossRelics = relicRepository.findBySource("boss");
        if (!bossRelics.isEmpty()) {
            Relic r = bossRelics.get(rand.nextInt(bossRelics.size()));
            Map<String, Object> relicReward = new HashMap<>();
            relicReward.put("type", "relic_add");
            relicReward.put("relic", relicToMap(r));
            relicReward.put("label", "获得遗物：" + r.getNameCn());
            rewards.add(relicReward);
        }

        // 遗物（从新表 expedition_relics）
        List<ExpeditionRelic> newRelics = expeditionRelicRepository.findByShowIdOrShowIdIsNull(exp.getShowId());
        if (!newRelics.isEmpty()) {
            List<PlayerRelic> owned = playerRelicRepository.findByExpeditionId(exp.getId());
            Set<Long> ownedIds = owned.stream().map(PlayerRelic::getRelicId).collect(Collectors.toSet());
            List<ExpeditionRelic> available = newRelics.stream()
                    .filter(r -> !ownedIds.contains(r.getId())).collect(Collectors.toList());
            if (!available.isEmpty()) {
                ExpeditionRelic er = available.get(rand.nextInt(available.size()));
                Map<String, Object> newRelicReward = new HashMap<>();
                newRelicReward.put("type", "new_relic");
                newRelicReward.put("relicId", er.getId());
                newRelicReward.put("relic", relicToNewMap(er));
                newRelicReward.put("label", er.getIcon() + " " + er.getNameCn() + "（Boss掉落）");
                rewards.add(newRelicReward);
            }
        }

        Map<String, Object> gold = new HashMap<>();
        gold.put("type", "gold");
        gold.put("value", 30);
        gold.put("label", "获得30金币");
        rewards.add(gold);

        return rewards;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> applyEventEffect(Expedition exp, String type, Map<String, Object> effect) {
        Map<String, Object> result = new HashMap<>();

        switch (type) {
            case "stardust": {
                int value = ((Number) effect.getOrDefault("value", 0)).intValue();
                User user = userRepository.findById(exp.getUserId()).orElse(null);
                if (user != null) {
                    user.setStardust(user.getStardust() + value);
                    userRepository.save(user);
                }
                result.put("stardustGained", value);
                break;
            }
            case "gold": {
                int value = ((Number) effect.getOrDefault("value", 0)).intValue();
                exp.setGold(exp.getGold() + value);
                result.put("goldGained", value);
                break;
            }
            case "heal": {
                int value = ((Number) effect.getOrDefault("value", 0)).intValue();
                exp.setPlayerHp(Math.min(exp.getMaxHp(), exp.getPlayerHp() + value));
                result.put("healed", value);
                break;
            }
            case "swap_card": {
                String rarity = (String) effect.get("rarity");
                List<Card> cards = cardRepository.findByShowId(exp.getShowId());
                if (!cards.isEmpty()) {
                    List<Card> filtered = cards.stream()
                            .filter(c -> rarity == null || rarity.equals(c.getRarity()))
                            .collect(Collectors.toList());
                    if (!filtered.isEmpty()) {
                        Random rand = new Random();
                        Card newCard = filtered.get(rand.nextInt(filtered.size()));
                        List<Long> deck = parseJsonList(exp.getCurrentDeck());
                        if (!deck.isEmpty()) {
                            deck.remove(deck.size() - 1);
                        }
                        deck.add(newCard.getId());
                        exp.setCurrentDeck(toJson(deck));
                        result.put("newCard", newCard.getNameCn());
                    }
                }
                break;
            }
            case "buff_attack": {
                int value = ((Number) effect.getOrDefault("value", 1)).intValue();
                try {
                    Map<String, Object> bs = objectMapper.readValue(
                            exp.getBattleState() != null ? exp.getBattleState() : "{}", Map.class);
                    bs.put("attackBuff", value);
                    exp.setBattleState(objectMapper.writeValueAsString(bs));
                } catch (Exception ignored) {}
                result.put("attackBuff", value);
                break;
            }
            case "buff_max_hp": {
                int value = ((Number) effect.getOrDefault("value", 5)).intValue();
                exp.setMaxHp(exp.getMaxHp() + value);
                exp.setPlayerHp(exp.getPlayerHp() + value);
                result.put("maxHpIncreased", value);
                break;
            }
            case "nothing": {
                result.put("message", "无事发生");
                break;
            }
            case "relic": {
                // 事件奖励遗物
                List<ExpeditionRelic> relics = expeditionRelicRepository.findByShowIdOrShowIdIsNull(exp.getShowId());
                if (!relics.isEmpty()) {
                    List<PlayerRelic> owned = playerRelicRepository.findByExpeditionId(exp.getId());
                    Set<Long> ownedIds = owned.stream().map(PlayerRelic::getRelicId).collect(Collectors.toSet());
                    List<ExpeditionRelic> available = relics.stream()
                            .filter(r -> !ownedIds.contains(r.getId())).collect(Collectors.toList());
                    if (!available.isEmpty()) {
                        ExpeditionRelic relic = available.get(new Random().nextInt(available.size()));
                        addRelicToPlayer(exp, relic.getId());
                        result.put("relicGained", relic.getNameCn());
                        result.put("relicIcon", relic.getIcon());
                    }
                }
                break;
            }
            default: {
                result.put("message", "效果已应用");
            }
        }

        return result;
    }

    private void applyClearRewards(Expedition exp) {
        User user = userRepository.findById(exp.getUserId()).orElse(null);
        if (user == null) return;
        // 通关奖励星尘
        int stardustReward = 100 + exp.getEnemiesKilled() * 10;
        user.setStardust(user.getStardust() + stardustReward);
        // 保存通关时获得的卡牌到用户卡牌库
        List<Long> currentDeck = parseJsonList(exp.getCurrentDeck());
        for (Long cardId : currentDeck) {
            Optional<UserCard> existing = userCardRepository.findByUserIdAndCardId(exp.getUserId(), cardId);
            if (existing.isPresent()) {
                existing.get().setQuantity(existing.get().getQuantity() + 1);
                userCardRepository.save(existing.get());
            } else {
                UserCard uc = new UserCard();
                uc.setUserId(exp.getUserId());
                uc.setCardId(cardId);
                uc.setQuantity(1);
                userCardRepository.save(uc);
            }
        }
        // 清除远征的遗物（远征结束遗物消失）
        clearPlayerRelics(exp.getId());
        userRepository.save(user);
    }

    private List<Map<String, Object>> getHistory(Long userId) {
        List<Expedition> results = expeditionRepository.findByUserIdAndStatusNotOrderByCreatedAtDesc(userId, "in_progress");
        List<Map<String, Object>> history = new ArrayList<>();
        for (Expedition e : results) {
            Map<String, Object> h = new HashMap<>();
            h.put("id", e.getId());
            h.put("status", e.getStatus());
            h.put("act", e.getMaxAct());
            h.put("enemiesKilled", e.getEnemiesKilled());
            h.put("questionsAnswered", e.getQuestionsAnswered());
            h.put("questionsTotal", e.getQuestionsTotal());
            h.put("createdAt", e.getCreatedAt());
            history.add(h);
        }
        return history.size() > 10 ? history.subList(0, 10) : history;
    }

    private Map<String, Object> getRelicEffects(List<Long> relicIds) {
        Map<String, Object> effects = new HashMap<>();
        for (Long rid : relicIds) {
            Optional<Relic> rOpt = relicRepository.findById(rid);
            if (rOpt.isPresent()) {
                Relic r = rOpt.get();
                try {
                    Map<String, Object> ej = objectMapper.readValue(r.getEffectJson(), Map.class);
                    String type = (String) ej.get("type");
                    Object value = ej.get("value");
                    effects.put(type, value != null ? value : true);
                } catch (Exception ignored) {}
            }
        }
        return effects;
    }

    private boolean hasRelicEffect(Map<String, Object> effects, String type) {
        return effects.containsKey(type);
    }

    private int getRelicValue(Map<String, Object> effects, String type, int defaultValue) {
        Object v = effects.get(type);
        if (v instanceof Number) return ((Number) v).intValue();
        return defaultValue;
    }

    /**
     * 获取新遗物系统的效果汇总（从 player_relics + expedition_relics 表）
     */
    private Map<String, Integer> getNewRelicEffects(Long expeditionId) {
        Map<String, Integer> effects = new HashMap<>();
        List<PlayerRelic> playerRelics = playerRelicRepository.findByExpeditionId(expeditionId);
        for (PlayerRelic pr : playerRelics) {
            Optional<ExpeditionRelic> erOpt = expeditionRelicRepository.findById(pr.getRelicId());
            if (erOpt.isPresent()) {
                ExpeditionRelic er = erOpt.get();
                String type = er.getEffectType();
                Integer value = er.getEffectValue();
                // 累积效果值（如多个伤害加成叠加）
                effects.merge(type, value != null ? value : 0, Integer::sum);
            }
        }
        return effects;
    }

    /**
     * 添加遗物到玩家当前远征（同时更新 expeditions.relics JSON + player_relics 表）
     */
    @Transactional
    public void addRelicToPlayer(Expedition exp, Long relicId) {
        // 更新 player_relics 表
        PlayerRelic pr = new PlayerRelic();
        pr.setExpeditionId(exp.getId());
        pr.setRelicId(relicId);
        playerRelicRepository.save(pr);

        // 同时更新 expeditions.relics JSON（保持向后兼容）
        List<Long> relics = parseJsonList(exp.getRelics());
        if (!relics.contains(relicId)) {
            relics.add(relicId);
            exp.setRelics(toJson(relics));
            expeditionRepository.save(exp);
        }
    }

    /**
     * 清除玩家远征的所有遗物（远征结束时）
     */
    @Transactional
    public void clearPlayerRelics(Long expeditionId) {
        playerRelicRepository.deleteByExpeditionId(expeditionId);
    }

    private Map<String, Object> buildExpeditionData(Expedition exp) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", exp.getId());
        data.put("showId", exp.getShowId());
        data.put("act", exp.getAct());
        data.put("node", exp.getNode());
        data.put("maxAct", exp.getMaxAct());
        data.put("playerHp", exp.getPlayerHp());
        data.put("maxHp", exp.getMaxHp());
        data.put("gold", exp.getGold());
        data.put("status", exp.getStatus());
        data.put("questionsAnswered", exp.getQuestionsAnswered());
        data.put("questionsTotal", exp.getQuestionsTotal());
        data.put("enemiesKilled", exp.getEnemiesKilled());

        // 解析JSON字段
        List<List<String>> mapNodeRows = parseJsonNodeRows(exp.getMapNodes());
        // 展平为前端兼容的一维数组（已完成选择的节点显示已选分支类型）
        List<String> flatNodes = new ArrayList<>();
        for (int i = 0; i < mapNodeRows.size(); i++) {
            List<String> row = mapNodeRows.get(i);
            if (row.size() <= 1) {
                flatNodes.add(row.isEmpty() ? "combat" : row.get(0));
            } else {
                int choice = getNodeChoice(exp, i);
                if (choice >= 0 && choice < row.size()) {
                    flatNodes.add(row.get(choice));
                } else {
                    flatNodes.add("branch");
                }
            }
        }
        data.put("mapNodes", flatNodes);
        data.put("currentNodeType", getCurrentNodeType(exp));

        // 如果是岔路节点，返回选项
        String nodeType = getCurrentNodeType(exp);
        if ("branch".equals(nodeType)) {
            data.put("nodeOptions", getCurrentNodeOptions(exp));
        }

        // 读取升级信息
        Map<Long, Map<String, Integer>> cardUpgrades = getCardUpgrades(exp);

        List<Long> deckIds = parseJsonList(exp.getCurrentDeck());
        List<Map<String, Object>> deckData = new ArrayList<>();
        for (Long cid : deckIds) {
            Optional<Card> cOpt = cardRepository.findById(cid);
            if (cOpt.isPresent()) {
                Card c = cOpt.get();
                Map<String, Object> cm = cardToMap(c);
                // 应用升级
                if (cardUpgrades.containsKey(cid)) {
                    Map<String, Integer> upgrade = cardUpgrades.get(cid);
                    cm.put("attackBonus", upgrade.getOrDefault("attackBonus", 0));
                    cm.put("healthBonus", upgrade.getOrDefault("healthBonus", 0));
                    cm.put("effectiveAttack", (c.getAttack() != null ? c.getAttack() : 0) + upgrade.getOrDefault("attackBonus", 0));
                    cm.put("effectiveHealth", (c.getHealth() != null ? c.getHealth() : 0) + upgrade.getOrDefault("healthBonus", 0));
                } else {
                    cm.put("attackBonus", 0);
                    cm.put("healthBonus", 0);
                    cm.put("effectiveAttack", c.getAttack());
                    cm.put("effectiveHealth", c.getHealth());
                }
                deckData.add(cm);
            }
        }
        data.put("deck", deckData);

        List<Long> relicIds = parseJsonList(exp.getRelics());
        List<Map<String, Object>> relicData = new ArrayList<>();
        // 先从旧 relic 表加载
        for (Long rid : relicIds) {
            Optional<Relic> rOpt = relicRepository.findById(rid);
            if (rOpt.isPresent()) {
                relicData.add(relicToMap(rOpt.get()));
            }
        }
        // 再从新 expedition_relics 表加载（通过 player_relics）
        List<PlayerRelic> prList = playerRelicRepository.findByExpeditionId(exp.getId());
        for (PlayerRelic pr : prList) {
            Optional<ExpeditionRelic> erOpt = expeditionRelicRepository.findById(pr.getRelicId());
            if (erOpt.isPresent()) {
                ExpeditionRelic er = erOpt.get();
                Map<String, Object> rm = new LinkedHashMap<>();
                rm.put("id", er.getId());
                rm.put("nameCn", er.getNameCn());
                rm.put("nameEn", er.getNameEn());
                rm.put("rarity", er.getRarity());
                rm.put("effectType", er.getEffectType());
                rm.put("effectValue", er.getEffectValue());
                rm.put("descriptionCn", er.getDescriptionCn());
                rm.put("descriptionEn", er.getDescriptionEn());
                rm.put("icon", er.getIcon());
                relicData.add(rm);
            }
        }
        data.put("relics", relicData);

        return data;
    }

    private Map<String, Object> cardToMap(Card c) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", c.getId());
        m.put("nameCn", c.getNameCn());
        m.put("nameEn", c.getNameEn());
        m.put("attack", c.getAttack());
        m.put("cost", c.getCost());
        m.put("rarity", c.getRarity());
        m.put("cardType", c.getCardType());
        return m;
    }

    private Map<String, Object> relicToMap(Relic r) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", r.getId());
        m.put("nameCn", r.getNameCn());
        m.put("nameEn", r.getNameEn());
        m.put("rarity", r.getRarity());
        m.put("effectCn", r.getEffectCn());
        try {
            m.put("effectJson", objectMapper.readValue(r.getEffectJson(), Map.class));
        } catch (Exception ignored) {}
        return m;
    }

    private Map<String, Object> relicToNewMap(ExpeditionRelic r) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", r.getId());
        m.put("nameCn", r.getNameCn());
        m.put("nameEn", r.getNameEn());
        m.put("rarity", r.getRarity());
        m.put("effectType", r.getEffectType());
        m.put("effectValue", r.getEffectValue());
        m.put("descriptionCn", r.getDescriptionCn());
        m.put("descriptionEn", r.getDescriptionEn());
        m.put("icon", r.getIcon());
        return m;
    }

    private List<List<String>> parseJsonNodeRows(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<List<String>>>() {});
        } catch (Exception e) {
            // 向下兼容：尝试解析为旧版一维数组
            try {
                List<String> flat = objectMapper.readValue(json, new TypeReference<List<String>>() {});
                List<List<String>> result = new ArrayList<>();
                for (String s : flat) {
                    result.add(List.of(s));
                }
                return result;
            } catch (Exception ex) {
                return new ArrayList<>();
            }
        }
    }

    private List<Long> parseJsonList(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<Long>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private List<String> parseJsonStringList(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "[]";
        }
    }
}
