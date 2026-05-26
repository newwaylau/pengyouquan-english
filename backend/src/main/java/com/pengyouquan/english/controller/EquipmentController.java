package com.pengyouquan.english.controller;

import com.pengyouquan.english.dto.ApiResponse;
import com.pengyouquan.english.security.CurrentUserId;
import com.pengyouquan.english.service.EquipmentService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/equipment")
public class EquipmentController {

    private final EquipmentService equipmentService;

    public EquipmentController(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> getAllEquipment(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        return ApiResponse.success(equipmentService.getAvailableEquipment(userId));
    }

    @GetMapping("/mine")
    public ApiResponse<Map<String, Object>> getMyGear(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        Map<String, Object> result = Map.of(
                "gear", equipmentService.getEquippedGear(userId),
                "setBonuses", equipmentService.getSetBonuses(userId)
        );
        return ApiResponse.success(result);
    }

    @PostMapping("/equip/{userEquipmentId}/{slot}")
    public ApiResponse<Map<String, Object>> equip(
            @CurrentUserId Long userId,
            @PathVariable Long userEquipmentId,
            @PathVariable String slot) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        try {
            return ApiResponse.success(equipmentService.equipItem(userId, userEquipmentId, slot));
        } catch (IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/equip-by-id/{equipmentId}/{slot}")
    public ApiResponse<Map<String, Object>> equipByEquipmentId(
            @CurrentUserId Long userId,
            @PathVariable Long equipmentId,
            @PathVariable String slot) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        try {
            return ApiResponse.success(equipmentService.equipByEquipmentId(userId, equipmentId, slot));
        } catch (IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/unequip/{slot}")
    public ApiResponse<Map<String, Object>> unequip(
            @CurrentUserId Long userId,
            @PathVariable String slot) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        try {
            return ApiResponse.success(equipmentService.unequipItem(userId, slot));
        } catch (IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/upgrade/{userEquipmentId}")
    public ApiResponse<Map<String, Object>> upgrade(
            @CurrentUserId Long userId,
            @PathVariable Long userEquipmentId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        try {
            return ApiResponse.success(equipmentService.upgradeEquipment(userId, userEquipmentId));
        } catch (IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/reroll/{userEquipmentId}")
    public ApiResponse<Map<String, Object>> reroll(
            @CurrentUserId Long userId,
            @PathVariable Long userEquipmentId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        try {
            return ApiResponse.success(equipmentService.rerollStats(userId, userEquipmentId));
        } catch (IllegalStateException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getBonuses(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        return ApiResponse.success(equipmentService.getTotalStatBonuses(userId));
    }
}
