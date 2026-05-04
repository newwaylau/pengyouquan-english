package com.pengyouquan.english.controller;

import com.pengyouquan.english.dto.*;
import com.pengyouquan.english.security.CurrentUserId;
import com.pengyouquan.english.service.PracticeService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 练习系统接口
 * 记录练习/获取练习句子/错题本
 */
@RestController
@RequestMapping("/api")
public class PracticeController {

    private final PracticeService practiceService;

    public PracticeController(PracticeService practiceService) {
        this.practiceService = practiceService;
    }

    /** 记录一次练习结果 */
    @PostMapping("/practice/log")
    public ApiResponse<Void> logPractice(@CurrentUserId Long userId,
                                         @RequestBody Map<String, Object> body) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        Long sentenceId = Long.valueOf(body.get("sentenceId").toString());
        boolean correct = Boolean.parseBoolean(body.get("correct").toString());
        int correctCount = Integer.parseInt(body.getOrDefault("correctCount", "0").toString());
        int totalWords = Integer.parseInt(body.getOrDefault("totalWords", "0").toString());
        String mode = (String) body.getOrDefault("mode", "sentry");
        practiceService.logPractice(userId, sentenceId, correct, correctCount, totalWords, mode);
        return ApiResponse.success();
    }

    /** 获取错题本 */
    @GetMapping("/wrong-sentences")
    public ApiResponse<List<Map<String, Object>>> getWrongSentences(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        return ApiResponse.success(practiceService.getWrongSentences(userId));
    }

    /** 清空错题本 */
    @DeleteMapping("/wrong-sentences")
    public ApiResponse<Void> clearWrongSentences(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        practiceService.clearWrongSentences(userId);
        return ApiResponse.success();
    }

    /** 删除一条错题 */
    @DeleteMapping("/wrong-sentences/{sentenceId}")
    public ApiResponse<Void> removeWrongSentence(@CurrentUserId Long userId,
                                                 @PathVariable Long sentenceId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        practiceService.removeWrongSentence(userId, sentenceId);
        return ApiResponse.success();
    }
}
