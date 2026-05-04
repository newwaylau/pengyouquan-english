package com.pengyouquan.english.controller;

import com.pengyouquan.english.dto.*;
import com.pengyouquan.english.security.CurrentUserId;
import com.pengyouquan.english.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理接口
 * 资料修改/统计/搜索/禁用/删除
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** 修改个人资料 */
    @PutMapping("/profile")
    public ApiResponse<UserInfoResponse> updateProfile(
            @CurrentUserId Long userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        return ApiResponse.success(userService.updateProfile(userId, request));
    }

    /** 获取我的统计信息 */
    @GetMapping("/me/stats")
    public ApiResponse<UserStatsResponse> myStats(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        return ApiResponse.success(userService.getUserStats(userId));
    }

    /** 管理员：搜索用户 */
    @GetMapping("/search")
    public ApiResponse<List<UserInfoResponse>> search(
            @CurrentUserId Long userId,
            @RequestParam String q) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        userService.checkAdmin(userId);
        return ApiResponse.success(userService.searchUsers(q));
    }

    /** 管理员：禁用/解禁用户 */
    @PutMapping("/{targetId}/toggle-enabled")
    public ApiResponse<Void> toggleEnabled(
            @CurrentUserId Long userId,
            @PathVariable Long targetId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        userService.checkAdmin(userId);
        userService.toggleEnabled(targetId);
        return ApiResponse.success();
    }

    /** 管理员：删除用户 */
    @DeleteMapping("/{targetId}")
    public ApiResponse<Void> deleteUser(
            @CurrentUserId Long userId,
            @PathVariable Long targetId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        userService.checkAdmin(userId);
        userService.deleteUser(targetId);
        return ApiResponse.success();
    }
}
