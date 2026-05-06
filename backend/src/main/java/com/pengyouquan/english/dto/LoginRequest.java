package com.pengyouquan.english.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求 DTO
 */
@Data
public class LoginRequest {

    @NotBlank(message = "邮箱/手机号不能为空")
    private String account;

    @NotBlank(message = "密码不能为空")
    private String password;
}
