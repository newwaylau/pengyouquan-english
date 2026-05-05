package com.pengyouquan.english;

import com.pengyouquan.english.model.WrongSentence;
import com.pengyouquan.english.repository.WrongSentenceRepository;
import com.pengyouquan.english.service.PracticeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 练习服务直接测试（绕过MockMvc）
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("练习服务直接测试")
class PracticeServiceTest {

    @Autowired
    private PracticeService practiceService;

    @Autowired
    private WrongSentenceRepository wrongSentenceRepository;

    @Test
    @DisplayName("记录错误练习后查询错题本应返回1条")
    void testLogPracticeCreatesWrongSentence() {
        // 直接调用服务层
        practiceService.logPractice(1L, 100L, false, 3, 5, "sentry");

        // 查询错题本
        List<WrongSentence> wrongs = wrongSentenceRepository.findByUserIdOrderByLastPracticedAtDesc(1L);
        assertEquals(1, wrongs.size(), "错题本应有1条记录");
        assertEquals(100L, wrongs.get(0).getSentenceId(), "sentenceId应为100");
        assertEquals(1, wrongs.get(0).getErrorCount(), "errorCount应为1");

        // 再次记录同一句的错误
        practiceService.logPractice(1L, 100L, false, 2, 5, "sentry");

        Optional<WrongSentence> ws = wrongSentenceRepository.findByUserIdAndSentenceId(1L, 100L);
        assertTrue(ws.isPresent(), "错题应存在");
        assertEquals(2, ws.get().getErrorCount(), "errorCount应增加到2");
    }

    @Test
    @DisplayName("记录正确练习不应创建错题")
    void testLogPracticeCorrectDoesNotCreateWrongSentence() {
        practiceService.logPractice(1L, 200L, true, 5, 5, "sentry");
        List<WrongSentence> wrongs = wrongSentenceRepository.findByUserIdOrderByLastPracticedAtDesc(1L);
        assertEquals(0, wrongs.size(), "正确练习不应创建错题");
    }

    @Test
    @DisplayName("删除错题后查询应返回空")
    void testRemoveWrongSentence() {
        Long uid = 1001L;
        practiceService.logPractice(uid, 300L, false, 1, 3, "sentry");
        practiceService.removeWrongSentence(uid, 300L);
        List<WrongSentence> wrongs = wrongSentenceRepository.findByUserIdOrderByLastPracticedAtDesc(uid);
        assertEquals(0, wrongs.size(), "删除后错题本应为空");
    }
}
