package com.pengyouquan.english.service;

import com.pengyouquan.english.config.GlobalExceptionHandler.BusinessException;
import com.pengyouquan.english.dto.*;
import com.pengyouquan.english.model.SystemSetting;
import com.pengyouquan.english.model.User;
import com.pengyouquan.english.repository.SystemSettingRepository;
import com.pengyouquan.english.repository.UserRepository;
import com.pengyouquan.english.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 认证服务
 * 处理注册、登录逻辑
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final SystemSettingRepository systemSettingRepository;
    private final EmailCodeService emailCodeService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       SystemSettingRepository systemSettingRepository,
                       EmailCodeService emailCodeService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.systemSettingRepository = systemSettingRepository;
        this.emailCodeService = emailCodeService;
    }

    /**
     * 用户注册（升级版：邮箱 + 手机号 + 验证码 + 密码 + 可选邀请码）
     */
    public LoginResponse register(RegisterRequest request) {
        // 检查注册是否开放
        SystemSetting regSetting = systemSettingRepository.findById("registrationEnabled").orElse(null);
        if (regSetting != null && "false".equals(regSetting.getSettingValue())) {
            throw new BusinessException("注册已关闭");
        }

        // 检查邮箱是否已注册
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("该邮箱已注册");
        }

        // 校验邮箱验证码
        emailCodeService.verifyCode(request.getEmail(), request.getCode());

        // 创建用户
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getEmail().split("@")[0]);
        user.setInvitedBy(request.getInvitedBy() != null ? request.getInvitedBy() : "");
        user.setEnabled(true);
        user.setRole("user");
        user = userRepository.save(user);

        // 生成 JWT
        String token = jwtUtil.generateToken(user.getId());

        return new LoginResponse(token, user.getEmail(), user.getNickname(),
                user.getAvatar(), user.getRole());
    }

    /**
     * 用户登录（支持邮箱或手机号）
     */
    public LoginResponse login(LoginRequest request) {
        String account = request.getAccount();

        // 根据输入内容判断是邮箱还是手机号
        User user;
        if (account.contains("@")) {
            user = userRepository.findByEmail(account).orElse(null);
        } else {
            user = userRepository.findByPhone(account).orElse(null);
        }

        // 统一提示，不暴露账号类型
        if (user == null || !user.getEnabled()
                || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("账号或密码错误");
        }

        // 生成 JWT
        String token = jwtUtil.generateToken(user.getId());

        return new LoginResponse(token, user.getEmail(), user.getNickname(),
                user.getAvatar(), user.getRole());
    }

    /**
     * 获取用户信息
     */
    public UserInfoResponse getUserInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        return UserInfoResponse.fromUser(user);
    }
}
