package com.pengyouquan.english.controller;

import com.pengyouquan.english.dto.*;
import com.pengyouquan.english.security.CurrentUserId;
import com.pengyouquan.english.service.AuthService;
import com.pengyouquan.english.service.EmailCodeService;
import com.pengyouquan.english.service.WechatAuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 认证接口
 * 注册 / 登录 / 发送验证码 / 获取当前用户信息
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final WechatAuthService wechatAuthService;
    private final EmailCodeService emailCodeService;

    public AuthController(AuthService authService,
                          WechatAuthService wechatAuthService,
                          EmailCodeService emailCodeService) {
        this.authService = authService;
        this.wechatAuthService = wechatAuthService;
        this.emailCodeService = emailCodeService;
    }

    /** 发送邮箱验证码 */
    @PostMapping("/send-code")
    public ApiResponse<Void> sendCode(@Valid @RequestBody SendCodeRequest request) {
        emailCodeService.sendCode(request.getEmail());
        return ApiResponse.success(null);
    }

    /** 用户注册（邮箱 + 手机号 + 验证码 + 密码 + 可选邀请码） */
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
