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
     * 忘记密码：发送验证码（检查邮箱是否已注册）
     */
    public void forgotPasswordSendCode(String email) {
        if (!userRepository.existsByEmail(email)) {
            throw new BusinessException("该邮箱未注册");
        }
        emailCodeService.sendCode(email);
    }

    /**
     * 重置密码（验证码 + 新密码）
     * 成功后返回新 JWT，用户自动登录
     */
    public LoginResponse resetPassword(ResetPasswordRequest request) {
        // 校验验证码
        emailCodeService.verifyCode(request.getEmail(), request.getCode());

        // 查找用户
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("该邮箱未注册"));

        // 更新密码（BCrypt 编码）
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);

        // 生成新 JWT
        String token = jwtUtil.generateToken(user.getId());

        return new LoginResponse(token, user.getEmail(), user.getNickname(),
                user.getAvatar(), user.getRole());
    }

    /**
     * 修改密码（登录用户通过旧密码 + 新密码修改）
     */
    public void updatePassword(Long userId, UpdatePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        // 校验旧密码
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException("旧密码错误");
        }

        // 校验新密码强度（至少8位，含字母和数字，同注册规则）
        String newPassword = request.getNewPassword();
        if (newPassword.length() < 8) {
            throw new BusinessException("密码长度至少8位");
        }
        if (!newPassword.matches(".*[a-zA-Z].*") || !newPassword.matches(".*\\d.*")) {
            throw new BusinessException("密码必须包含字母和数字");
        }

        // 新密码不能与旧密码相同
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new BusinessException("新密码不能与旧密码相同");
        }

        // 更新密码（BCrypt 编码）
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
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
