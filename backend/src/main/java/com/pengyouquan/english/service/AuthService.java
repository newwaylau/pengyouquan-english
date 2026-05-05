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

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       SystemSettingRepository systemSettingRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.systemSettingRepository = systemSettingRepository;
    }

    /**
     * 用户注册
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

        // 创建用户
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getEmail().split("@")[0]);
        user.setEnabled(true);
        user.setRole("user");
        user = userRepository.save(user);

        // 生成 JWT
        String token = jwtUtil.generateToken(user.getId());

        return new LoginResponse(token, user.getEmail(), user.getNickname(),
                user.getAvatar(), user.getRole());
    }

    /**
     * 用户登录
     */
    public LoginResponse login(LoginRequest request) {
        // 查找用户
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("邮箱或密码错误"));

        // 检查是否被禁用
        if (!user.getEnabled()) {
            throw new BusinessException("账号已被禁用");
        }

        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("邮箱或密码错误");
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
