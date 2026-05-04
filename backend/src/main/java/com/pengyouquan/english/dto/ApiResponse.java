package com.pengyouquan.english.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 统一 API 响应格式
 * 所有接口返回此格式，保证前后端数据格式一致
 */
@Data
@AllArgsConstructor
public class ApiResponse<T> {

    private int code;      // 状态码：200 成功，4xx 客户端错误，5xx 服务端错误
    private String message; // 提示信息
    private T data;        // 数据负载

    /** 成功响应（带数据） */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "success", data);
    }

    /** 成功响应（无数据） */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(200, "success", null);
    }

    /** 失败响应 */
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    /** 参数错误 */
    public static <T> ApiResponse<T> badRequest(String message) {
        return new ApiResponse<>(400, message, null);
    }

    /** 未授权 */
    public static <T> ApiResponse<T> unauthorized(String message) {
        return new ApiResponse<>(401, message, null);
    }

    /** 未找到 */
    public static <T> ApiResponse<T> notFound(String message) {
        return new ApiResponse<>(404, message, null);
    }

    /** 服务端错误 */
    public static <T> ApiResponse<T> serverError() {
        return new ApiResponse<>(500, "服务器内部错误", null);
    }
}
