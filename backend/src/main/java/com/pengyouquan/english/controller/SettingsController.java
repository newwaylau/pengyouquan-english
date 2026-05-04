package com.pengyouquan.english.controller;

import com.pengyouquan.english.dto.ApiResponse;
import com.pengyouquan.english.security.CurrentUserId;
import com.pengyouquan.english.service.SettingsService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户设置接口
 * 持久化练习偏好到服务端
 */
@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    /** 获取全部设置 */
    @GetMapping
    public ApiResponse<Map<String, String>> getSettings(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        return ApiResponse.success(settingsService.getAllSettings(userId));
    }

    /** 批量保存设置 */
    @PutMapping
    public ApiResponse<Void> saveSettings(@CurrentUserId Long userId,
                                          @RequestBody Map<String, String> settings) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        settingsService.saveSettings(userId, settings);
        return ApiResponse.success();
    }

    /** 获取单个设置 */
    @GetMapping("/{key}")
    public ApiResponse<String> getSetting(@CurrentUserId Long userId,
                                          @PathVariable String key) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        return ApiResponse.success(settingsService.getSetting(userId, key));
    }

    /** 删除单个设置 */
    @DeleteMapping("/{key}")
    public ApiResponse<Void> deleteSetting(@CurrentUserId Long userId,
                                           @PathVariable String key) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        settingsService.deleteSetting(userId, key);
        return ApiResponse.success();
    }
}
