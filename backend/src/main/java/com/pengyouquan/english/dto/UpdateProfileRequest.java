package com.pengyouquan.english.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改用户资料请求
 */
@Data
public class UpdateProfileRequest {
    @Size(max = 100, message = "昵称最长100字")
    private String nickname;
    private String avatar;
}
