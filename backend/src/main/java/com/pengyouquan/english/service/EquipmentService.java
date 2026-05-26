package com.pengyouquan.english.service;

import com.pengyouquan.english.model.*;
import com.pengyouquan.english.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final UserEquipmentRepository userEquipmentRepository;
    private final HeroGearRepository heroGearRepository;
    private final UserRepository userRepository;

    public EquipmentService(EquipmentRepository equipmentRepository,
                            UserEquipmentRepository userEquipmentRepository,
                            HeroGearRepository heroGearRepository,
                            UserRepository userRepository) {
        this.equipmentRepository = equipmentRepository;
        this.userEquipmentRepository = userEquipmentRepository;
        this.heroGearRepository = heroGearRepository;
        this.userRepository = userRepository;
    }

    private static final Map<String, Integer> RARITY_ORDER = Map.of(
            "common", 1, "rare", 2, "epic", 3, "legendary", 4
    );

    private static final Map<String, Integer> UPGRADE_COSTS = Map.of(
            "common", 50, "rare", 150, "epic", 400, "legendary", 1000
    );

    private static final Map<String, Integer> REROLL_COSTS = Map.of(
            "common", 30, "rare", 100, "epic", 300, "legendary", 800
    );

    public static List<String> ALL_SLOTS = List.of("weapon", "armor", "trinket", "tome", "crown");

    public static Map<String, String> SLOT_CN = Map.of(
            "weapon", "武器", "armor", "护甲", "trinket", "饰品", "tome", "典籍", "crown", "头冠"
    );

    /**
     * 获取全部装备及用户持有状态
     */
    public Map<String, Object> getAvailableEquipment(Long userId) {
        List<Equipment> allEquipment = equipmentRepository.findAllByOrderBySlotAsc();
        List<UserEquipment> owned = userEquipmentRepository.findByUserId(userId);
        Map<Long, Integer> ownedMap = owned.stream()
                .collect(Collectors.toMap(UserEquipment::getEquipmentId, UserEquipment::getQuantity));

        Map<Long, Long> ueIdMap = owned.stream()
                .collect(Collectors.toMap(UserEquipment::getEquipmentId, UserEquipment::getId,
                        (a, b) -> a));

        List<Map<String, Object>> equipmentList = allEquipment.stream().map(eq -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", eq.getId());
            item.put("nameCn", eq.getNameCn());
            item.put("nameEn", eq.getNameEn());
            item.put("slot", eq.getSlot());
            item.put("rarity", eq.getRarity());
            item.put("statBonus", parseJson(eq.getStatBonus()));
            item.put("effectJson", parseJson(eq.getEffectJson()));
            item.put("affinityShowId", eq.getAffinityShowId());
            item.put("unlockCondition", eq.getUnlockCondition());
            item.put("owned", ownedMap.getOrDefault(eq.getId(), 0));
            item.put("userEquipmentId", ueIdMap.get(eq.getId()));
            return item;
        }).collect(Collectors.toList());

        // 按槽位分组
        Map<String, List<Map<String, Object>>> grouped = new LinkedHashMap<>();
        for (String slot : ALL_SLOTS) {
            List<Map<String, Object>> slotItems = equipmentList.stream()
                    .filter(e -> slot.equals(e.get("slot")))
                    .collect(Collectors.toList());
            if (!slotItems.isEmpty()) {
                grouped.put(slot, slotItems);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("equipment", equipmentList);
        result.put("grouped", grouped);
        result.put("slots", ALL_SLOTS);
        result.put("slotNames", SLOT_CN);

        // 当前装备配置
        result.put("currentGear", getEquippedGear(userId));
        result.put("setBonuses", getSetBonuses(userId));

        return result;
    }

    /**
     * 获取当前装备配置
     */
    public Map<String, Object> getEquippedGear(Long userId) {
        HeroGear gear = heroGearRepository.findByUserId(userId).orElse(null);
        if (gear == null) {
            Map<String, Object> empty = new HashMap<>();
            for (String slot : ALL_SLOTS) {
                empty.put(slot, null);
            }
            return empty;
        }

        Map<String, Object> result = new HashMap<>();
        Map<Long, Equipment> cache = new HashMap<>();

        result.put("weapon", getEquippedItem(gear.getWeaponId(), cache));
        result.put("armor", getEquippedItem(gear.getArmorId(), cache));
        result.put("trinket", getEquippedItem(gear.getTrinketId(), cache));
        result.put("tome", getEquippedItem(gear.getTomeId(), cache));
        result.put("crown", getEquippedItem(gear.getCrownId(), cache));

        return result;
    }

    private Map<String, Object> getEquippedItem(Long userEquipmentId, Map<Long, Equipment> cache) {
        if (userEquipmentId == null) return null;
        Optional<UserEquipment> ueOpt = userEquipmentRepository.findById(userEquipmentId);
        if (ueOpt.isEmpty()) return null;
        UserEquipment ue = ueOpt.get();

        Equipment eq = cache.computeIfAbsent(ue.getEquipmentId(),
                id -> equipmentRepository.findById(id).orElse(null));
        if (eq == null) return null;

        Map<String, Object> item = new HashMap<>();
        item.put("userEquipmentId", ue.getId());
        item.put("equipmentId", eq.getId());
        item.put("nameCn", eq.getNameCn());
        item.put("nameEn", eq.getNameEn());
        item.put("slot", eq.getSlot());
        item.put("rarity", eq.getRarity());
        item.put("statBonus", parseJson(eq.getStatBonus()));
        item.put("effectJson", parseJson(eq.getEffectJson()));
        return item;
    }

    /**
     * 穿装备
     */
    @Transactional
    public Map<String, Object> equipItem(Long userId, Long userEquipmentId, String slot) {
        UserEquipment ue = userEquipmentRepository.findById(userEquipmentId)
                .orElseThrow(() -> new IllegalStateException("装备不存在"));
        if (!ue.getUserId().equals(userId)) {
            throw new IllegalStateException("无权使用此装备");
        }

        Equipment eq = equipmentRepository.findById(ue.getEquipmentId())
                .orElseThrow(() -> new IllegalStateException("装备配置不存在"));

        if (!eq.getSlot().equals(slot)) {
            throw new IllegalStateException("装备槽位不匹配");
        }

        HeroGear gear = heroGearRepository.findByUserId(userId).orElseGet(() -> {
            HeroGear g = new HeroGear();
            g.setUserId(userId);
            return heroGearRepository.save(g);
        });

        // 设置对应槽位
        switch (slot) {
            case "weapon" -> gear.setWeaponId(userEquipmentId);
            case "armor" -> gear.setArmorId(userEquipmentId);
            case "trinket" -> gear.setTrinketId(userEquipmentId);
            case "tome" -> gear.setTomeId(userEquipmentId);
            case "crown" -> gear.setCrownId(userEquipmentId);
            default -> throw new IllegalStateException("无效槽位: " + slot);
        }

        heroGearRepository.save(gear);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("gear", getEquippedGear(userId));
        result.put("setBonuses", getSetBonuses(userId));
        return result;
    }

    /**
     * 通过 equipmentId 穿装备（省去前端查 userEquipmentId 的麻烦）
     */
    @Transactional
    public Map<String, Object> equipByEquipmentId(Long userId, Long equipmentId, String slot) {
        Optional<UserEquipment> ueOpt = userEquipmentRepository.findByUserIdAndEquipmentId(userId, equipmentId);
        if (ueOpt.isEmpty()) {
            throw new IllegalStateException("未拥有此装备");
        }
        return equipItem(userId, ueOpt.get().getId(), slot);
    }

    /**
     * 卸装备
     */
    @Transactional
    public Map<String, Object> unequipItem(Long userId, String slot) {
        HeroGear gear = heroGearRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("没有装备配置"));

        switch (slot) {
            case "weapon" -> gear.setWeaponId(null);
            case "armor" -> gear.setArmorId(null);
            case "trinket" -> gear.setTrinketId(null);
            case "tome" -> gear.setTomeId(null);
            case "crown" -> gear.setCrownId(null);
            default -> throw new IllegalStateException("无效槽位: " + slot);
        }

        heroGearRepository.save(gear);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("gear", getEquippedGear(userId));
        result.put("setBonuses", getSetBonuses(userId));
        return result;
    }

    /**
     * 计算当前激活的套装效果
     */
    public Map<String, Object> getSetBonuses(Long userId) {
        HeroGear gear = heroGearRepository.findByUserId(userId).orElse(null);
        if (gear == null) return Map.of("sets", List.of(), "activeStats", Map.of());

        // 收集所有已装备的 equipmentId
        List<Long> ueIds = new ArrayList<>();
        if (gear.getWeaponId() != null) ueIds.add(gear.getWeaponId());
        if (gear.getArmorId() != null) ueIds.add(gear.getArmorId());
        if (gear.getTrinketId() != null) ueIds.add(gear.getTrinketId());
        if (gear.getTomeId() != null) ueIds.add(gear.getTomeId());
        if (gear.getCrownId() != null) ueIds.add(gear.getCrownId());

        if (ueIds.isEmpty()) return Map.of("sets", List.of(), "activeStats", Map.of());

        // 获取装备详情
        List<Equipment> equipped = ueIds.stream()
                .map(id -> userEquipmentRepository.findById(id).orElse(null))
                .filter(Objects::nonNull)
                .map(ue -> equipmentRepository.findById(ue.getEquipmentId()).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 计算套装
        Map<String, List<Equipment>> sets = new HashMap<>();
        for (Equipment eq : equipped) {
            String effectJson = eq.getEffectJson();
            if (effectJson != null && effectJson.contains("\"set\"")) {
                try {
                    Map<String, Object> effect = parseJson(effectJson);
                    if (effect != null && effect.containsKey("set")) {
                        String setName = effect.get("set").toString();
                        sets.computeIfAbsent(setName, k -> new ArrayList<>()).add(eq);
                    }
                } catch (Exception ignored) {}
            }
        }

        List<Map<String, Object>> setList = new ArrayList<>();
        for (Map.Entry<String, List<Equipment>> entry : sets.entrySet()) {
            Map<String, Object> setInfo = new HashMap<>();
            setInfo.put("setName", entry.getKey());
            setInfo.put("count", entry.getValue().size());
            setInfo.put("total", 5);
            setInfo.put("active", entry.getValue().size() >= 3); // 3件以上激活套装
            setInfo.put("items", entry.getValue().stream().map(eq -> {
                Map<String, Object> item = new HashMap<>();
                item.put("nameCn", eq.getNameCn());
                item.put("slot", eq.getSlot());
                return item;
            }).collect(Collectors.toList()));
            setList.add(setInfo);
        }

        // 计算总属性加成
        Map<String, Object> totalStats = new HashMap<>();
        for (Equipment eq : equipped) {
            Map<String, Object> stats = parseJson(eq.getStatBonus());
            if (stats != null) {
                for (Map.Entry<String, Object> s : stats.entrySet()) {
                    totalStats.merge(s.getKey(), s.getValue(), (a, b) -> {
                        if (a instanceof Number && b instanceof Number) {
                            return ((Number) a).doubleValue() + ((Number) b).doubleValue();
                        }
                        return b;
                    });
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("sets", setList);
        result.put("activeStats", totalStats);
        return result;
    }

    /**
     * 升级装备（消耗星尘）
     */
    @Transactional
    public Map<String, Object> upgradeEquipment(Long userId, Long userEquipmentId) {
        UserEquipment ue = userEquipmentRepository.findById(userEquipmentId)
                .orElseThrow(() -> new IllegalStateException("装备不存在"));
        if (!ue.getUserId().equals(userId)) {
            throw new IllegalStateException("无权操作此装备");
        }

        Equipment eq = equipmentRepository.findById(ue.getEquipmentId())
                .orElseThrow(() -> new IllegalStateException("装备配置不存在"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("用户不存在"));

        int cost = UPGRADE_COSTS.getOrDefault(eq.getRarity(), 100);
        if (user.getStardust() < cost) {
            throw new IllegalStateException("星尘不足，需要 " + cost);
        }

        user.setStardust(user.getStardust() - cost);
        userRepository.save(user);

        // 升级：增加数量（track 升级次数）
        ue.setQuantity(ue.getQuantity() + 1);
        userEquipmentRepository.save(ue);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("newLevel", ue.getQuantity());
        result.put("stardustRemaining", user.getStardust());
        return result;
    }

    /**
     * 重铸属性（消耗星尘）
     */
    @Transactional
    public Map<String, Object> rerollStats(Long userId, Long userEquipmentId) {
        UserEquipment ue = userEquipmentRepository.findById(userEquipmentId)
                .orElseThrow(() -> new IllegalStateException("装备不存在"));
        if (!ue.getUserId().equals(userId)) {
            throw new IllegalStateException("无权操作此装备");
        }

        Equipment eq = equipmentRepository.findById(ue.getEquipmentId())
                .orElseThrow(() -> new IllegalStateException("装备配置不存在"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("用户不存在"));

        int cost = REROLL_COSTS.getOrDefault(eq.getRarity(), 50);
        if (user.getStardust() < cost) {
            throw new IllegalStateException("星尘不足，需要 " + cost);
        }

        user.setStardust(user.getStardust() - cost);
        userRepository.save(user);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("stardustRemaining", user.getStardust());
        return result;
    }

    /**
     * 解锁装备（首次获得）
     */
    @Transactional
    public Map<String, Object> grantEquipment(Long userId, Long equipmentId, int quantity) {
        Optional<UserEquipment> existing = userEquipmentRepository.findByUserIdAndEquipmentId(userId, equipmentId);
        if (existing.isPresent()) {
            UserEquipment ue = existing.get();
            ue.setQuantity(ue.getQuantity() + quantity);
            userEquipmentRepository.save(ue);
        } else {
            UserEquipment ue = new UserEquipment();
            ue.setUserId(userId);
            ue.setEquipmentId(equipmentId);
            ue.setQuantity(quantity);
            userEquipmentRepository.save(ue);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return result;
    }

    /**
     * 获取装备对英雄的加成值
     */
    public Map<String, Object> getTotalStatBonuses(Long userId) {
        HeroGear gear = heroGearRepository.findByUserId(userId).orElse(null);
        if (gear == null) return Map.of();

        Map<String, Object> totalStats = new HashMap<>();
        List<Long> ueIds = List.of(gear.getWeaponId(), gear.getArmorId(), gear.getTrinketId(),
                gear.getTomeId(), gear.getCrownId());

        for (Long ueId : ueIds) {
            if (ueId == null) continue;
            Optional<UserEquipment> ueOpt = userEquipmentRepository.findById(ueId);
            if (ueOpt.isEmpty()) continue;
            Optional<Equipment> eqOpt = equipmentRepository.findById(ueOpt.get().getEquipmentId());
            if (eqOpt.isEmpty()) continue;
            Map<String, Object> stats = parseJson(eqOpt.get().getStatBonus());
            if (stats != null) {
                for (Map.Entry<String, Object> s : stats.entrySet()) {
                    totalStats.merge(s.getKey(), s.getValue(), (a, b) -> {
                        if (a instanceof Number && b instanceof Number) {
                            return ((Number) a).doubleValue() + ((Number) b).doubleValue();
                        }
                        return b;
                    });
                }
            }
        }

        return totalStats;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJson(String json) {
        if (json == null || json.isBlank() || json.equals("{}")) return new HashMap<>();
        try {
            return com.fasterxml.jackson.databind.ObjectMapper.class
                    .cast(new com.fasterxml.jackson.databind.ObjectMapper())
                    .readValue(json, Map.class);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
}
