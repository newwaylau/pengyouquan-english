package com.pengyouquan.english.controller;

import com.pengyouquan.english.dto.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Demo API — 流水线演示接口
 */
@RestController
@RequestMapping("/api/demo")
public class DemoController {

    @GetMapping("/greeting")
    public ApiResponse<Map<String, Object>> greeting(
            @RequestParam(value = "name", defaultValue = "访客") String name) {
        return ApiResponse.success(Map.of(
                "message", "你好, " + name + "! 欢迎使用朋友圈英语。",
                "serverTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        ));
    }
}
