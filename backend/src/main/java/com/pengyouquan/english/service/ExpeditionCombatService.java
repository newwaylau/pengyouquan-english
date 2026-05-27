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
public class ExpeditionCombatService {

    private final ExpeditionRepository expeditionRepository;
    private final ExpeditionBattleStateRepository battleStateRepository;
    private final ExpeditionCardRepository cardRepository;
    private final ExpeditionEnemyRepository enemyRepository;
    private final ObjectMapper objectMapper;

    private static final int HAND_SIZE = 5;
    private static final int MAX_HAND_SIZE = 10;
    private static final int STARTING_ENERGY = 3;

    public ExpeditionCombatService(ExpeditionRepository expeditionRepository,
                                   ExpeditionBattleStateRepository battleStateRepository,
                                   ExpeditionCardRepository cardRepository,
                                   ExpeditionEnemyRepository enemyRepository,
                                   ObjectMapper objectMapper) {
        this.expeditionRepository = expeditionRepository;
        this.battleStateRepository = battleStateRepository;
        this.cardRepository = cardRepository;
        this.enemyRepository = enemyRepository;
        this.objectMapper = objectMapper;
    }

    // ==================== 1. 开始战斗 ====================

    @Transactional
    public Map<String, Object> startBattle(Long userId) {
        Expedition exp = getActiveExpedition(userId);
        if (exp.getCurrentEnemyId() == null) {
            throw new IllegalStateException("当前节点没有敌人");
        }

        Optional<ExpeditionBattleState> existing = battleStateRepository.findByExpeditionId(exp.getId());
        if (existing.isPresent()) {
            return buildBattleData(existing.get(), exp);
        }

        ExpeditionEnemy enemy = enemyRepository.findById(exp.getCurrentEnemyId())
                .orElseThrow(() -> new IllegalStateException("敌人不存在"));

        List<Long> deckCardIds = parseDeck(exp);

        List<Map<String, Object>> drawPile = new ArrayList<>();
        int uid = 0;
        for (Long cardId : deckCardIds) {
            Map<String, Object> cardInstance = new HashMap<>();
            cardInstance.put("uid", uid++);
            cardInstance.put("cardId", cardId);
            cardInstance.put("upgraded", false);
            drawPile.add(cardInstance);
        }

        Collections.shuffle(drawPile, new Random());

        List<Map<String, Object>> hand = new ArrayList<>();
        for (int i = 0; i < HAND_SIZE && !drawPile.isEmpty(); i++) {
            hand.add(drawPile.remove(drawPile.size() - 1));
        }

        ExpeditionBattleState state = new ExpeditionBattleState();
        state.setExpeditionId(exp.getId());
        state.setEnemyId(enemy.getId());
        state.setEnemyHp(enemy.getHp());
        state.setEnemyMaxHp(enemy.getHp());
        state.setEnemyBlock(0);
        state.setEnemyBuffs("[]");
        state.setPlayerBuffs("[]");
        state.setTurnNumber(1);
        state.setHandCards(toJson(hand));
        state.setDrawPile(toJson(drawPile));
        state.setDiscardPile("[]");
        state.setEnergy(STARTING_ENERGY);
        state.setMaxEnergy(STARTING_ENERGY);
        state.setPlayerBlock(0);
        state.setStatus("fighting");
        battleStateRepository.save(state);

        exp.setBattleState("{\"inCombat\":true}");
        expeditionRepository.save(exp);

        return buildBattleData(state, exp);
    }

    // ==================== 2. 出牌 ====================

    @Transactional
    @SuppressWarnings("unchecked")
    public Map<String, Object> playCard(Long userId, int cardUid, Integer targetIndex) {
        Expedition exp = getActiveExpedition(userId);
        ExpeditionBattleState state = battleStateRepository.findByExpeditionId(exp.getId())
                .orElseThrow(() -> new IllegalStateException("没有进行中的战斗"));

        if (!"fighting".equals(state.getStatus())) {
            throw new IllegalStateException("战斗已结束");
        }

        List<Map<String, Object>> hand = parseJsonListOfMaps(state.getHandCards());
        List<Map<String, Object>> drawPile = parseJsonListOfMaps(state.getDrawPile());
        List<Map<String, Object>> discardPile = parseJsonListOfMaps(state.getDiscardPile());
        List<Map<String, Object>> playerBuffs = parseJsonListOfMaps(state.getPlayerBuffs());
        List<Map<String, Object>> enemyBuffs = parseJsonListOfMaps(state.getEnemyBuffs());

        Map<String, Object> cardInstance = null;
        int handIndex = -1;
        for (int i = 0; i < hand.size(); i++) {
            if (Objects.equals(((Number) hand.get(i).get("uid")).intValue(), cardUid)) {
                cardInstance = hand.get(i);
                handIndex = i;
                break;
            }
        }
        if (cardInstance == null) {
            throw new IllegalStateException("该卡牌不在手牌中");
        }

        Long cardId = ((Number) cardInstance.get("cardId")).longValue();
        ExpeditionCard card = cardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalStateException("卡牌不存在"));

        boolean upgraded = Boolean.TRUE.equals(cardInstance.get("upgraded"));

        int cost = card.getIsXCost() ? state.getEnergy() : card.getCost();
        if (cost > state.getEnergy()) {
            throw new IllegalStateException("能量不足");
        }

        state.setEnergy(state.getEnergy() - (card.getIsXCost() ? 0 : card.getCost()));

        // 计算伤害/格挡
        int damage = upgraded ? card.getBaseDamage() + card.getUpgradeDamage() : card.getBaseDamage();
        int block = upgraded ? card.getBaseBlock() + card.getUpgradeBlock() : card.getBaseBlock();

        int strength = getBuffStacks(playerBuffs, "strength");
        if (damage > 0) {
            damage += strength;
        }

        boolean isWeak = hasBuff(playerBuffs, "weak");
        if (isWeak && damage > 0) {
            damage = (int) Math.floor(damage * 0.75);
        }

        Map<String, Object> combatResult = new HashMap<>();
        combatResult.put("cardId", cardId);
        combatResult.put("cardName", card.getCardNameEn());
        combatResult.put("cardType", card.getCardType());

        if (block > 0) {
            int dexterity = getBuffStacks(playerBuffs, "dexterity");
            block += dexterity;
            boolean isFrail = hasBuff(playerBuffs, "frail");
            if (isFrail) {
                block = (int) Math.floor(block * 0.75);
            }
            state.setPlayerBlock(state.getPlayerBlock() + block);
            combatResult.put("blockGained", block);
        }

        if (damage > 0) {
            int finalDamage = calculateDamageToEnemy(damage, enemyBuffs, state);
            combatResult.put("damageDealt", finalDamage);
            combatResult.put("enemyHpBefore", state.getEnemyHp() + finalDamage + Math.max(0,
                    state.getEnemyBlock() > finalDamage ? 0 : finalDamage - state.getEnemyBlock()));
        }

        if (card.getKeywords() != null && !card.getKeywords().isBlank()) {
            try {
                List<Map<String, Object>> keywords = objectMapper.readValue(
                        card.getKeywords(), new TypeReference<List<Map<String, Object>>>() {});
                combatResult.putAll(processKeywords(keywords, hand, drawPile, discardPile,
                        playerBuffs, enemyBuffs, exp));
            } catch (Exception e) {
                // skip keyword errors
            }
        }

        hand.remove(handIndex);

        boolean exhaust = false;
        if (card.getKeywords() != null && card.getKeywords().contains("exhaust")) {
            exhaust = true;
        }
        if (!exhaust) {
            discardPile.add(cardInstance);
        }

        String battleResult = checkBattleEnd(state, exp);

        state.setHandCards(toJson(hand));
        state.setDrawPile(toJson(drawPile));
        state.setDiscardPile(toJson(discardPile));
        state.setPlayerBuffs(toJson(playerBuffs));
        state.setEnemyBuffs(toJson(enemyBuffs));
        battleStateRepository.save(state);

        combatResult.put("battleResult", battleResult);
        combatResult.put("currentEnergy", state.getEnergy());
        combatResult.put("playerBlock", state.getPlayerBlock());
        combatResult.put("enemyHp", state.getEnemyHp());
        combatResult.put("enemyBlock", state.getEnemyBlock());

        Map<String, Object> result = new HashMap<>();
        result.put("combatResult", combatResult);
        result.put("battleState", buildBattleData(state, exp));
        return result;
    }

    // ==================== 3. 结束回合 ====================

    @Transactional
    @SuppressWarnings("unchecked")
    public Map<String, Object> endTurn(Long userId) {
        Expedition exp = getActiveExpedition(userId);
        ExpeditionBattleState state = battleStateRepository.findByExpeditionId(exp.getId())
                .orElseThrow(() -> new IllegalStateException("没有进行中的战斗"));

        if (!"fighting".equals(state.getStatus())) {
            throw new IllegalStateException("战斗已结束");
        }

        List<Map<String, Object>> hand = parseJsonListOfMaps(state.getHandCards());
        List<Map<String, Object>> drawPile = parseJsonListOfMaps(state.getDrawPile());
        List<Map<String, Object>> discardPile = parseJsonListOfMaps(state.getDiscardPile());
        List<Map<String, Object>> playerBuffs = parseJsonListOfMaps(state.getPlayerBuffs());
        List<Map<String, Object>> enemyBuffs = parseJsonListOfMaps(state.getEnemyBuffs());

        state.setPlayerBlock(0);

        discardPile.addAll(hand);
        hand.clear();

        decrementBuffs(playerBuffs);

        ExpeditionEnemy enemy = enemyRepository.findById(state.getEnemyId())
                .orElseThrow(() -> new IllegalStateException("敌人不存在"));

        Map<String, Object> enemyActionResult = executeEnemyTurn(enemy, state, playerBuffs, enemyBuffs, drawPile, discardPile, exp);

        decrementBuffs(enemyBuffs);

        String battleResult = checkBattleEnd(state, exp);

        if ("fighting".equals(state.getStatus())) {
            state.setEnergy(state.getMaxEnergy());
            state.setTurnNumber(state.getTurnNumber() + 1);
            drawCards(HAND_SIZE, hand, drawPile, discardPile);
        }

        state.setHandCards(toJson(hand));
        state.setDrawPile(toJson(drawPile));
        state.setDiscardPile(toJson(discardPile));
        state.setPlayerBuffs(toJson(playerBuffs));
        state.setEnemyBuffs(toJson(enemyBuffs));
        battleStateRepository.save(state);

        Map<String, Object> result = new HashMap<>();
        result.put("enemyAction", enemyActionResult);
        result.put("battleResult", battleResult);
        result.put("battleState", buildBattleData(state, exp));
        return result;
    }

    // ==================== 4. 用药水 ====================

    @Transactional
    public Map<String, Object> usePotion(Long userId, Long potionId) {
        throw new IllegalStateException("药水功能暂未实现");
    }

    // ==================== 5. 获取战斗状态 ====================

    public Map<String, Object> getBattleStateData(Long userId) {
        Expedition exp = getActiveExpedition(userId);
        ExpeditionBattleState state = battleStateRepository.findByExpeditionId(exp.getId())
                .orElseThrow(() -> new IllegalStateException("没有进行中的战斗"));
        return buildBattleData(state, exp);
    }

    // ==================== 伤害计算 ====================

    private int calculateDamageToEnemy(int rawDamage, List<Map<String, Object>> enemyBuffs,
                                       ExpeditionBattleState state) {
        int damage = rawDamage;
        if (hasBuff(enemyBuffs, "vulnerable")) {
            damage = (int) Math.floor(damage * 1.5);
        }
        if (state.getEnemyBlock() > 0) {
            if (damage <= state.getEnemyBlock()) {
                state.setEnemyBlock(state.getEnemyBlock() - damage);
                return 0;
            } else {
                damage -= state.getEnemyBlock();
                state.setEnemyBlock(0);
            }
        }
        state.setEnemyHp(Math.max(0, state.getEnemyHp() - damage));
        return damage;
    }

    // ==================== 关键字处理 ====================

    @SuppressWarnings("unchecked")
    private Map<String, Object> processKeywords(List<Map<String, Object>> keywords,
                                                 List<Map<String, Object>> hand,
                                                 List<Map<String, Object>> drawPile,
                                                 List<Map<String, Object>> discardPile,
                                                 List<Map<String, Object>> playerBuffs,
                                                 List<Map<String, Object>> enemyBuffs,
                                                 Expedition exp) {
        Map<String, Object> effects = new HashMap<>();
        for (Map<String, Object> kw : keywords) {
            String type = (String) kw.get("type");
            if (type == null) continue;
            switch (type) {
                case "draw":
                    int drawCount = ((Number) kw.getOrDefault("value", 1)).intValue();
                    drawCards(drawCount, hand, drawPile, discardPile);
                    effects.put("drewCards", drawCount);
                    break;
                case "vulnerable":
                    addBuff(enemyBuffs, "vulnerable", ((Number) kw.getOrDefault("value", 1)).intValue());
                    effects.put("appliedVulnerable", kw.get("value"));
                    break;
                case "weak":
                    addBuff(enemyBuffs, "weak", ((Number) kw.getOrDefault("value", 1)).intValue());
                    effects.put("appliedWeak", kw.get("value"));
                    break;
                case "strength":
                    addBuff(playerBuffs, "strength", ((Number) kw.getOrDefault("value", 1)).intValue());
                    effects.put("gainedStrength", kw.get("value"));
                    break;
                case "dexterity":
                    addBuff(playerBuffs, "dexterity", ((Number) kw.getOrDefault("value", 1)).intValue());
                    effects.put("gainedDexterity", kw.get("value"));
                    break;
                case "heal":
                    int healAmount = ((Number) kw.getOrDefault("value", 2)).intValue();
                    int newHp = Math.min(exp.getMaxHp(), exp.getPlayerHp() + healAmount);
                    effects.put("healed", newHp - exp.getPlayerHp());
                    exp.setPlayerHp(newHp);
                    break;
                case "aoe":
                    effects.put("aoe", true);
                    break;
                case "exhaust":
                    effects.put("exhausted", true);
                    break;
                case "frail":
                    addBuff(enemyBuffs, "frail", ((Number) kw.getOrDefault("value", 1)).intValue());
                    effects.put("appliedFrail", kw.get("value"));
                    break;
                case "poison":
                    addBuff(enemyBuffs, "poison", ((Number) kw.getOrDefault("value", 1)).intValue());
                    effects.put("appliedPoison", kw.get("value"));
                    break;
                default:
                    break;
            }
        }
        return effects;
    }

    // ==================== Buff管理 ====================

    private void addBuff(List<Map<String, Object>> buffs, String name, int stacks) {
        for (Map<String, Object> buff : buffs) {
            if (name.equals(buff.get("name"))) {
                int current = ((Number) buff.getOrDefault("stacks", 0)).intValue();
                buff.put("stacks", current + stacks);
                return;
            }
        }
        Map<String, Object> newBuff = new HashMap<>();
        newBuff.put("name", name);
        newBuff.put("stacks", stacks);
        buffs.add(newBuff);
    }

    private int getBuffStacks(List<Map<String, Object>> buffs, String name) {
        for (Map<String, Object> buff : buffs) {
            if (name.equals(buff.get("name"))) {
                return ((Number) buff.getOrDefault("stacks", 0)).intValue();
            }
        }
        return 0;
    }

    private boolean hasBuff(List<Map<String, Object>> buffs, String name) {
        return getBuffStacks(buffs, name) > 0;
    }

    private void decrementBuffs(List<Map<String, Object>> buffs) {
        Iterator<Map<String, Object>> it = buffs.iterator();
        while (it.hasNext()) {
            Map<String, Object> buff = it.next();
            String name = (String) buff.get("name");
            if ("strength".equals(name) || "dexterity".equals(name)) {
                continue;
            }
            int stacks = ((Number) buff.getOrDefault("stacks", 0)).intValue() - 1;
            if (stacks <= 0) {
                it.remove();
            } else {
                buff.put("stacks", stacks);
            }
        }
    }

    // ==================== 敌人AI ====================

    @SuppressWarnings("unchecked")
    private Map<String, Object> executeEnemyTurn(ExpeditionEnemy enemy, ExpeditionBattleState state,
                                                  List<Map<String, Object>> playerBuffs,
                                                  List<Map<String, Object>> enemyBuffs,
                                                  List<Map<String, Object>> drawPile,
                                                  List<Map<String, Object>> discardPile,
                                                  Expedition exp) {
        Map<String, Object> result = new HashMap<>();

        List<Map<String, Object>> pattern;
        try {
            String behaviorStr = enemy.getSpecialRules();
            if (behaviorStr != null && !behaviorStr.isBlank()) {
                Map<String, Object> rules = objectMapper.readValue(behaviorStr, Map.class);
                Object patternObj = rules.get("pattern");
                if (patternObj instanceof List) {
                    pattern = (List<Map<String, Object>>) patternObj;
                } else {
                    pattern = getDefaultPattern(enemy);
                }
            } else {
                pattern = getDefaultPattern(enemy);
            }
        } catch (Exception e) {
            pattern = getDefaultPattern(enemy);
        }

        int turnIndex = (state.getTurnNumber() - 1) % pattern.size();
        Map<String, Object> action = pattern.get(turnIndex);

        String actionType = (String) action.getOrDefault("type", "attack");
        int value = ((Number) action.getOrDefault("value", 6)).intValue();

        result.put("actionType", actionType);
        result.put("actionValue", value);

        switch (actionType) {
            case "attack":
                int enemyDamage = value;
                int enemyStrength = getBuffStacks(enemyBuffs, "strength");
                enemyDamage += enemyStrength;

                if (hasBuff(playerBuffs, "vulnerable")) {
                    enemyDamage = (int) Math.floor(enemyDamage * 1.5);
                }
                if (hasBuff(enemyBuffs, "weak")) {
                    enemyDamage = (int) Math.floor(enemyDamage * 0.75);
                }

                int playerBlock = state.getPlayerBlock();
                int damageToHp = enemyDamage;
                if (playerBlock > 0) {
                    if (enemyDamage <= playerBlock) {
                        state.setPlayerBlock(playerBlock - enemyDamage);
                        damageToHp = 0;
                    } else {
                        damageToHp = enemyDamage - playerBlock;
                        state.setPlayerBlock(0);
                    }
                }
                if (damageToHp > 0) {
                    exp.setPlayerHp(Math.max(0, exp.getPlayerHp() - damageToHp));
                }
                result.put("damageToPlayer", damageToHp);
                result.put("blockBroken", Math.min(enemyDamage, playerBlock));
                break;

            case "buff":
                String buffName = (String) action.getOrDefault("buff", "strength");
                int buffValue = ((Number) action.getOrDefault("value", 1)).intValue();
                String target = (String) action.getOrDefault("target", "self");
                if ("player".equals(target)) {
                    addBuff(playerBuffs, buffName, buffValue);
                    result.put("playerBuffApplied", buffName + "+" + buffValue);
                } else {
                    addBuff(enemyBuffs, buffName, buffValue);
                    result.put("enemyBuffApplied", buffName + "+" + buffValue);
                }
                break;

            case "block":
                state.setEnemyBlock(state.getEnemyBlock() + value);
                result.put("enemyBlockGained", value);
                break;

            case "debuff":
                String debuffName = (String) action.getOrDefault("buff", "weak");
                int debuffValue = ((Number) action.getOrDefault("value", 1)).intValue();
                addBuff(playerBuffs, debuffName, debuffValue);
                result.put("playerDebuffApplied", debuffName + "+" + debuffValue);
                break;

            default:
                int dmg = value;
                int pBlock = state.getPlayerBlock();
                int dtHp = dmg;
                if (pBlock > 0) {
                    if (dmg <= pBlock) {
                        state.setPlayerBlock(pBlock - dmg);
                        dtHp = 0;
                    } else {
                        dtHp = dmg - pBlock;
                        state.setPlayerBlock(0);
                    }
                }
                if (dtHp > 0) {
                    exp.setPlayerHp(Math.max(0, exp.getPlayerHp() - dtHp));
                }
                result.put("damageToPlayer", dtHp);
                break;
        }

        result.put("playerHpAfter", exp.getPlayerHp());
        result.put("playerBlockAfter", state.getPlayerBlock());
        return result;
    }

    private List<Map<String, Object>> getDefaultPattern(ExpeditionEnemy enemy) {
        List<Map<String, Object>> pattern = new ArrayList<>();
        Map<String, Object> attack = new HashMap<>();
        attack.put("type", "attack");
        attack.put("value", Math.max(3, enemy.getHp() / 4));
        pattern.add(attack);
        return pattern;
    }

    // ==================== 战斗结算 ====================

    private String checkBattleEnd(ExpeditionBattleState state, Expedition exp) {
        if (state.getEnemyHp() <= 0) {
            state.setStatus("won");
            state.setEnemyHp(0);
            exp.setEnemiesKilled(exp.getEnemiesKilled() + 1);
            exp.setCurrentEnemyId(null);
            exp.setCurrentEnemyHp(null);
            exp.setBattleState("{}");
            expeditionRepository.save(exp);
            return "won";
        }
        if (exp.getPlayerHp() <= 0) {
            state.setStatus("lost");
            exp.setPlayerHp(0);
            exp.setStatus("dead");
            exp.setBattleState("{}");
            expeditionRepository.save(exp);
            return "lost";
        }
        return "fighting";
    }

    // ==================== 牌组管理 ====================

    private void drawCards(int count, List<Map<String, Object>> hand,
                           List<Map<String, Object>> drawPile,
                           List<Map<String, Object>> discardPile) {
        for (int i = 0; i < count; i++) {
            if (hand.size() >= MAX_HAND_SIZE) break;
            if (drawPile.isEmpty()) {
                if (discardPile.isEmpty()) break;
                drawPile.addAll(discardPile);
                discardPile.clear();
                Collections.shuffle(drawPile, new Random());
            }
            hand.add(drawPile.remove(drawPile.size() - 1));
        }
    }

    private List<Long> parseDeck(Expedition exp) {
        try {
            String deckStr = exp.getCurrentDeck();
            if (deckStr != null && !deckStr.isBlank() && !"[]".equals(deckStr)) {
                return objectMapper.readValue(deckStr, new TypeReference<List<Long>>() {});
            }
            deckStr = exp.getStartingDeck();
            if (deckStr != null && !deckStr.isBlank()) {
                return objectMapper.readValue(deckStr, new TypeReference<List<Long>>() {});
            }
        } catch (Exception e) {
            // ignore
        }
        return new ArrayList<>();
    }

    // ==================== 构建响应 ====================

    private Map<String, Object> buildBattleData(ExpeditionBattleState state, Expedition exp) {
        Map<String, Object> data = new HashMap<>();
        data.put("battleId", state.getId());
        data.put("status", state.getStatus());
        data.put("turnNumber", state.getTurnNumber());
        data.put("energy", state.getEnergy());
        data.put("maxEnergy", state.getMaxEnergy());
        data.put("playerBlock", state.getPlayerBlock());

        Map<String, Object> playerInfo = new HashMap<>();
        playerInfo.put("hp", exp.getPlayerHp());
        playerInfo.put("maxHp", exp.getMaxHp());
        data.put("player", playerInfo);

        Optional<ExpeditionEnemy> enemyOpt = enemyRepository.findById(state.getEnemyId());
        Map<String, Object> enemyInfo = new HashMap<>();
        enemyInfo.put("id", state.getEnemyId());
        enemyInfo.put("hp", state.getEnemyHp());
        enemyInfo.put("maxHp", state.getEnemyMaxHp());
        enemyInfo.put("block", state.getEnemyBlock());
        enemyOpt.ifPresent(e -> {
            enemyInfo.put("name", e.getNameEn());
            enemyInfo.put("nameCn", e.getNameCn());
        });
        data.put("enemy", enemyInfo);

        // 手牌信息 — 批量查卡牌，避免 N+1
        List<Map<String, Object>> handCards = parseJsonListOfMaps(state.getHandCards());
        List<Long> cardIds = handCards.stream()
                .map(h -> ((Number) h.get("cardId")).longValue())
                .collect(Collectors.toList());
        Map<Long, ExpeditionCard> cardMap = cardRepository.findAllById(cardIds).stream()
                .collect(Collectors.toMap(ExpeditionCard::getId, c -> c));
        List<Map<String, Object>> handWithDetails = new ArrayList<>();
        for (Map<String, Object> instance : handCards) {
            Long cid = ((Number) instance.get("cardId")).longValue();
            ExpeditionCard card = cardMap.get(cid);
            if (card == null) continue;
            Map<String, Object> cardData = new HashMap<>(instance);
            cardData.put("cardName", card.getCardName());
            cardData.put("cardNameEn", card.getCardNameEn());
            cardData.put("cardType", card.getCardType());
            cardData.put("cost", card.getIsXCost() ? state.getEnergy() : card.getCost());
            boolean upgraded = Boolean.TRUE.equals(instance.get("upgraded"));
            cardData.put("baseDamage", upgraded ? card.getBaseDamage() + card.getUpgradeDamage() : card.getBaseDamage());
            cardData.put("baseBlock", upgraded ? card.getBaseBlock() + card.getUpgradeBlock() : card.getBaseBlock());
            cardData.put("description", upgraded && card.getUpgradeDescription() != null && !card.getUpgradeDescription().isBlank()
                    ? card.getUpgradeDescription() : card.getDescription());
            cardData.put("rarity", card.getRarity());
            cardData.put("isXCost", card.getIsXCost());
            handWithDetails.add(cardData);
        }
        data.put("hand", handWithDetails);

        data.put("playerBuffs", parseJsonListOfMaps(state.getPlayerBuffs()));
        data.put("enemyBuffs", parseJsonListOfMaps(state.getEnemyBuffs()));

        List<Map<String, Object>> drawPile = parseJsonListOfMaps(state.getDrawPile());
        List<Map<String, Object>> discardPile = parseJsonListOfMaps(state.getDiscardPile());
        data.put("drawPileCount", drawPile.size());
        data.put("discardPileCount", discardPile.size());

        return data;
    }

    private Expedition getActiveExpedition(Long userId) {
        return expeditionRepository.findByUserIdAndStatus(userId, "in_progress")
                .orElseThrow(() -> new IllegalStateException("没有进行中的远征"));
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<Map<String, Object>> parseJsonListOfMaps(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
