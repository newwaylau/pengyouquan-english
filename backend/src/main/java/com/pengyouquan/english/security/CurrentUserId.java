package com.pengyouquan.english.security;

import java.lang.annotation.*;

/**
 * 从 JWT Token 中提取当前用户 ID
 * 用法：@CurrentUserId Long userId
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentUserId {
}
