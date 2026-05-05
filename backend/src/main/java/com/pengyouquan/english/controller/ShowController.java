package com.pengyouquan.english.controller;

import com.pengyouquan.english.dto.*;
import com.pengyouquan.english.model.Show;
import com.pengyouquan.english.model.Sentence;
import com.pengyouquan.english.repository.ShowRepository;
import com.pengyouquan.english.repository.SentenceRepository;

import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.PageRequest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 剧集系统接口
 * 剧集列表 / 随机句子 / 搜索 / 浏览
 */
@RestController
@RequestMapping("/api")
public class ShowController {

    private final ShowRepository showRepository;
    private final SentenceRepository sentenceRepository;

    public ShowController(ShowRepository showRepository,
                          SentenceRepository sentenceRepository) {
        this.showRepository = showRepository;
        this.sentenceRepository = sentenceRepository;
    }

    /** 获取所有剧集（含句子数），缓存 10 分钟 */
    @GetMapping("/shows")
    public ApiResponse<List<ShowDto>> listShows() {
        List<Show> shows = showRepository.findAllByOrderByImportedAtDesc();
        List<ShowDto> dtos = shows.stream()
                .map(s -> ShowDto.from(s, sentenceRepository.countByShowId(s.getId())))
                .toList();
        return ApiResponse.success(dtos);
    }

    /** 随机抽取句子（支持剧集筛选和排除已练） */
    @GetMapping("/random")
    public ApiResponse<List<SentenceDto>> randomSentences(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) Long showId,
            @RequestParam(required = false) String showIds,
            @RequestParam(required = false) String exclude) {

        limit = Math.min(limit, 50);
        List<Long> excludeIds = parseIdList(exclude);
        List<Sentence> results;

        if (excludeIds != null && !excludeIds.isEmpty()) {
            results = sentenceRepository.findRandomExcluding(excludeIds, limit);
        } else if (showIds != null && !showIds.isEmpty()) {
            List<Long> ids = parseIdList(showIds);
            results = sentenceRepository.findRandomByShowIds(ids, limit);
        } else if (showId != null) {
            results = sentenceRepository.findRandomByShowId(showId, limit);
        } else {
            results = sentenceRepository.findRandom(limit);
        }

        List<SentenceDto> dtos = enrichWithShowName(results);
        return ApiResponse.success(dtos);
    }

    /** 搜索句子（中英文） */
    @GetMapping("/search")
    public ApiResponse<List<SentenceDto>> search(@RequestParam(defaultValue = "") String q) {
        if (q.length() < 2) {
            return ApiResponse.success(List.of());
        }
        List<Sentence> results = sentenceRepository.searchByText(q);
        List<SentenceDto> dtos = enrichWithShowName(results);
        return ApiResponse.success(dtos);
    }

    /** 按ID获取单条句子 */
    @GetMapping("/sentence/{id}")
    public ApiResponse<SentenceDto> getSentence(@PathVariable Long id) {
        Sentence s = sentenceRepository.findById(id)
                .orElse(null);
        if (s == null) return ApiResponse.notFound("句子不存在");
        String showName = showRepository.findById(s.getShowId())
                .map(Show::getName).orElse("");
        return ApiResponse.success(SentenceDto.from(s, showName));
    }

    /** 浏览指定剧集的句子列表（分页） */
    @GetMapping("/show/{showId}/sentences")
    public ApiResponse<Map<String, Object>> browseSentences(
            @PathVariable Long showId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        size = Math.min(size, 100);
        long total = sentenceRepository.countByShowId(showId);
        List<Sentence> list = sentenceRepository.findByShowIdOrderById(showId, PageRequest.of(page, size));
        List<SentenceDto> dtos = enrichWithShowName(list);
        String showName = showRepository.findById(showId).map(Show::getName).orElse("");
        return ApiResponse.success(Map.of(
                "sentences", dtos,
                "total", total,
                "showName", showName,
                "page", page,
                "totalPages", (int) Math.ceil((double) total / size)
        ));
    }

    /** 统计数据 */
    @GetMapping("/shows-stats")
    public ApiResponse<Map<String, Object>> stats(
            @RequestParam(required = false) Long showId,
            @RequestParam(required = false) String showIds) {

        long totalSentences;
        if (showId != null) {
            totalSentences = sentenceRepository.countByShowId(showId);
        } else if (showIds != null && !showIds.isEmpty()) {
            totalSentences = sentenceRepository.countByShowIds(parseIdList(showIds));
        } else {
            totalSentences = sentenceRepository.count();
        }

        return ApiResponse.success(Map.of("totalSentences", totalSentences));
    }

    // ── 工具方法 ──

    /** 把句子列表附加上剧集名 */
    private List<SentenceDto> enrichWithShowName(List<Sentence> sentences) {
        // 收集所有不重复的 showId
        Set<Long> showIds = sentences.stream().map(Sentence::getShowId).collect(Collectors.toSet());
        Map<Long, String> showNames = showRepository.findAllById(showIds).stream()
                .collect(Collectors.toMap(Show::getId, Show::getName));

        return sentences.stream()
                .map(s -> SentenceDto.from(s, showNames.getOrDefault(s.getShowId(), "")))
                .toList();
    }

    /** 解析逗号分隔的ID列表 */
    private List<Long> parseIdList(String str) {
        if (str == null || str.isBlank()) return null;
        return Arrays.stream(str.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .toList();
    }
}
