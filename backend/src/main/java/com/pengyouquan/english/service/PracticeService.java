package com.pengyouquan.english.service;

import com.pengyouquan.english.model.PracticeLog;
import com.pengyouquan.english.model.Sentence;
import com.pengyouquan.english.model.Show;
import com.pengyouquan.english.model.WrongSentence;
import com.pengyouquan.english.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 练习服务
 * 记录练习/错题管理
 */
@Service
public class PracticeService {

    private final PracticeLogRepository practiceLogRepository;
    private final WrongSentenceRepository wrongSentenceRepository;
    private final SentenceRepository sentenceRepository;
    private final ShowRepository showRepository;

    public PracticeService(PracticeLogRepository practiceLogRepository,
                           WrongSentenceRepository wrongSentenceRepository,
                           SentenceRepository sentenceRepository,
                           ShowRepository showRepository) {
        this.practiceLogRepository = practiceLogRepository;
        this.wrongSentenceRepository = wrongSentenceRepository;
        this.sentenceRepository = sentenceRepository;
        this.showRepository = showRepository;
    }

    /**
     * 记录一次练习
     * 增强：响应中增加错题检查信息，用于前端"再练一遍"判断
     * @return 包含 inWrongBook / errorCount / newErrorCount 的 Map
     */
    @Transactional
    public Map<String, Object> logPractice(Long userId, Long sentenceId, boolean correct,
                                           int correctCount, int totalWords, String mode) {
        PracticeLog log = new PracticeLog();
        log.setUserId(userId);
        log.setSentenceId(sentenceId);
        log.setCorrect(correct);
        log.setCorrectCount(correctCount);
        log.setTotalWords(totalWords);
        log.setMode(mode);
        practiceLogRepository.save(log);

        // 记录错题本检查结果
        boolean inWrongBook = false;
        int errorCount = 0;
        int newErrorCount = 0;

        // 如果错了，记录/更新到错题本
        if (!correct) {
            WrongSentence ws = wrongSentenceRepository
                    .findByUserIdAndSentenceId(userId, sentenceId)
                    .orElseGet(() -> {
                        // 新实体：从句子表提取冗余字段
                        WrongSentence w = new WrongSentence();
                        w.setUserId(userId);
                        w.setSentenceId(sentenceId);
                        w.setErrorCount(1);
                        // 填充冗余字段
                        fillRedundantFields(w);
                        return w;
                    });
            if (ws.getId() == null) {
                // 新创建的实体：errorCount 已在构造函数中设为 1
                ws.setErrorCount(ws.getErrorCount() != null ? ws.getErrorCount() + 1 : 1);
                newErrorCount = ws.getErrorCount();
            } else {
                // 已有记录：增加错误次数
                ws.setErrorCount(ws.getErrorCount() + 1);
                newErrorCount = ws.getErrorCount();
            }
            ws.setLastPracticedAt(LocalDateTime.now());
            // 如果之前标记已掌握，出错后自动取消掌握标记
            if (Boolean.TRUE.equals(ws.getIsMastered())) {
                ws.setIsMastered(false);
            }
            wrongSentenceRepository.save(ws);
            inWrongBook = true;
            errorCount = ws.getErrorCount();
        } else {
            // 答对了，但检查这个句子是否在错题本中
            Optional<WrongSentence> optWs = wrongSentenceRepository
                    .findByUserIdAndSentenceId(userId, sentenceId);
            if (optWs.isPresent()) {
                WrongSentence ws = optWs.get();
                inWrongBook = true;
                errorCount = ws.getErrorCount();

                // 减少错误计数，如果减到 0 或以下则标记已掌握
                int newErr = ws.getErrorCount() - 1;
                if (newErr <= 0) {
                    ws.setIsMastered(true);
                    ws.setErrorCount(0);
                } else {
                    ws.setErrorCount(newErr);
                }
                ws.setLastPracticedAt(LocalDateTime.now());
                wrongSentenceRepository.save(ws);
                newErrorCount = Math.max(0, newErr);
            }
        }

        // 封装返回数据
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("inWrongBook", inWrongBook);
        result.put("errorCount", errorCount);
        result.put("newErrorCount", newErrorCount);
        return result;
    }

    /**
     * 填充冗余字段（剧集名）
     */
    private void fillRedundantFields(WrongSentence ws) {
        Optional<Sentence> optS = sentenceRepository.findById(ws.getSentenceId());
        optS.ifPresent(s -> {
            Optional<Show> optShow = showRepository.findById(s.getShowId());
            optShow.ifPresent(show -> ws.setShowName(show.getName() != null ? show.getName() : ""));
        });
    }

    /**
     * 获取错题列表（增强版：支持排序和筛选）
     */
    public List<Map<String, Object>> getWrongSentences(Long userId, String showName,
                                                       String sortBy, String sortDir,
                                                       boolean includeMastered) {
        List<WrongSentence> wrongs;

        // 根据筛选条件选择不同查询
        if (showName != null && !showName.isEmpty()) {
            if (includeMastered) {
                // 按剧集名筛选（含已掌握）
                wrongs = wrongSentenceRepository
                        .findByUserIdAndShowNameOrderByErrorCountDesc(userId, showName);
            } else {
                // 按剧集名筛选（仅未掌握）
                wrongs = wrongSentenceRepository
                        .findByUserIdAndShowNameAndIsMasteredFalseOrderByErrorCountDesc(userId, showName);
            }
        } else {
            if (includeMastered) {
                // 全部（含已掌握），按错误次数降序
                wrongs = wrongSentenceRepository
                        .findByUserIdOrderByErrorCountDesc(userId);
            } else {
                // 仅未掌握
                wrongs = wrongSentenceRepository
                        .findByUserIdAndIsMasteredFalseOrderByErrorCountDesc(userId);
            }
        }

        // 如果指定了排序方向，在内存中调整
        if ("asc".equalsIgnoreCase(sortDir) && wrongs.size() > 1) {
            boolean sortByError = "errorCount".equalsIgnoreCase(sortBy);
            wrongs.sort((a, b) -> {
                int compare = sortByError
                    ? Integer.compare(a.getErrorCount() != null ? a.getErrorCount() : 0,
                                       b.getErrorCount() != null ? b.getErrorCount() : 0)
                    : a.getLastPracticedAt() != null && b.getLastPracticedAt() != null
                        ? a.getLastPracticedAt().compareTo(b.getLastPracticedAt())
                        : 0;
                return compare; // asc
            });
        } else if ("desc".equalsIgnoreCase(sortDir) && "lastPracticedAt".equalsIgnoreCase(sortBy)) {
            // 特殊：按最后练习时间降序
            wrongs.sort((a, b) -> {
                if (a.getLastPracticedAt() != null && b.getLastPracticedAt() != null) {
                    return b.getLastPracticedAt().compareTo(a.getLastPracticedAt());
                }
                return 0;
            });
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (WrongSentence ws : wrongs) {
            // 读取句子原文
            Optional<Sentence> optS = sentenceRepository.findById(ws.getSentenceId());
            String text = optS.map(Sentence::getText).orElse("");

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("sentenceId", ws.getSentenceId());
            item.put("text", text);
            item.put("showName", ws.getShowName() != null ? ws.getShowName() : "");
            item.put("errorCount", ws.getErrorCount() != null ? ws.getErrorCount() : 0);
            item.put("lastPracticedAt", ws.getLastPracticedAt() != null ?
                    ws.getLastPracticedAt().toString() : "");
            item.put("isMastered", ws.getIsMastered() != null && ws.getIsMastered());
            result.add(item);
        }
        return result;
    }

    /**
     * 获取错题中所有剧集名（用于前端筛选下拉）
     */
    public List<String> getDistinctShowNames(Long userId) {
        return wrongSentenceRepository.findDistinctShowNamesByUserId(userId);
    }

    /**
     * 标记已掌握
     */
    @Transactional
    public void markMastered(Long userId, Long sentenceId) {
        wrongSentenceRepository.findByUserIdAndSentenceId(userId, sentenceId)
                .ifPresent(ws -> {
                    ws.setIsMastered(true);
                    ws.setLastPracticedAt(LocalDateTime.now());
                    wrongSentenceRepository.save(ws);
                });
    }

    /**
     * 撤销已掌握
     */
    @Transactional
    public void unmarkMastered(Long userId, Long sentenceId) {
        wrongSentenceRepository.findByUserIdAndSentenceId(userId, sentenceId)
                .ifPresent(ws -> {
                    ws.setIsMastered(false);
                    ws.setLastPracticedAt(LocalDateTime.now());
                    wrongSentenceRepository.save(ws);
                });
    }

    /**
     * 获取批量练习的错题句子（按优先级排序，仅未掌握）
     */
    public Map<String, Object> getWrongPractice(Long userId, int limit) {
        List<WrongSentence> wrongs = wrongSentenceRepository
                .findByUserIdAndIsMasteredFalseOrderByErrorCountDesc(userId);

        // 限制返回数量
        if (wrongs.size() > limit) {
            wrongs = wrongs.subList(0, limit);
        }

        List<Map<String, Object>> sentences = new ArrayList<>();
        for (WrongSentence ws : wrongs) {
            Optional<Sentence> optS = sentenceRepository.findById(ws.getSentenceId());
            if (optS.isEmpty()) continue;
            Sentence s = optS.get();

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("sentenceId", ws.getSentenceId());
            item.put("text", s.getText());
            item.put("showName", ws.getShowName() != null ? ws.getShowName() : "");
            item.put("errorCount", ws.getErrorCount() != null ? ws.getErrorCount() : 0);
            item.put("lastPracticedAt", ws.getLastPracticedAt() != null ?
                    ws.getLastPracticedAt().toString() : "");
            sentences.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", sentences.size());
        result.put("sentences", sentences);
        return result;
    }

    /** 获取错题本（旧版，保持兼容） */
    public List<Map<String, Object>> getWrongSentences(Long userId) {
        return getWrongSentences(userId, null, null, null, true);
    }

    /** 清空错题本 */
    @Transactional
    public void clearWrongSentences(Long userId) {
        wrongSentenceRepository.deleteByUserId(userId);
    }

    /** 删除一条错题 */
    @Transactional
    public void removeWrongSentence(Long userId, Long sentenceId) {
        wrongSentenceRepository.deleteByUserIdAndSentenceId(userId, sentenceId);
    }

    /** 获取练习记录导出数据（含句子文本） */
    public List<Object[]> getExportRecords(Long userId) {
        return practiceLogRepository.findPracticeRecordsForExport(userId);
    }
}
