package com.pengyouquan.english.controller;

import com.pengyouquan.english.dto.*;
import com.pengyouquan.english.security.CurrentUserId;
import com.pengyouquan.english.service.AuthService;
import com.pengyouquan.english.service.WechatAuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 认证接口
 * 注册 / 登录 / 获取当前用户信息
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final WechatAuthService wechatAuthService;

    public AuthController(AuthService authService, WechatAuthService wechatAuthService) {
        this.authService = authService;
        this.wechatAuthService = wechatAuthService;
    }

    /** 用户注册 */
    @PostMapping("/register")
    public ApiResponse<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.register(request));
    }

    /** 用户登录 */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    /** 获取当前用户信息（需要 JWT Token） */
    @GetMapping("/me")
    public ApiResponse<UserInfoResponse> me(@CurrentUserId Long userId) {
        if (userId == null) {
            return ApiResponse.unauthorized("未登录");
        }
        return ApiResponse.success(authService.getUserInfo(userId));
    }

    /** 微信小程序登录 */
    @PostMapping("/wechat-login")
    public ApiResponse<LoginResponse> wechatLogin(@Valid @RequestBody WechatLoginRequest request) {
        return ApiResponse.success(wechatAuthService.login(request.getCode()));
    }
}
