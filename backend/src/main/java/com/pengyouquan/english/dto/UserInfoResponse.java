package com.pengyouquan.english.dto;

import com.pengyouquan.english.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 用户信息响应 DTO
 */
@Data
@AllArgsConstructor
public class UserInfoResponse {
    private Long id;
    private String email;
    private String nickname;
    private String avatar;
    private String role;
    private String createdAt;
    private int prestige;
    private int rankTier;
    private int consecutiveDays;

    public static UserInfoResponse fromUser(User user) {
        return new UserInfoResponse(
            user.getId(),
            user.getEmail(),
            user.getNickname(),
            user.getAvatar(),
            user.getRole(),
            user.getCreatedAt() != null ? user.getCreatedAt().toString() : "",
            user.getPrestige() != null ? user.getPrestige() : 0,
            user.getRankTier() != null ? user.getRankTier() : 1,
            user.getConsecutiveDays() != null ? user.getConsecutiveDays() : 0
        );
    }
}
