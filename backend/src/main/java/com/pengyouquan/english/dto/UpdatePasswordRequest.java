package com.pengyouquan.english.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改密码请求 DTO
 */
@Data
public class UpdatePasswordRequest {

    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 50, message = "密码长度8-50位")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d).{8,50}$", message = "密码需包含字母和数字")
    private String newPassword;
}
