package com.pengyouquan.english.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 远征Boss独特技能系统
 * 为每个Act的Boss提供独特技能和行为模式
 * 纯内存逻辑，不依赖数据库
 */
@Service
public class ExpeditionBossService {

    // ============================================================
    // 内部数据结构
    // ============================================================

    /**
     * Boss技能定义
     */
    public static class BossSkill {
        private String name;
        private String description;
        private int damage;
        private int cooldown;     // 冷却回合数
        private int currentCooldown; // 当前剩余冷却

        public BossSkill() {}

        public BossSkill(String name, String description, int damage, int cooldown) {
            this.name = name;
            this.description = description;
            this.damage = damage;
            this.cooldown = cooldown;
            this.currentCooldown = 0;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public int getDamage() { return damage; }
        public void setDamage(int damage) { this.damage = damage; }

        public int getCooldown() { return cooldown; }
        public void setCooldown(int cooldown) { this.cooldown = cooldown; }

        public int getCurrentCooldown() { return currentCooldown; }
        public void setCurrentCooldown(int currentCooldown) { this.currentCooldown = currentCooldown; }

        /** 是否可用（冷却为0） */
        public boolean isReady() { return currentCooldown <= 0; }

        /** 使用后重置冷却 */
        public void use() { this.currentCooldown = this.cooldown; }

        /** 每回合减少冷却 */
        public void tickCooldown() {
            if (currentCooldown > 0) {
                currentCooldown--;
            }
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("name", name);
            map.put("description", description);
            map.put("damage", damage);
            map.put("cooldown", cooldown);
            map.put("currentCooldown", currentCooldown);
            return map;
        }
    }

    /**
     * Boss Buff 定义
     */
    public static class BossBuff {
        private String name;
        private String description;
        private int duration;
        private int remainingTurns;

        public BossBuff() {}

        public BossBuff(String name, String description, int duration) {
            this.name = name;
            this.description = description;
            this.duration = duration;
            this.remainingTurns = duration;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public int getDuration() { return duration; }
        public void setDuration(int duration) { this.duration = duration; }

        public int getRemainingTurns() { return remainingTurns; }
        public void setRemainingTurns(int remainingTurns) { this.remainingTurns = remainingTurns; }

        /** 每回合减少剩余回合 */
        public void tick() { if (remainingTurns > 0) remainingTurns--; }

        /** 是否过期 */
        public boolean isExpired() { return remainingTurns <= 0; }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("name", name);
            map.put("description", description);
            map.put("duration", duration);
            map.put("remainingTurns", remainingTurns);
            return map;
        }
    }

    /**
     * Boss完整数据结构
     */
    public static class BossData {
        private String id;
        private String nameCn;
        private String nameEn;
        private int maxHp;
        private int currentHp;
        private List<BossSkill> skills;
        private List<BossBuff> buffs;
        private int phase;           // 1 = 第一阶段, 2 = 第二阶段
        private int phaseThreshold;  // 进入第二阶段的血量百分比(默认50)
        private boolean canResurrect;  // 是否可以复活
        private boolean hasResurrected; // 是否已用掉复活
        private int resurrectHp;       // 复活后的血量
        private int currentTurn;       // 当前战斗回合计数

        public BossData() {}

        public BossData(String id, String nameCn, String nameEn, int maxHp,
                        List<BossSkill> skills, int phaseThreshold) {
            this.id = id;
            this.nameCn = nameCn;
            this.nameEn = nameEn;
            this.maxHp = maxHp;
            this.currentHp = maxHp;
            this.skills = skills;
            this.buffs = new ArrayList<>();
            this.phase = 1;
            this.phaseThreshold = phaseThreshold;
            this.canResurrect = false;
            this.hasResurrected = false;
            this.resurrectHp = 0;
            this.currentTurn = 0;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getNameCn() { return nameCn; }
        public void setNameCn(String nameCn) { this.nameCn = nameCn; }

        public String getNameEn() { return nameEn; }
        public void setNameEn(String nameEn) { this.nameEn = nameEn; }

        public int getMaxHp() { return maxHp; }
        public void setMaxHp(int maxHp) { this.maxHp = maxHp; }

        public int getCurrentHp() { return currentHp; }
        public void setCurrentHp(int currentHp) { this.currentHp = currentHp; }

        public List<BossSkill> getSkills() { return skills; }
        public void setSkills(List<BossSkill> skills) { this.skills = skills; }

        public List<BossBuff> getBuffs() { return buffs; }
        public void setBuffs(List<BossBuff> buffs) { this.buffs = buffs; }

        public int getPhase() { return phase; }
        public void setPhase(int phase) { this.phase = phase; }

        public int getPhaseThreshold() { return phaseThreshold; }
        public void setPhaseThreshold(int phaseThreshold) { this.phaseThreshold = phaseThreshold; }

        public boolean isCanResurrect() { return canResurrect; }
        public void setCanResurrect(boolean canResurrect) { this.canResurrect = canResurrect; }

        public boolean isHasResurrected() { return hasResurrected; }
        public void setHasResurrected(boolean hasResurrected) { this.hasResurrected = hasResurrected; }

        public int getResurrectHp() { return resurrectHp; }
        public void setResurrectHp(int resurrectHp) { this.resurrectHp = resurrectHp; }

        public int getCurrentTurn() { return currentTurn; }
        public void setCurrentTurn(int currentTurn) { this.currentTurn = currentTurn; }

        /** 检查是否应该进入第二阶段 */
        public boolean shouldPhaseTransition() {
            return phase == 1 && currentHp > 0
                    && (currentHp * 100 / maxHp) < phaseThreshold;
        }

        /** 检查Boss是否已死亡 */
        public boolean isDead() {
            return currentHp <= 0;
        }

        /** Boss受到伤害 */
        public void takeDamage(int damage) {
            this.currentHp = Math.max(0, this.currentHp - damage);
        }

        /** Boss恢复生命 */
        public void heal(int amount) {
            this.currentHp = Math.min(maxHp, this.currentHp + amount);
        }

        /** 添加Buff */
        public void addBuff(BossBuff buff) {
            buffs.add(buff);
        }

        /** 清除所有Buff */
        public void clearBuffs() {
            buffs.clear();
        }

        /** 每回合更新Boss状态 */
        public void tickTurn() {
            currentTurn++;
            // 冷却递减
            for (BossSkill skill : skills) {
                skill.tickCooldown();
            }
            // Buff递减
            buffs.removeIf(BossBuff::isExpired);
            for (BossBuff buff : buffs) {
                buff.tick();
            }
            buffs.removeIf(BossBuff::isExpired);

            // 检查阶段转换
            if (shouldPhaseTransition()) {
                phase = 2;
            }
        }

        /** 转换为Map输出 */
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("id", id);
            map.put("nameCn", nameCn);
            map.put("nameEn", nameEn);
            map.put("maxHp", maxHp);
            map.put("currentHp", currentHp);
            map.put("phase", phase);
            map.put("currentTurn", currentTurn);
            List<Map<String, Object>> skillMaps = new ArrayList<>();
            for (BossSkill skill : skills) {
                skillMaps.add(skill.toMap());
            }
            map.put("skills", skillMaps);
            List<Map<String, Object>> buffMaps = new ArrayList<>();
            for (BossBuff buff : buffs) {
                buffMaps.add(buff.toMap());
            }
            map.put("buffs", buffMaps);
            return map;
        }
    }

    /**
     * Boss行动结果
     */
    public static class BossAction {
        private String skillName;
        private String description;
        private int damageToPlayer;    // 对玩家造成的伤害
        private boolean isAoe;         // 是否AOE
        private int healAmount;        // Boss回血量
        private List<String> summonedMinions; // 召唤的小兵信息
        private List<BossBuff> buffsApplied;  // 获得的Buff
        private boolean phaseTransition;       // 是否阶段转换
        private boolean resurrected;           // 是否触发了复活

        public BossAction() {
            this.summonedMinions = new ArrayList<>();
            this.buffsApplied = new ArrayList<>();
        }

        public String getSkillName() { return skillName; }
        public void setSkillName(String skillName) { this.skillName = skillName; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public int getDamageToPlayer() { return damageToPlayer; }
        public void setDamageToPlayer(int damageToPlayer) { this.damageToPlayer = damageToPlayer; }

        public boolean isAoe() { return isAoe; }
        public void setAoe(boolean aoe) { isAoe = aoe; }

        public int getHealAmount() { return healAmount; }
        public void setHealAmount(int healAmount) { this.healAmount = healAmount; }

        public List<String> getSummonedMinions() { return summonedMinions; }
        public void setSummonedMinions(List<String> summonedMinions) { this.summonedMinions = summonedMinions; }

        public List<BossBuff> getBuffsApplied() { return buffsApplied; }
        public void setBuffsApplied(List<BossBuff> buffsApplied) { this.buffsApplied = buffsApplied; }

        public boolean isPhaseTransition() { return phaseTransition; }
        public void setPhaseTransition(boolean phaseTransition) { this.phaseTransition = phaseTransition; }

        public boolean isResurrected() { return resurrected; }
        public void setResurrected(boolean resurrected) { this.resurrected = resurrected; }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("skillName", skillName);
            map.put("description", description);
            map.put("damageToPlayer", damageToPlayer);
            map.put("isAoe", isAoe);
            map.put("healAmount", healAmount);
            map.put("summonedMinions", new ArrayList<>(summonedMinions));
            List<Map<String, Object>> buffMaps = new ArrayList<>();
            for (BossBuff buff : buffsApplied) {
                buffMaps.add(buff.toMap());
            }
            map.put("buffsApplied", buffMaps);
            map.put("phaseTransition", phaseTransition);
            map.put("resurrected", resurrected);
            return map;
        }
    }

    // ============================================================
    // 预定义的Boss数据（静态初始化）
    // ============================================================

    private static final Map<String, Map<Integer, BossData>> BOSS_REGISTRY = new HashMap<>();

    static {
        initGoTBosses();
        initDABosses();
    }

    /** 初始化GoT(showId=1)的Boss */
    private static void initGoTBosses() {
        String showKey = "1";

        // ---- Act 1: 巨人WunWun ----
        BossSkill wunWunSmash = new BossSkill("巨力猛击", "挥舞巨大木棍猛击敌人，造成大量伤害", 8, 3);
        BossSkill wunWunStomp = new BossSkill("震地践踏", "巨人重重踩踏地面，造成范围伤害", 4, 2);
        BossSkill wunWunRoar = new BossSkill("战吼", "巨人的怒吼震慑敌人，造成中等伤害", 3, 1);
        List<BossSkill> wunWunSkills = new ArrayList<>(Arrays.asList(wunWunSmash, wunWunStomp, wunWunRoar));

        Map<Integer, BossData> goTBosses = new HashMap<>();
        BossData wunWun = new BossData("got_act1_boss", "巨人WunWun", "WunWun the Giant", 40, wunWunSkills, 50);
        goTBosses.put(1, wunWun);

        // ---- Act 2: 瑟曦·兰尼斯特 ----
        BossSkill cerseiSummon = new BossSkill("召唤金袍卫", "瑟曦召唤金袍卫队前来助战", 0, 2);
        BossSkill cerseiWildfire = new BossSkill("野火焚城", "引爆野火，对所有敌人造成大量伤害", 6, 4);
        BossSkill cerseiPoison = new BossSkill("毒酒暗算", "在酒中下毒，造成伤害并减弱对手攻击", 3, 2);
        List<BossSkill> cerseiSkills = new ArrayList<>(Arrays.asList(cerseiSummon, cerseiWildfire, cerseiPoison));

        BossData cersei = new BossData("got_act2_boss", "瑟曦·兰尼斯特", "Cersei Lannister", 50, cerseiSkills, 50);
        goTBosses.put(2, cersei);

        // ---- Act 3: 夜王 ----
        BossSkill nightKingFrost = new BossSkill("冰霜吐息", "夜王释放极寒冰霜吐息，对所有敌人造成伤害", 5, 3);
        BossSkill nightKingRaise = new BossSkill("复活死者", "夜王复活阵亡的尸鬼加入战斗", 0, 4);
        BossSkill nightKingIceSpear = new BossSkill("冰矛投掷", "投掷冰矛刺穿敌人，造成大量伤害", 7, 2);
        BossSkill nightKingBlizzard = new BossSkill("暴风雪", "召唤暴风雪冻结一切，造成AOE伤害并减速", 4, 3);
        List<BossSkill> nightKingSkills = new ArrayList<>(Arrays.asList(nightKingFrost, nightKingRaise, nightKingIceSpear, nightKingBlizzard));

        BossData nightKing = new BossData("got_act3_boss", "夜王", "The Night King", 65, nightKingSkills, 50);
        nightKing.setCanResurrect(true);
        nightKing.setHasResurrected(false);
        nightKing.setResurrectHp(30);
        goTBosses.put(3, nightKing);

        BOSS_REGISTRY.put(showKey, goTBosses);
    }

    /** 初始化DA(showId=12)的Boss */
    private static void initDABosses() {
        String showKey = "12";

        // ---- Act 1: 托马斯·巴罗（暗算） ----
        BossSkill barrowBackstab = new BossSkill("背后暗算", "托马斯从暗中发动致命袭击，造成大量伤害", 7, 2);
        BossSkill barrowPoison = new BossSkill("下毒", "在食物中下毒，造成持续伤害并降低防御", 3, 3);
        BossSkill barrowFeint = new BossSkill("佯攻", "虚晃一枪后快速出击，造成中等伤害", 4, 1);
        List<BossSkill> barrowSkills = new ArrayList<>(Arrays.asList(barrowBackstab, barrowPoison, barrowFeint));

        Map<Integer, BossData> daBosses = new HashMap<>();
        BossData barrow = new BossData("da_act1_boss", "托马斯·巴罗", "Thomas Barrow", 35, barrowSkills, 50);
        daBosses.put(1, barrow);

        // ---- Act 2: 奥布莱恩（阴谋） ----
        BossSkill obrienCurse = new BossSkill("恶毒诅咒", "奥布莱恩散布恶毒诅咒，削弱敌人的攻击力", 0, 2);
        BossSkill obrienFrame = new BossSkill("栽赃陷害", "在敌人房间藏匿赃物，造成大量伤害并引起混乱", 5, 3);
        BossSkill obrienRumor = new BossSkill("散布谣言", "编造谣言毁坏敌人名誉，造成中等伤害", 3, 1);
        List<BossSkill> obrienSkills = new ArrayList<>(Arrays.asList(obrienCurse, obrienFrame, obrienRumor));

        BossData obrien = new BossData("da_act2_boss", "奥布莱恩", "Mrs. O'Brien", 45, obrienSkills, 50);
        daBosses.put(2, obrien);

        // ---- Act 3: 罗斯伯爵夫人（社交） ----
        BossSkill rosheCharm = new BossSkill("社交舞会", "伯爵夫人以优雅舞姿魅惑对手，使其丧失攻击欲望", 0, 2);
        BossSkill rosheGossip = new BossSkill("散布流言", "利用社交圈散布流言，造成伤害并降低士气", 4, 2);
        BossSkill rosheHeal = new BossSkill("优雅回血", "伯爵夫人品茶休息，恢复大量生命值", 0, 4);
        BossSkill rosheTeaParty = new BossSkill("茶会邀请", "邀请敌人参加茶会，在轻松氛围中削弱对方", 3, 3);
        List<BossSkill> rosheSkills = new ArrayList<>(Arrays.asList(rosheCharm, rosheGossip, rosheHeal, rosheTeaParty));

        BossData roshe = new BossData("da_act3_boss", "罗斯伯爵夫人", "Countess Rosse", 55, rosheSkills, 50);
        daBosses.put(3, roshe);

        BOSS_REGISTRY.put(showKey, daBosses);
    }

    /**
     * 克隆一个BossData副本，避免静态数据被修改
     */
    private BossData cloneBoss(BossData original) {
        List<BossSkill> clonedSkills = new ArrayList<>();
        for (BossSkill skill : original.getSkills()) {
            clonedSkills.add(new BossSkill(
                    skill.getName(),
                    skill.getDescription(),
                    skill.getDamage(),
                    skill.getCooldown()
            ));
        }
        BossData clone = new BossData(
                original.getId(),
                original.getNameCn(),
                original.getNameEn(),
                original.getMaxHp(),
                clonedSkills,
                original.getPhaseThreshold()
        );
        clone.setCurrentHp(original.getMaxHp());
        clone.setCanResurrect(original.isCanResurrect());
        clone.setResurrectHp(original.getResurrectHp());
        return clone;
    }

    // ============================================================
    // 公开API方法
    // ============================================================

    /**
     * 获取指定剧集和Act的Boss数据（含完整技能列表）
     *
     * @param showId 剧集ID
     * @param act    Act序号(1-3)
     * @return BossData包含所有技能，如果Boss不存在返回null
     */
    public BossData getBossForAct(Long showId, Integer act) {
        String showKey = String.valueOf(showId);
        Map<Integer, BossData> actBosses = BOSS_REGISTRY.get(showKey);
        if (actBosses == null) {
            return null;
        }
        BossData original = actBosses.get(act);
        if (original == null) {
            return null;
        }
        // 返回克隆副本，保证每次调用都是全新实例
        return cloneBoss(original);
    }

    /**
     * 执行Boss本回合行动
     * <p>
     * Boss根据以下逻辑选择技能：
     * 1. 如果Boss可复活且还未使用复活且当前血量<=0，触发复活
     * 2. 根据当前回合数、阶段和技能冷却选择可用技能
     * 3. 有特定阶段行为：第二阶段Boss攻击更凶猛/使用更强技能
     * 4. 优先使用冷却已结束的高伤害技能
     *
     * @param boss        Boss数据
     * @param currentTurn 当前回合数
     * @param battleState 当前战斗状态映射（可扩张，预留扩展）
     * @return BossAction包含本回合行动详情
     */
    public BossAction executeBossTurn(BossData boss, int currentTurn, Map<String, Object> battleState) {
        boss.setCurrentTurn(currentTurn);
        BossAction action = new BossAction();
        Random rand = ThreadLocalRandom.current();

        // ---- 1. 检查复活逻辑 ----
        if (boss.isCanResurrect() && !boss.isHasResurrected() && boss.isDead()) {
            boss.setHasResurrected(true);
            boss.setCurrentHp(boss.getResurrectHp());
            action.setResurrected(true);
            action.setDescription(boss.getNameCn() + " 破冰重生，恢复了 " + boss.getResurrectHp() + " 点生命值！");

            // 复活后Boss清空冷却，重新进入战斗
            for (BossSkill skill : boss.getSkills()) {
                skill.setCurrentCooldown(0);
            }
            return action;
        }

        // ---- 2. Boss回合更新 ----
        boss.tickTurn();

        // ---- 3. 根据阶段和回合选择技能 ----
        int phase = boss.getPhase();
        List<BossSkill> skills = boss.getSkills();
        List<BossSkill> availableSkills = new ArrayList<>();

        for (BossSkill skill : skills) {
            if (skill.isReady()) {
                availableSkills.add(skill);
            }
        }

        // 如果没有可用技能，使用普通攻击
        if (availableSkills.isEmpty()) {
            int baseDamage = phase == 2 ? 3 : 2;  // 第二阶段基础伤害更高
            action.setSkillName("普通攻击");
            action.setDescription(boss.getNameCn() + " 发动了普通攻击！");
            action.setDamageToPlayer(baseDamage);
            action.setAoe(false);
            return action;
        }

        // ---- 根据Boss id选择行为模式 ----
        BossSkill chosenSkill = selectSkillByBossPattern(boss, availableSkills, phase, currentTurn, rand);

        if (chosenSkill == null) {
            // 备选：随机选一个可用技能
            chosenSkill = availableSkills.get(rand.nextInt(availableSkills.size()));
        }

        // ---- 4. 执行技能效果 ----
        chosenSkill.use(); // 设置冷却
        action.setSkillName(chosenSkill.getName());

        switch (chosenSkill.getName()) {
            // === GoT Boss技能 ===
            case "巨力猛击":
                action.setDescription("WunWun挥舞巨大木棍猛击，造成 " + chosenSkill.getDamage() + " 点伤害！");
                action.setDamageToPlayer(chosenSkill.getDamage() + (phase == 2 ? 2 : 0));
                action.setAoe(false);
                break;

            case "震地践踏":
                action.setDescription("WunWun重重踩踏地面，对全体造成 " + chosenSkill.getDamage() + " 点伤害！");
                action.setDamageToPlayer(chosenSkill.getDamage() + (phase == 2 ? 1 : 0));
                action.setAoe(true);
                break;

            case "战吼":
                action.setDescription("WunWun发出震耳欲聋的战吼，造成 " + chosenSkill.getDamage() + " 点伤害并震慑敌人！");
                action.setDamageToPlayer(chosenSkill.getDamage());
                action.setAoe(false);
                break;

            case "召唤金袍卫":
                action.setDescription("瑟曦召唤金袍卫队前来助战！");
                action.setDamageToPlayer(0);
                action.getSummonedMinions().add("金袍卫兵");
                if (phase == 2) {
                    action.getSummonedMinions().add("御林铁卫");
                }
                break;

            case "野火焚城":
                action.setDescription("瑟曦引爆野火，对全体造成 " + (chosenSkill.getDamage() + (phase == 2 ? 2 : 0)) + " 点火焰伤害！");
                action.setDamageToPlayer(chosenSkill.getDamage() + (phase == 2 ? 2 : 0));
                action.setAoe(true);
                break;

            case "毒酒暗算":
                action.setDescription("瑟曦在酒中下毒，造成 " + chosenSkill.getDamage() + " 点伤害并削弱对手！");
                action.setDamageToPlayer(chosenSkill.getDamage());
                action.setAoe(false);
                break;

            case "冰霜吐息":
                action.setDescription("夜王释放极寒冰霜吐息，对全体造成 " + (chosenSkill.getDamage() + (phase == 2 ? 2 : 0)) + " 点冰冻伤害！");
                action.setDamageToPlayer(chosenSkill.getDamage() + (phase == 2 ? 2 : 0));
                action.setAoe(true);
                break;

            case "复活死者":
                action.setDescription("夜王举起双臂，复活尸鬼加入战斗！");
                action.setDamageToPlayer(0);
                int minionCount = phase == 2 ? 3 : 2;
                for (int i = 0; i < minionCount; i++) {
                    action.getSummonedMinions().add("尸鬼");
                }
                break;

            case "冰矛投掷":
                int iceSpearDamage = chosenSkill.getDamage() + (phase == 2 ? 3 : 0);
                action.setDescription("夜王投掷冰矛，造成 " + iceSpearDamage + " 点贯穿伤害！");
                action.setDamageToPlayer(iceSpearDamage);
                action.setAoe(false);
                break;

            case "暴风雪":
                int blizzardDamage = chosenSkill.getDamage() + (phase == 2 ? 2 : 0);
                action.setDescription("夜王召唤暴风雪，对全体造成 " + blizzardDamage + " 点冰冻伤害并冻结敌人！");
                action.setDamageToPlayer(blizzardDamage);
                action.setAoe(true);
                break;

            // === DA Boss技能 ===
            case "背后暗算":
                int backstabDamage = chosenSkill.getDamage() + (phase == 2 ? 3 : 0);
                action.setDescription("托马斯从暗中发动致命袭击，造成 " + backstabDamage + " 点伤害！");
                action.setDamageToPlayer(backstabDamage);
                action.setAoe(false);
                break;

            case "下毒":
                int poisonDamage = chosenSkill.getDamage() + (phase == 2 ? 2 : 0);
                action.setDescription("托马斯在食物中下毒，造成 " + poisonDamage + " 点伤害并削弱防御！");
                action.setDamageToPlayer(poisonDamage);
                action.setAoe(false);
                break;

            case "佯攻":
                action.setDescription("托马斯虚晃一枪后快速出击，造成 " + chosenSkill.getDamage() + " 点伤害！");
                action.setDamageToPlayer(chosenSkill.getDamage());
                action.setAoe(false);
                break;

            case "恶毒诅咒":
                BossBuff weakenBuff = new BossBuff("虚弱诅咒", "攻击力降低", phase == 2 ? 4 : 3);
                action.setDescription("奥布莱恩散布恶毒诅咒，使敌人陷入虚弱状态(" + weakenBuff.getDuration() + "回合)！");
                action.setDamageToPlayer(1);
                action.getBuffsApplied().add(weakenBuff);
                break;

            case "栽赃陷害":
                int frameDamage = chosenSkill.getDamage() + (phase == 2 ? 2 : 0);
                action.setDescription("奥布莱恩在敌人房间藏匿赃物，造成 " + frameDamage + " 点伤害并引起混乱！");
                action.setDamageToPlayer(frameDamage);
                action.setAoe(false);
                break;

            case "散布谣言":
                int rumorDamage = chosenSkill.getDamage() + (phase == 2 ? 1 : 0);
                action.setDescription("奥布莱恩编造谣言毁坏敌人名誉，造成 " + rumorDamage + " 点伤害！");
                action.setDamageToPlayer(rumorDamage);
                action.setAoe(false);
                break;

            case "社交舞会":
                BossBuff charmBuff = new BossBuff("魅惑", "攻击力降低", phase == 2 ? 3 : 2);
                action.setDescription("罗斯伯爵夫人以优雅舞姿魅惑对手(" + charmBuff.getDuration() + "回合)，使其丧失攻击欲望！");
                action.setDamageToPlayer(0);
                action.getBuffsApplied().add(charmBuff);
                break;

            case "散布流言":
                int gossipDamage = chosenSkill.getDamage() + (phase == 2 ? 2 : 0);
                BossBuff moraleBuff = new BossBuff("士气低落", "造成的伤害降低", phase == 2 ? 3 : 2);
                action.setDescription("罗斯伯爵夫人散布流言，造成 " + gossipDamage + " 点伤害并降低士气(" + moraleBuff.getDuration() + "回合)！");
                action.setDamageToPlayer(gossipDamage);
                action.getBuffsApplied().add(moraleBuff);
                break;

            case "优雅回血":
                int healAmount = phase == 2 ? 14 : 10;
                boss.heal(healAmount);
                action.setDescription("罗斯伯爵夫人品茶休息，恢复了 " + healAmount + " 点生命值！");
                action.setHealAmount(healAmount);
                action.setDamageToPlayer(0);
                break;

            case "茶会邀请":
                int teaDamage = chosenSkill.getDamage() + (phase == 2 ? 1 : 0);
                action.setDescription("罗斯伯爵夫人邀请敌人参加茶会，在轻松氛围中造成 " + teaDamage + " 点伤害！");
                action.setDamageToPlayer(teaDamage);
                action.setAoe(false);
                break;

            default:
                // 默认技能效果
                action.setDescription(boss.getNameCn() + " 使用了 " + chosenSkill.getName() + "，造成 " + chosenSkill.getDamage() + " 点伤害！");
                action.setDamageToPlayer(chosenSkill.getDamage());
                action.setAoe(false);
                break;
        }

        // ---- 5. 检查阶段转换 ----
        if (boss.shouldPhaseTransition()) {
            boss.setPhase(2);
            action.setPhaseTransition(true);
            action.setDescription(action.getDescription()
                    + " " + boss.getNameCn() + " 进入第二阶段！攻击更加凶猛！");
        }

        return action;
    }

    /**
     * 根据Boss不同行为模式选择技能
     */
    private BossSkill selectSkillByBossPattern(BossData boss, List<BossSkill> available,
                                                int phase, int currentTurn, Random rand) {
        String bossId = boss.getId();
        Map<String, Object> aiState = new HashMap<>();
        aiState.put("phase", phase);
        aiState.put("currentTurn", currentTurn);
        aiState.put("bossHpPercent", boss.getCurrentHp() * 100 / boss.getMaxHp());
        aiState.put("hasSummonedMinions", boss.getBuffs().stream().anyMatch(b -> b.getName().contains("召唤")));

        // WunWun: 有巨力猛击就用，否则战吼
        if ("got_act1_boss".equals(bossId)) {
            BossSkill smash = findSkillByName(available, "巨力猛击");
            if (smash != null) return smash;
            BossSkill stomp = findSkillByName(available, "震地践踏");
            if (stomp != null && phase == 2) return stomp;
            BossSkill roar = findSkillByName(available, "战吼");
            if (roar != null) return roar;
        }

        // 瑟曦: 优先野火焚城，然后召唤，然后毒酒
        if ("got_act2_boss".equals(bossId)) {
            BossSkill wildfire = findSkillByName(available, "野火焚城");
            if (wildfire != null && currentTurn >= 3) return wildfire;
            BossSkill summon = findSkillByName(available, "召唤金袍卫");
            if (summon != null && currentTurn % 2 == 0) return summon;
            BossSkill poison = findSkillByName(available, "毒酒暗算");
            if (poison != null) return poison;
        }

        // 夜王: 阶段1优先冰矛和吐息，阶段2优先暴风雪和复活死者
        if ("got_act3_boss".equals(bossId)) {
            if (phase == 2) {
                BossSkill blizzard = findSkillByName(available, "暴风雪");
                if (blizzard != null) return blizzard;
                BossSkill raise = findSkillByName(available, "复活死者");
                if (raise != null && currentTurn % 3 == 0) return raise;
            }
            BossSkill iceSpear = findSkillByName(available, "冰矛投掷");
            if (iceSpear != null && currentTurn % 2 == 1) return iceSpear;
            BossSkill frost = findSkillByName(available, "冰霜吐息");
            if (frost != null) return frost;
            BossSkill raise = findSkillByName(available, "复活死者");
            if (raise != null && currentTurn % 4 == 0) return raise;
        }

        // 托马斯·巴罗: 优先背后暗算
        if ("da_act1_boss".equals(bossId)) {
            BossSkill backstab = findSkillByName(available, "背后暗算");
            if (backstab != null && currentTurn % 2 == 1) return backstab;
            BossSkill poison = findSkillByName(available, "下毒");
            if (poison != null && phase == 2) return poison;
            BossSkill feint = findSkillByName(available, "佯攻");
            if (feint != null) return feint;
        }

        // 奥布莱恩: 优先诅咒和栽赃
        if ("da_act2_boss".equals(bossId)) {
            BossSkill curse = findSkillByName(available, "恶毒诅咒");
            if (curse != null && currentTurn % 3 == 1) return curse;
            BossSkill frame = findSkillByName(available, "栽赃陷害");
            if (frame != null && currentTurn % 2 == 0) return frame;
            BossSkill rumor = findSkillByName(available, "散布谣言");
            if (rumor != null) return rumor;
        }

        // 罗斯伯爵夫人: 优先回血(HP低时)，然后社交舞会，然后流言
        if ("da_act3_boss".equals(bossId)) {
            int hpPercent = boss.getCurrentHp() * 100 / boss.getMaxHp();
            if (hpPercent < 40) {
                BossSkill heal = findSkillByName(available, "优雅回血");
                if (heal != null) return heal;
            }
            BossSkill charm = findSkillByName(available, "社交舞会");
            if (charm != null && currentTurn % 3 == 1) return charm;
            BossSkill gossip = findSkillByName(available, "散布流言");
            if (gossip != null && currentTurn % 2 == 0) return gossip;
            BossSkill tea = findSkillByName(available, "茶会邀请");
            if (tea != null) return tea;
        }

        // 回退：随机选择
        return available.isEmpty() ? null : available.get(rand.nextInt(available.size()));
    }

    /**
     * 根据技能名称在可用列表中查找技能
     */
    private BossSkill findSkillByName(List<BossSkill> skills, String name) {
        for (BossSkill skill : skills) {
            if (skill.getName().equals(name)) {
                return skill;
            }
        }
        return null;
    }

    /**
     * 获取Boss击败后的奖励列表
     * <p>
     * 不同Boss掉落不同层级的奖励：
     * - Act1 Boss: 基础奖励
     * - Act2 Boss: 中级奖励
     * - Act3 Boss: 高级奖励（含稀有物品）
     *
     * @param boss Boss数据
     * @return 奖励列表（Map格式，含type/label/value等字段）
     */
    public List<Map<String, Object>> getBossRewards(BossData boss) {
        List<Map<String, Object>> rewards = new ArrayList<>();
        Random rand = ThreadLocalRandom.current();

        String bossId = boss.getId();

        // ---- 通用奖励：金币 ----
        int goldAmount;
        if (bossId.contains("act3")) {
            goldAmount = 25 + rand.nextInt(10); // 25-34
        } else if (bossId.contains("act2")) {
            goldAmount = 18 + rand.nextInt(8);  // 18-25
        } else {
            goldAmount = 12 + rand.nextInt(6);  // 12-17
        }
        Map<String, Object> goldReward = new HashMap<>();
        goldReward.put("type", "gold");
        goldReward.put("value", goldAmount);
        goldReward.put("label", "获得 " + goldAmount + " 金币");
        rewards.add(goldReward);

        // ---- 生命恢复 ----
        int healAmount;
        if (bossId.contains("act3")) {
            healAmount = 20;
        } else if (bossId.contains("act2")) {
            healAmount = 15;
        } else {
            healAmount = 10;
        }
        Map<String, Object> healReward = new HashMap<>();
        healReward.put("type", "heal");
        healReward.put("healAmount", healAmount);
        healReward.put("label", "回复 " + healAmount + " 点生命值");
        rewards.add(healReward);

        // ---- Boss特有掉落（根据Boss id定制） ----
        String bossRewardLabel;
        String bossRewardType;

        switch (bossId) {
            case "got_act1_boss":
                bossRewardType = "relic";
                bossRewardLabel = "掉落：巨人碎骨（攻击力+2）";
                break;
            case "got_act2_boss":
                bossRewardType = "relic";
                bossRewardLabel = "掉落：铁王座碎片（全属性+1）";
                break;
            case "got_act3_boss":
                bossRewardType = "rare_relic";
                bossRewardLabel = "掉落：龙晶匕首（对Boss伤害+50%）";
                break;
            case "da_act1_boss":
                bossRewardType = "relic";
                bossRewardLabel = "掉落：管家钥匙（打开隐藏宝箱）";
                break;
            case "da_act2_boss":
                bossRewardType = "relic";
                bossRewardLabel = "掉落：阴谋信件（每回合抽牌+1）";
                break;
            case "da_act3_boss":
                bossRewardType = "rare_relic";
                bossRewardLabel = "掉落：伯爵夫人项链（生命上限+10）";
                break;
            default:
                bossRewardType = "gold";
                bossRewardLabel = "额外获得5金币";
                break;
        }

        Map<String, Object> bossSpecificReward = new HashMap<>();
        bossSpecificReward.put("type", bossRewardType);
        bossSpecificReward.put("label", bossRewardLabel);
        bossSpecificReward.put("bossId", bossId);
        rewards.add(bossSpecificReward);

        // ---- 额外稀有奖励（Act3 Boss专属） ----
        if (bossId.contains("act3")) {
            Map<String, Object> rareReward = new HashMap<>();
            rareReward.put("type", "card");
            rareReward.put("label", "随机获得一张稀有卡牌");
            rareReward.put("rarity", "rare");
            rewards.add(rareReward);
        }

        return rewards;
    }

    /**
     * 检查指定剧集是否有Boss
     *
     * @param showId 剧集ID
     * @return 是否有已注册的Boss
     */
    public boolean hasBossForShow(Long showId) {
        return BOSS_REGISTRY.containsKey(String.valueOf(showId));
    }

    /**
     * 检查指定剧集和Act是否有Boss
     *
     * @param showId 剧集ID
     * @param act    Act序号
     * @return 是否有已注册的Boss
     */
    public boolean hasBossForAct(Long showId, Integer act) {
        Map<Integer, BossData> actBosses = BOSS_REGISTRY.get(String.valueOf(showId));
        return actBosses != null && actBosses.containsKey(act);
    }

    /**
     * 获取所有有Boss注册的剧集ID列表
     *
     * @return 剧集ID列表
     */
    public List<Long> getAllBossShowIds() {
        List<Long> ids = new ArrayList<>();
        for (String key : BOSS_REGISTRY.keySet()) {
            ids.add(Long.parseLong(key));
        }
        return ids;
    }
}
