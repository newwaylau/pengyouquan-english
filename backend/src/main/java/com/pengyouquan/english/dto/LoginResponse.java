package com.pengyouquan.english.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录响应 DTO
 */
@Data
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String email;
    private String nickname;
    private String avatar;
    private String role;
}
