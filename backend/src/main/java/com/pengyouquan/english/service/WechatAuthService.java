package com.pengyouquan.english.service;

import com.pengyouquan.english.config.GlobalExceptionHandler.BusinessException;
import com.pengyouquan.english.dto.LoginResponse;
import com.pengyouquan.english.model.User;
import com.pengyouquan.english.repository.UserRepository;
import com.pengyouquan.english.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 微信登录服务
 * - 小程序端通过 wx.login() 获取 code
 * - 服务端用 code 向微信服务器换取 openId
 * - 绑定或创建用户，返回 JWT
 */
@Service
public class WechatAuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final String appId;
    private final String appSecret;

    public WechatAuthService(UserRepository userRepository,
                             JwtUtil jwtUtil,
                             @Value("${wechat.app-id:}") String appId,
                             @Value("${wechat.app-secret:}") String appSecret) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.appId = appId;
        this.appSecret = appSecret;
    }

    /**
     * 微信登录
     * @param code 小程序 wx.login() 返回的临时 code
     */
    @SuppressWarnings("unchecked")
    public LoginResponse login(String code) {
        // 1. 用 code 向微信服务器换取 session_key 和 openId
        String url = String.format(
            "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
            appId, appSecret, code
        );

        Map<String, Object> result;
        try {
            result = new RestTemplate().getForObject(url, Map.class);
        } catch (Exception e) {
            throw new BusinessException("微信登录失败：" + e.getMessage());
        }

        if (result == null || result.containsKey("errcode")) {
            throw new BusinessException("微信登录失败：code 无效或已过期");
        }

        String openId = (String) result.get("openid");

        // 2. 查找或创建用户
        User user = userRepository.findByWechatOpenId(openId)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setWechatOpenId(openId);
                    newUser.setEmail("wx_" + openId.substring(0, 8) + "@pengyouquan.app");
                    newUser.setPassword("");
                    newUser.setNickname("微信用户");
                    newUser.setEnabled(true);
                    newUser.setRole("user");
                    return userRepository.save(newUser);
                });

        // 3. 生成 JWT
        String token = jwtUtil.generateToken(user.getId());
        return new LoginResponse(token, user.getEmail(), user.getNickname(),
                user.getAvatar(), user.getRole());
    }
}
