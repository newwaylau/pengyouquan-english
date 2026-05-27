package com.pengyouquan.english.service;

import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 杀戮尖塔风格的状态/增益/减益系统服务。
 * <p>
 * 提供纯状态计算逻辑，类似 Slay the Spire 的格挡(Block)/虚弱(Weak)/易伤(Vulnerable)/力量(Strength) 系统。
 * 所有方法接受和返回标准的 {@code Map<String, Object>} 格式的状态数据。
 * 不依赖任何 Repository 或数据库。
 * <p>
 * 状态存储格式示例：
 * <pre>{@code
 * {
 *   "block": 10,
 *   "weak": 2,
 *   "vulnerable": 1,
 *   "strength": 3,
 *   "strength_turns": 2
 * }
 * }</pre>
 */
@Service
public class ExpeditionStatusService {

    // ==================== 状态键常量 ====================

    /** 格挡/护甲值 */
    public static final String KEY_BLOCK = "block";

    /** 虚弱剩余回合数（>0 时目标造成的伤害减少25%） */
    public static final String KEY_WEAK = "weak";

    /** 易伤剩余回合数（>0 时目标受到的伤害增加50%） */
    public static final String KEY_VULNERABLE = "vulnerable";

    /** 力量/额外攻击力值 */
    public static final String KEY_STRENGTH = "strength";

    /** 临时力量剩余回合数（不存在或 ≤0 表示永久力量） */
    public static final String KEY_STRENGTH_TURNS = "strength_turns";

    // ==================== Block（格挡/护甲） ====================

    /**
     * 增加格挡值。格挡可以叠加，但不超过最大生命值。
     * <p>
     * 格挡会吸收伤害，受到伤害时先消耗格挡再扣血。
     * 每回合结束时格挡归零。
     *
     * @param statuses 当前状态Map（会被修改）
     * @param amount   增加的格挡值
     * @param maxHp    最大生命值（格挡上限）
     */
    public void addBlock(Map<String, Object> statuses, int amount, int maxHp) {
        int currentBlock = getInt(statuses, KEY_BLOCK);
        int newBlock = Math.min(currentBlock + amount, maxHp);
        statuses.put(KEY_BLOCK, newBlock);
    }

    /**
     * 增加格挡值（无最大生命值上限版本）。
     *
     * @param statuses 当前状态Map（会被修改）
     * @param amount   增加的格挡值
     */
    public void addBlock(Map<String, Object> statuses, int amount) {
        int currentBlock = getInt(statuses, KEY_BLOCK);
        statuses.put(KEY_BLOCK, currentBlock + amount);
    }

    // ==================== Weak（虚弱） ====================

    /**
     * 施加虚弱状态。
     * <p>
     * 虚弱状态下，目标造成的伤害减少25%。
     * 每回合结束时减少1回合持续。
     *
     * @param statuses 当前状态Map（会被修改）
     * @param turns    施加的回合数（叠加模式，累加而非覆盖）
     */
    public void applyWeak(Map<String, Object> statuses, int turns) {
        int currentWeak = getInt(statuses, KEY_WEAK);
        statuses.put(KEY_WEAK, currentWeak + turns);
    }

    // ==================== Vulnerable（易伤） ====================

    /**
     * 施加易伤状态。
     * <p>
     * 易伤状态下，目标受到的伤害增加50%。
     * 每回合结束时减少1回合持续。
     *
     * @param statuses 当前状态Map（会被修改）
     * @param turns    施加的回合数（叠加模式）
     */
    public void applyVulnerable(Map<String, Object> statuses, int turns) {
        int currentVuln = getInt(statuses, KEY_VULNERABLE);
        statuses.put(KEY_VULNERABLE, currentVuln + turns);
    }

    // ==================== Strength（力量/攻击力） ====================

    /**
     * 施加临时力量（攻击力加成）。
     * <p>
     * 力量值会直接增加造成的伤害。
     * 临时力量有持续回合数限制，回合结束后衰减。
     *
     * @param statuses 当前状态Map（会被修改）
     * @param turns    持续回合数（≤0 表示永久力量，不衰减）
     * @param amount   力量值（正数为增加，负数为减少）
     */
    public void applyStrength(Map<String, Object> statuses, int turns, int amount) {
        int currentStr = getInt(statuses, KEY_STRENGTH);
        statuses.put(KEY_STRENGTH, currentStr + amount);

        if (turns > 0) {
            // 临时力量：取较大的剩余回合数（不覆盖已有更长的持续时间）
            int currentTurns = getInt(statuses, KEY_STRENGTH_TURNS);
            if (turns > currentTurns) {
                statuses.put(KEY_STRENGTH_TURNS, turns);
            }
        }
        // turns <= 0 表示永久力量，不设置 strength_turns
        // 如果之前是临时力量转为永久，需要移除回合数
        if (turns <= 0) {
            statuses.remove(KEY_STRENGTH_TURNS);
        }
    }

    /**
     * 施加永久力量（攻击力加成）。
     * <p>
     * 永久力量不会因回合结束而衰减。
     *
     * @param statuses 当前状态Map（会被修改）
     * @param amount   力量值
     */
    public void applyStrengthPermanent(Map<String, Object> statuses, int amount) {
        applyStrength(statuses, 0, amount);
    }

    // ==================== 伤害计算核心方法 ====================

    /**
     * 计算经过攻击者状态修正后的实际输出伤害。
     * <p>
     * 修正顺序：力量加成 → 虚弱减免（伤害减少25%）
     * <p>
     * 此方法不修改 statuses 中的任何值（纯计算）。
     *
     * @param baseDamage       基础伤害值（卡牌的原始攻击力）
     * @param attackerStatuses 攻击者的状态Map
     * @return 实际输出伤害（不小于0）
     */
    public int calculateDamageDealt(int baseDamage, Map<String, Object> attackerStatuses) {
        double damage = baseDamage;

        // 1. 力量加成：直接增加攻击力
        int strength = getInt(attackerStatuses, KEY_STRENGTH);
        damage += strength;

        // 2. 虚弱减免：造成的伤害减少25%
        int weak = getInt(attackerStatuses, KEY_WEAK);
        if (weak > 0) {
            damage *= 0.75;
        }

        return Math.max(0, (int) Math.round(damage));
    }

    /**
     * 计算经过防御者状态修正后的实际承受伤害（含格挡吸收）。
     * <p>
     * 修正顺序：易伤加成（伤害增加50%）→ 格挡吸收
     * <p>
     * 注意：此方法会修改 {@code defenderStatuses} 中的格挡值（消耗格挡）。
     * 如果需要纯计算不修改状态，请使用 {@link #calculateDamageTakenPreview(int, Map)}。
     *
     * @param baseDamage        基础伤害值
     * @param defenderStatuses  防御者的状态Map（会被修改：格挡值减少）
     * @return 经过格挡吸收后的实际扣血量（不小于0）
     */
    public int calculateDamageTaken(int baseDamage, Map<String, Object> defenderStatuses) {
        // 1. 易伤加成：受到的伤害增加50%
        double rawDamage = baseDamage;
        int vulnerable = getInt(defenderStatuses, KEY_VULNERABLE);
        if (vulnerable > 0) {
            rawDamage *= 1.5;
        }

        int damage = (int) Math.round(rawDamage);

        // 2. 格挡吸收
        return applyBlockAbsorption(damage, defenderStatuses);
    }

    /**
     * 预览经过防御者状态修正后的伤害（不修改状态）。
     * <p>
     * 与 {@link #calculateDamageTaken(int, Map)} 功能相同，但不会消耗格挡值。
     * 用于 UI 预览、AI 决策等不需要实际消耗状态的场景。
     *
     * @param baseDamage        基础伤害值
     * @param defenderStatuses  防御者的状态Map（不会被修改）
     * @return 修正后的伤害预览值，包含：
     *         <ul>
     *           <li>{@code rawDamage} - 易伤加成后的总伤害</li>
     *           <li>{@code blockAbsorbed} - 被格挡吸收的伤害</li>
     *           <li>{@code hpDamage} - 实际扣血量</li>
     *         </ul>
     */
    public Map<String, Object> calculateDamageTakenPreview(int baseDamage, Map<String, Object> defenderStatuses) {
        double rawDamage = baseDamage;
        int vulnerable = getInt(defenderStatuses, KEY_VULNERABLE);
        if (vulnerable > 0) {
            rawDamage *= 1.5;
        }

        int totalDamage = (int) Math.round(rawDamage);
        int block = getInt(defenderStatuses, KEY_BLOCK);
        int blocked = Math.min(block, totalDamage);
        int hpDamage = Math.max(0, totalDamage - blocked);

        Map<String, Object> preview = new LinkedHashMap<>();
        preview.put("rawDamage", totalDamage);
        preview.put("blockAbsorbed", blocked);
        preview.put("hpDamage", hpDamage);
        preview.put("hasBlock", block > 0);
        preview.put("hasVulnerable", vulnerable > 0);
        return preview;
    }

    // ==================== 快照与回合结束 ====================

    /**
     * 获取当前所有状态的快照（用于前端显示）。
     * <p>
     * 返回一个只包含有效状态（值 > 0）的不可变副本。
     * 不修改原始状态Map。
     *
     * @param statusMap 当前状态Map
     * @return 状态快照（仅包含值 > 0 的状态条目，不可修改）
     */
    public Map<String, Object> getStatusSnapshot(Map<String, Object> statusMap) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        if (statusMap == null || statusMap.isEmpty()) {
            return Collections.unmodifiableMap(snapshot);
        }
        for (Map.Entry<String, Object> entry : statusMap.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Number && ((Number) value).intValue() > 0) {
                snapshot.put(entry.getKey(), value);
            }
        }
        return Collections.unmodifiableMap(snapshot);
    }

    /**
     * 处理回合结束时的状态衰减。
     * <p>
     * 执行以下操作：
     * <ul>
     *   <li>格挡归零（{@code block = 0}）</li>
     *   <li>虚弱减少1回合（减到0后移除键）</li>
     *   <li>易伤减少1回合（减到0后移除键）</li>
     *   <li>临时力量减少1回合（归零后移除力量和回合数）</li>
     * </ul>
     * <p>
     * 永久力量不受影响。
     *
     * @param statuses 当前状态Map（会被修改）
     */
    public void endTurn(Map<String, Object> statuses) {
        if (statuses == null) {
            return;
        }

        // 1. 格挡归零（不保留值，直接删除键以保持Map整洁）
        statuses.remove(KEY_BLOCK);

        // 2. 虚弱衰减
        int weak = getInt(statuses, KEY_WEAK);
        if (weak > 1) {
            statuses.put(KEY_WEAK, weak - 1);
        } else {
            statuses.remove(KEY_WEAK);
        }

        // 3. 易伤衰减
        int vulnerable = getInt(statuses, KEY_VULNERABLE);
        if (vulnerable > 1) {
            statuses.put(KEY_VULNERABLE, vulnerable - 1);
        } else {
            statuses.remove(KEY_VULNERABLE);
        }

        // 4. 临时力量衰减
        int strengthTurns = getInt(statuses, KEY_STRENGTH_TURNS);
        if (strengthTurns > 0) {
            if (strengthTurns <= 1) {
                // 临时力量到期，移除力量和回合数
                statuses.remove(KEY_STRENGTH);
                statuses.remove(KEY_STRENGTH_TURNS);
            } else {
                statuses.put(KEY_STRENGTH_TURNS, strengthTurns - 1);
            }
        }
        // strength_turns 不存在或 ≤0 表示永久力量，不衰减
    }

    // ==================== 辅助方法 ====================

    /**
     * 计算经过格挡吸收后的实际扣血量（仅防御端，无易伤加成）。
     * <p>
     * 会消耗防御者的格挡值。
     *
     * @param rawDamage        原始伤害值
     * @param defenderStatuses 防御者的状态Map（会被修改：格挡值减少）
     * @return 经过格挡吸收后的实际扣血量
     */
    public int applyBlockAbsorption(int rawDamage, Map<String, Object> defenderStatuses) {
        int block = getInt(defenderStatuses, KEY_BLOCK);
        int blocked = Math.min(block, rawDamage);
        int hpDamage = rawDamage - blocked;

        // 消耗格挡
        int remainingBlock = block - blocked;
        if (remainingBlock > 0) {
            defenderStatuses.put(KEY_BLOCK, remainingBlock);
        } else {
            defenderStatuses.remove(KEY_BLOCK);
        }

        return Math.max(0, hpDamage);
    }

    /**
     * 创建一个空的状态Map。
     *
     * @return 新的空状态Map
     */
    public Map<String, Object> createEmptyStatus() {
        return new LinkedHashMap<>();
    }

    /**
     * 创建一个带有初始值的状态Map。
     *
     * @param block       初始格挡值
     * @param weak        初始虚弱回合数
     * @param vulnerable  初始易伤回合数
     * @param strength    初始力量值
     * @return 初始化的状态Map
     */
    public Map<String, Object> createStatus(int block, int weak, int vulnerable, int strength) {
        Map<String, Object> statuses = new LinkedHashMap<>();
        if (block > 0) statuses.put(KEY_BLOCK, block);
        if (weak > 0) statuses.put(KEY_WEAK, weak);
        if (vulnerable > 0) statuses.put(KEY_VULNERABLE, vulnerable);
        if (strength > 0) statuses.put(KEY_STRENGTH, strength);
        return statuses;
    }

    /**
     * 检查指定状态是否存在且大于0。
     *
     * @param statuses 状态Map
     * @param key      状态键
     * @return 如果状态存在且值大于0返回true
     */
    public boolean hasStatus(Map<String, Object> statuses, String key) {
        return getInt(statuses, key) > 0;
    }

    /**
     * 清空所有状态。
     *
     * @param statuses 状态Map（会被清空）
     */
    public void clearAllStatuses(Map<String, Object> statuses) {
        if (statuses != null) {
            statuses.clear();
        }
    }

    // ==================== 内部辅助 ====================

    /**
     * 从Map中安全获取int值。如果key不存在或值不是Number类型，返回0。
     */
    private int getInt(Map<String, Object> map, String key) {
        if (map == null) {
            return 0;
        }
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }
}
