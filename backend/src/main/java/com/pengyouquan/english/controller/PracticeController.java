package com.pengyouquan.english.controller;

import com.pengyouquan.english.dto.*;
import com.pengyouquan.english.model.SentenceFlag;
import com.pengyouquan.english.repository.SentenceFlagRepository;
import com.pengyouquan.english.model.UserChest;
import com.pengyouquan.english.security.CurrentUserId;
import com.pengyouquan.english.service.ChestService;
import com.pengyouquan.english.service.PracticeService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 练习系统接口
 * 记录练习/获取练习句子/错题本/举报句子
 */
@RestController
@RequestMapping("/api")
public class PracticeController {

    private final PracticeService practiceService;
    private final SentenceFlagRepository sentenceFlagRepository;
    private final ChestService chestService;

    public PracticeController(PracticeService practiceService,
                              SentenceFlagRepository sentenceFlagRepository,
                              ChestService chestService) {
        this.practiceService = practiceService;
        this.sentenceFlagRepository = sentenceFlagRepository;
        this.chestService = chestService;
    }

    /** 记录一次练习结果（增强版：返回错题检查信息 + 宝箱进度） */
    @PostMapping("/practice/log")
    public ApiResponse<Map<String, Object>> logPractice(@CurrentUserId Long userId,
                                                        @RequestBody Map<String, Object> body) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        Long sentenceId = Long.valueOf(body.get("sentenceId").toString());
        boolean correct = Boolean.parseBoolean(body.get("correct").toString());
        int correctCount = Integer.parseInt(body.getOrDefault("correctCount", "0").toString());
        int totalWords = Integer.parseInt(body.getOrDefault("totalWords", "0").toString());
        String mode = (String) body.getOrDefault("mode", "sentry");
        Map<String, Object> result = practiceService.logPractice(userId, sentenceId, correct, correctCount, totalWords, mode);
        // 推进宝箱解锁进度（每个练习句推进1句）
        if (userId != null) {
            List<UserChest> chestUpdates = chestService.progressChest(userId, 1);
            result.put("chestUpdates", chestUpdates);
        }
        return ApiResponse.success(result);
    }

    /** 获取错题本（增强版：支持排序和筛选） */
    @GetMapping("/wrong-sentences")
    public ApiResponse<List<Map<String, Object>>> getWrongSentences(
            @CurrentUserId Long userId,
            @RequestParam(required = false) String showName,
            @RequestParam(required = false, defaultValue = "errorCount") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortDir,
            @RequestParam(required = false, defaultValue = "false") Boolean includeMastered) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        return ApiResponse.success(
            practiceService.getWrongSentences(userId, showName, sortBy, sortDir, includeMastered));
    }

    /** 获取错题本中所有不同的剧集名（用于筛选下拉） */
    @GetMapping("/wrong-sentences/shows")
    public ApiResponse<List<String>> getDistinctShowNames(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        return ApiResponse.success(practiceService.getDistinctShowNames(userId));
    }

    /** 标记已掌握 */
    @PutMapping("/wrong-sentences/{sentenceId}/master")
    public ApiResponse<Void> markMastered(@CurrentUserId Long userId,
                                          @PathVariable Long sentenceId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        practiceService.markMastered(userId, sentenceId);
        return ApiResponse.success();
    }

    /** 撤销已掌握 */
    @PutMapping("/wrong-sentences/{sentenceId}/unmaster")
    public ApiResponse<Void> unmarkMastered(@CurrentUserId Long userId,
                                            @PathVariable Long sentenceId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        practiceService.unmarkMastered(userId, sentenceId);
        return ApiResponse.success();
    }

    /** 获取批量练习的错题句子（仅今天要复习的） */
    @GetMapping("/wrong-sentences/practice")
    public ApiResponse<Map<String, Object>> getWrongPractice(
            @CurrentUserId Long userId,
            @RequestParam(required = false, defaultValue = "20") Integer limit) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        return ApiResponse.success(practiceService.getWrongPractice(userId, limit));
    }

    /**
     * 记录间隔复习结果
     * 调用此接口后，服务端自动更新 review_count 和 next_review_at
     */
    @PostMapping("/wrong-sentences/review")
    public ApiResponse<Void> updateReview(@CurrentUserId Long userId,
                                          @RequestBody Map<String, Object> body) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        Long sentenceId = Long.valueOf(body.get("sentenceId").toString());
        boolean correct = Boolean.parseBoolean(body.get("correct").toString());
        practiceService.updateReview(userId, sentenceId, correct);
        return ApiResponse.success();
    }

    /** 获取今天要复习的错题列表 */
    @GetMapping("/wrong-sentences/due")
    public ApiResponse<?> getDueWrongSentences(
            @CurrentUserId Long userId,
            @RequestParam(required = false, defaultValue = "20") Integer limit) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        return ApiResponse.success(practiceService.getDueWrongSentences(userId));
    }

    /** 获取间隔复习统计 */
    @GetMapping("/wrong-sentences/stats")
    public ApiResponse<Map<String, Object>> getReviewStats(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        return ApiResponse.success(practiceService.getReviewStats(userId));
    }

    /** 获取按间隔分组的错题列表 */
    @GetMapping("/wrong-sentences/grouped")
    public ApiResponse<Map<String, Object>> getWrongSentencesGrouped(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        return ApiResponse.success(practiceService.getWrongSentencesGrouped(userId));
    }

    /** 清空错题本 */
    @DeleteMapping("/wrong-sentences")
    public ApiResponse<Void> clearWrongSentences(@CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        practiceService.clearWrongSentences(userId);
        return ApiResponse.success();
    }

    /** 批量练习预览（按间隔分组排序） */
    @GetMapping("/wrong-sentences/with-review")
    public ApiResponse<List<Map<String, Object>>> getWrongSentencesWithReview(
            @CurrentUserId Long userId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        return ApiResponse.success(practiceService.getWrongSentencesWithReviewInfo(userId));
    }

    /** 删除一条错题 */
    @DeleteMapping("/wrong-sentences/{sentenceId}")
    public ApiResponse<Void> removeWrongSentence(@CurrentUserId Long userId,
                                                 @PathVariable Long sentenceId) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        practiceService.removeWrongSentence(userId, sentenceId);
        return ApiResponse.success();
    }

    /**
     * 用户举报句子（标记"跟原音对不上"）
     * body: { flag: true } 举报，{ flag: false } 取消举报
     */
    @PostMapping("/sentences/{id}/flag")
    public ApiResponse<Map<String, Object>> flagSentence(@CurrentUserId Long userId,
                                                         @PathVariable Long id,
                                                         @RequestBody Map<String, Object> body) {
        if (userId == null) return ApiResponse.unauthorized("未登录");
        boolean flag = Boolean.parseBoolean(body.getOrDefault("flag", "true").toString());

        if (flag) {
            // 举报：检查是否已经举报过
            Optional<SentenceFlag> existing = sentenceFlagRepository.findBySentenceIdAndUserId(id, userId);
            if (existing.isEmpty()) {
                SentenceFlag sf = new SentenceFlag();
                sf.setSentenceId(id);
                sf.setUserId(userId);
                sentenceFlagRepository.save(sf);
            }
        } else {
            // 取消举报
            sentenceFlagRepository.deleteBySentenceIdAndUserId(id, userId);
        }

        long count = sentenceFlagRepository.countBySentenceId(id);
        return ApiResponse.success(Map.of("flagged", flag, "count", count));
    }

    /** 导出当前用户的练习记录为 CSV */
    @GetMapping("/practice/export")
    public void exportPractice(@CurrentUserId Long userId, HttpServletResponse response) throws IOException {
        if (userId == null) {
            response.setStatus(401);
            return;
        }

        String filename = "pengyouquan-practice-" + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + ".csv";
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=" + filename);

        // CSV 表头
        response.getWriter().write("日期,句子ID,中文,英文,正确率,模式\n");

        // 查询该用户的所有练习记录
        var records = practiceService.getExportRecords(userId);
        for (var row : records) {
            // row: [id, practiced_at, sentence_id, text, correct, correct_count, total_words, mode]
            Object practicedAt = row[1];
            Object sentenceId = row[2];
            Object text = row[3];
            boolean correct = row[4] != null && Boolean.parseBoolean(row[4].toString());
            int correctCount = row[5] != null ? Integer.parseInt(row[5].toString()) : 0;
            int totalWords = row[6] != null ? Integer.parseInt(row[6].toString()) : 0;
            Object mode = row[7];

            String accuracy = totalWords > 0 ? Math.round(correctCount * 100.0 / totalWords) + "%" : "";

            // 句子文本中可能包含中英文，用转义处理
            String sentenceText = text != null ? text.toString() : "";

            String line = String.format("%s,%s,%s,%s,%s,%s\n",
                practicedAt != null ? practicedAt.toString().substring(0, 19).replace("T", " ") : "",
                sentenceId != null ? sentenceId.toString() : "",
                "", // 中文（单独提取后续可优化）
                escapeCsv(sentenceText),
                accuracy,
                mode != null ? escapeCsv(mode.toString()) : ""
            );
            response.getWriter().write(line);
        }

        response.getWriter().flush();
    }

    /** CSV 转义：如果包含逗号/引号/换行则包裹引号 */
    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
