package com.pengyouquan.english.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 微信登录请求
 * 小程序前端通过 wx.login() 获取 code 后传过来
 */
@Data
public class WechatLoginRequest {
    @NotBlank(message = "微信code不能为空")
    private String code;
}
