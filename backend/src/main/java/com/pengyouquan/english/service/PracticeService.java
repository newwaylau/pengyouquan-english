package com.pengyouquan.english.service;

import com.pengyouquan.english.model.PracticeLog;
import com.pengyouquan.english.model.Sentence;
import com.pengyouquan.english.model.Show;
import com.pengyouquan.english.model.WrongSentence;
import com.pengyouquan.english.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /** 记录一次练习 */
    @Transactional
    public void logPractice(Long userId, Long sentenceId, boolean correct,
                            int correctCount, int totalWords, String mode) {
        PracticeLog log = new PracticeLog();
        log.setUserId(userId);
        log.setSentenceId(sentenceId);
        log.setCorrect(correct);
        log.setCorrectCount(correctCount);
        log.setTotalWords(totalWords);
        log.setMode(mode);
        practiceLogRepository.save(log);

        // 如果错了，记录到错题本
        if (!correct) {
            WrongSentence ws = wrongSentenceRepository
                    .findByUserIdAndSentenceId(userId, sentenceId)
                    .orElseGet(() -> {
                        WrongSentence w = new WrongSentence();
                        w.setUserId(userId);
                        w.setSentenceId(sentenceId);
                        return w;
                    });
            ws.setErrorCount(ws.getErrorCount() == null ? 1 : ws.getErrorCount() + 1);
            wrongSentenceRepository.save(ws);
        }
    }

    /** 获取错题本（含句子内容） */
    public List<Map<String, Object>> getWrongSentences(Long userId) {
        List<WrongSentence> wrongs = wrongSentenceRepository.findByUserIdOrderByLastPracticedAtDesc(userId);
        List<Map<String, Object>> result = new ArrayList<>();

        for (WrongSentence ws : wrongs) {
            Optional<Sentence> optS = sentenceRepository.findById(ws.getSentenceId());
            if (optS.isEmpty()) continue;
            Sentence s = optS.get();
            String showName = showRepository.findById(s.getShowId())
                    .map(Show::getName).orElse("");

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("sentenceId", s.getId());
            item.put("text", s.getText());
            item.put("showName", showName);
            item.put("errorCount", ws.getErrorCount());
            item.put("lastPracticedAt", ws.getLastPracticedAt() != null ?
                    ws.getLastPracticedAt().toString() : "");
            result.add(item);
        }
        return result;
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
}
