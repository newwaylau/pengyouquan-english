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
    private final ExpeditionEnemyRepository expeditionEnemyRepository;
    private final ExpeditionEventRepository expeditionEventRepository;
    private final CardRepository cardRepository;
    private final UserCardRepository userCardRepository;
    private final UserRepository userRepository;
    private final SentenceRepository sentenceRepository;
    private final ObjectMapper objectMapper;
    private final AchievementService achievementService;

    // 每层节点序列模板
    private static final Map<Integer, List<String>> ACT_NODE_TEMPLATES = new LinkedHashMap<>();
    static {
        ACT_NODE_TEMPLATES.put(1, List.of("combat", "event", "rest", "combat", "combat", "boss"));
        ACT_NODE_TEMPLATES.put(2, List.of("combat", "event", "rest", "shop", "combat", "combat", "boss"));
        ACT_NODE_TEMPLATES.put(3, List.of("combat", "event", "rest", "shop", "combat", "event", "boss"));
    }

    // 基础敌人攻击力
    private static final int BASE_ENEMY_ATTACK = 3;
    private static final int STARTING_GOLD = 50;
    private static final int HEAL_PERCENT = 30;

    public ExpeditionService(ExpeditionRepository expeditionRepository,
                             RelicRepository relicRepository,
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

        // 生成地图节点
        List<String> nodes = ACT_NODE_TEMPLATES.get(1);
        String mapNodesJson = toJson(nodes);

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

        if ("combat".equals(nodeType) || "boss".equals(nodeType)) {
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

        // 获取当前层的敌人列表
        List<ExpeditionEnemy> enemies = expeditionEnemyRepository.findByShowIdAndAct(exp.getShowId(), exp.getAct());
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
        int cardAttack = cardOpt.map(c -> c.getAttack() != null ? c.getAttack() : 2).orElse(2);

        int damageDealt = 0;
        int damageTaken = 0;
        String resultText = "";

        // 检查遗物效果
        List<Long> relicIds = parseJsonList(exp.getRelics());
        Map<String, Object> relicEffects = getRelicEffects(relicIds);

        if (correct) {
            // 答对：造成伤害
            exp.setQuestionsAnswered(exp.getQuestionsAnswered() + 1);
            damageDealt = cardAttack;

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

            // 检测敌人是否死亡
            if (exp.getCurrentEnemyHp() <= 0) {
                exp.setEnemiesKilled(exp.getEnemiesKilled() + 1);
                // 掉落金币
                int goldReward = 5 + rand.nextInt(11); // 5-15
                exp.setGold(exp.getGold() + goldReward);

                String nodeType = getCurrentNodeType(exp);
                boolean isBoss = "boss".equals(nodeType);

                exp.setCurrentEnemyId(null);
                exp.setCurrentEnemyHp(null);
                expeditionRepository.save(exp);

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
            // 升级一张卡牌：增加攻击力2
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
            // 找原卡升级（这里简化处理，记录升级状态）
            Optional<Card> cardOpt = cardRepository.findById(cardId);
            if (cardOpt.isPresent()) {
                Card original = cardOpt.get();
                int newAttack = (original.getAttack() != null ? original.getAttack() : 2) + 2;
                // 在current_deck中用该卡牌id多次出现都算已升级
                // 实际升级效果在前端处理，后端记录升级在battle_state
                try {
                    Map<String, Object> upgrades = new HashMap<>();
                    if (exp.getBattleState() != null && !"{}".equals(exp.getBattleState())) {
                        upgrades = objectMapper.readValue(exp.getBattleState(), Map.class);
                    }
                    if (!upgrades.containsKey("upgradedCards")) {
                        upgrades.put("upgradedCards", new ArrayList<>());
                    }
                    ((List<Object>) upgrades.get("upgradedCards")).add(cardId);
                    exp.setBattleState(objectMapper.writeValueAsString(upgrades));
                } catch (Exception ignored) {}
            }
        }

        expeditionRepository.save(exp);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("healAmount", healAmount);
        result.put("expedition", buildExpeditionData(exp));
        return result;
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

    // ==================== 9. 下一个节点 ====================

    @Transactional
    public Map<String, Object> nextNode(Long userId) {
        Expedition exp = getActiveExpedition(userId);
        List<String> nodes = parseJsonStringList(exp.getMapNodes());

        int nextNode = exp.getNode() + 1;
        if (nextNode > nodes.size()) {
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

    private String getCurrentNodeType(Expedition exp) {
        List<String> nodes = parseJsonStringList(exp.getMapNodes());
        if (nodes.isEmpty()) return "combat";
        int idx = exp.getNode() - 1;
        if (idx >= nodes.size()) return "boss";
        return nodes.get(idx);
    }

    private int getEnemyIndex(Expedition exp, List<ExpeditionEnemy> enemies) {
        List<String> nodes = parseJsonStringList(exp.getMapNodes());
        int combatIndex = 0;
        for (int i = 0; i < exp.getNode() - 1 && i < nodes.size(); i++) {
            String nt = nodes.get(i);
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
        int handSize = Math.min(4, deck.size());

        // 手牌：随机抽取4张
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
                cardData.put("attack", c.getAttack() != null ? c.getAttack() : 2);
                cardData.put("cost", c.getCost() != null ? c.getCost() : 1);
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

        // 3. 移除卡牌或金币
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

        return rewards;
    }

    private List<Map<String, Object>> generateBossRewards(Expedition exp) {
        List<Map<String, Object>> rewards = new ArrayList<>();

        // Boss固定奖励：稀有卡牌 + 遗物 + 大量金币
        List<Card> cards = cardRepository.findByShowId(exp.getShowId());
        if (!cards.isEmpty()) {
            List<Card> shuffled = new ArrayList<>(cards);
            Collections.shuffle(shuffled, new Random());
            Map<String, Object> cardReward = new HashMap<>();
            cardReward.put("type", "card");
            Card c = shuffled.get(0);
            cardReward.put("card", cardToMap(c));
            cardReward.put("label", "获得稀有卡牌：" + c.getNameCn());
            rewards.add(cardReward);
        }

        // 遗物
        List<Relic> bossRelics = relicRepository.findBySource("boss");
        if (!bossRelics.isEmpty()) {
            Random rand = new Random();
            Relic r = bossRelics.get(rand.nextInt(bossRelics.size()));
            Map<String, Object> relicReward = new HashMap<>();
            relicReward.put("type", "relic_add");
            relicReward.put("relic", relicToMap(r));
            relicReward.put("label", "获得遗物：" + r.getNameCn());
            rewards.add(relicReward);
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
        List<String> mapNodes = parseJsonStringList(exp.getMapNodes());
        data.put("mapNodes", mapNodes);
        data.put("currentNodeType", getCurrentNodeType(exp));

        List<Long> deckIds = parseJsonList(exp.getCurrentDeck());
        List<Map<String, Object>> deckData = new ArrayList<>();
        for (Long cid : deckIds) {
            Optional<Card> cOpt = cardRepository.findById(cid);
            if (cOpt.isPresent()) {
                Card c = cOpt.get();
                deckData.add(cardToMap(c));
            }
        }
        data.put("deck", deckData);

        List<Long> relicIds = parseJsonList(exp.getRelics());
        List<Map<String, Object>> relicData = new ArrayList<>();
        for (Long rid : relicIds) {
            Optional<Relic> rOpt = relicRepository.findById(rid);
            if (rOpt.isPresent()) {
                relicData.add(relicToMap(rOpt.get()));
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
