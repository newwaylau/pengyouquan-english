package com.pengyouquan.english.service;

import com.pengyouquan.english.model.*;
import com.pengyouquan.english.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class HeroService {

    private final HeroRepository heroRepository;
    private final UserHeroRepository userHeroRepository;
    private final UserRepository userRepository;
    private final EquipmentService equipmentService;

    private static final Map<Integer, Integer> SKILL_UPGRADE_COSTS = Map.of(
            1, 100, 2, 200, 3, 400, 4, 800, 5, 1600
    );

    public HeroService(HeroRepository heroRepository,
                       UserHeroRepository userHeroRepository,
                       UserRepository userRepository,
                       EquipmentService equipmentService) {
        this.heroRepository = heroRepository;
        this.userHeroRepository = userHeroRepository;
        this.userRepository = userRepository;
        this.equipmentService = equipmentService;
    }

    /**
     * 获取全部英雄及用户解锁状态
     */
    public Map<String, Object> getAvailableHeroes(Long userId) {
        List<Hero> allHeroes = heroRepository.findAll();
        List<UserHero> userHeroes = userHeroRepository.findByUserId(userId);
        Map<Long, UserHero> userHeroMap = userHeroes.stream()
                .collect(Collectors.toMap(UserHero::getHeroId, uh -> uh));

        // 获取用户完成的句子数（用于判断解锁条件）
        User user = userRepository.findById(userId).orElse(null);
        int completedSentences = 0;
        int rankTier = (user != null) ? user.getRankTier() : 0;

        List<Map<String, Object>> heroList = allHeroes.stream().map(hero -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", hero.getId());
            item.put("showId", hero.getShowId());
            item.put("nameCn", hero.getNameCn());
            item.put("nameEn", hero.getNameEn());
            item.put("health", hero.getHealth());
            item.put("skillNameCn", hero.getSkillNameCn());
            item.put("skillNameEn", hero.getSkillNameEn());
            item.put("skillDescriptionCn", hero.getSkillDescriptionCn());
            item.put("skillDescriptionEn", hero.getSkillDescriptionEn());
            item.put("skillCooldown", hero.getSkillCooldown());
            item.put("baseEffectJson", parseJson(hero.getBaseEffectJson()));
            item.put("unlockCondition", hero.getUnlockCondition());

            UserHero uh = userHeroMap.get(hero.getId());
            boolean unlocked = uh != null;
            item.put("unlocked", unlocked);
            item.put("isActive", unlocked && uh.getIsActive());
            item.put("skillLevel", unlocked ? uh.getSkillLevel() : 0);
            return item;
        }).collect(Collectors.toList());

        // 当前激活的英雄
        Map<String, Object> activeHero = null;
        Optional<UserHero> active = userHeroRepository.findByUserIdAndIsActiveTrue(userId);
        if (active.isPresent()) {
            Optional<Hero> hOpt = heroRepository.findById(active.get().getHeroId());
            if (hOpt.isPresent()) {
                Hero h = hOpt.get();
                activeHero = new HashMap<>();
                activeHero.put("id", h.getId());
                activeHero.put("nameCn", h.getNameCn());
                activeHero.put("nameEn", h.getNameEn());
                activeHero.put("health", h.getHealth());
                activeHero.put("skillNameCn", h.getSkillNameCn());
                activeHero.put("skillNameEn", h.getSkillNameEn());
                activeHero.put("skillDescriptionCn", h.getSkillDescriptionCn());
                activeHero.put("skillDescriptionEn", h.getSkillDescriptionEn());
                activeHero.put("skillCooldown", h.getSkillCooldown());
                activeHero.put("skillLevel", active.get().getSkillLevel());
                activeHero.put("gear", equipmentService.getEquippedGear(userId));
                activeHero.put("statBonuses", equipmentService.getTotalStatBonuses(userId));
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("heroes", heroList);
        result.put("activeHero", activeHero);
        return result;
    }

    /**
     * 选择/切换英雄
     */
    @Transactional
    public Map<String, Object> selectHero(Long userId, Long heroId) {
        Optional<Hero> heroOpt = heroRepository.findById(heroId);
        if (heroOpt.isEmpty()) {
            throw new IllegalStateException("英雄不存在");
        }

        // 检查是否已解锁
        Optional<UserHero> uhOpt = userHeroRepository.findByUserIdAndHeroId(userId, heroId);
        UserHero uh;
        if (uhOpt.isEmpty()) {
            // 尝试解锁
            Hero hero = heroOpt.get();
            if (!"initial".equals(hero.getUnlockCondition())) {
                throw new IllegalStateException("英雄未解锁");
            }
            uh = new UserHero();
            uh.setUserId(userId);
            uh.setHeroId(heroId);
            uh.setIsActive(true);
            uh.setSkillLevel(1);
            userHeroRepository.save(uh);
        } else {
            uh = uhOpt.get();
            // 取消之前的激活
            Optional<UserHero> currentActive = userHeroRepository.findByUserIdAndIsActiveTrue(userId);
            currentActive.ifPresent(active -> {
                active.setIsActive(false);
                userHeroRepository.save(active);
            });
            uh.setIsActive(true);
            userHeroRepository.save(uh);
        }

        return getAvailableHeroes(userId);
    }

    /**
     * 获取当前激活的英雄及其装备
     */
    public Map<String, Object> getActiveHero(Long userId) {
        Map<String, Object> result = new HashMap<>();

        Optional<UserHero> active = userHeroRepository.findByUserIdAndIsActiveTrue(userId);
        if (active.isEmpty()) {
            result.put("active", false);
            return result;
        }

        UserHero uh = active.get();
        Optional<Hero> hOpt = heroRepository.findById(uh.getHeroId());
        if (hOpt.isEmpty()) {
            result.put("active", false);
            return result;
        }

        Hero h = hOpt.get();
        Map<String, Object> heroData = new HashMap<>();
        heroData.put("id", h.getId());
        heroData.put("nameCn", h.getNameCn());
        heroData.put("nameEn", h.getNameEn());
        heroData.put("health", h.getHealth());
        heroData.put("skillNameCn", h.getSkillNameCn());
        heroData.put("skillNameEn", h.getSkillNameEn());
        heroData.put("skillDescriptionCn", h.getSkillDescriptionCn());
        heroData.put("skillDescriptionEn", h.getSkillDescriptionEn());
        heroData.put("skillCooldown", h.getSkillCooldown());
        heroData.put("skillLevel", uh.getSkillLevel());
        heroData.put("baseEffectJson", parseJson(h.getBaseEffectJson()));

        result.put("active", true);
        result.put("hero", heroData);
        result.put("gear", equipmentService.getEquippedGear(userId));
        result.put("statBonuses", equipmentService.getTotalStatBonuses(userId));
        result.put("totalHealth", h.getHealth() + getHealthBonus(userId));

        return result;
    }

    /**
     * 升级英雄技能
     */
    @Transactional
    public Map<String, Object> upgradeSkill(Long userId) {
        Optional<UserHero> active = userHeroRepository.findByUserIdAndIsActiveTrue(userId);
        if (active.isEmpty()) {
            throw new IllegalStateException("未选择英雄");
        }

        UserHero uh = active.get();
        int currentLevel = uh.getSkillLevel();
        int maxLevel = 5;

        if (currentLevel >= maxLevel) {
            throw new IllegalStateException("技能已达最高等级");
        }

        int cost = SKILL_UPGRADE_COSTS.getOrDefault(currentLevel, 1000);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("用户不存在"));

        if (user.getStardust() < cost) {
            throw new IllegalStateException("星尘不足，需要 " + cost);
        }

        user.setStardust(user.getStardust() - cost);
        userRepository.save(user);

        uh.setSkillLevel(currentLevel + 1);
        userHeroRepository.save(uh);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("newLevel", currentLevel + 1);
        result.put("stardustRemaining", user.getStardust());
        return result;
    }

    private int getHealthBonus(Long userId) {
        Map<String, Object> stats = equipmentService.getTotalStatBonuses(userId);
        Object healthBonus = stats.get("health_bonus");
        if (healthBonus instanceof Number) {
            return ((Number) healthBonus).intValue();
        }
        return 0;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJson(String json) {
        if (json == null || json.isBlank() || json.equals("{}")) return new HashMap<>();
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, Map.class);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
}
