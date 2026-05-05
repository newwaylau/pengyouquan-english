package com.pengyouquan.english;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 练习系统接口单元测试
 * 测试练习记录提交、错题本列表、错题移除
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("练习接口测试")
class PracticeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private String token;
    private static int userCounter = 100;

    @BeforeEach
    void setUp() throws Exception {
        // 每次注册不同邮箱，避免重复注册冲突
        String email = "practice-test-" + (userCounter++) + "@example.com";
        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"Practice1\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String response = registerResult.getResponse().getContentAsString();
        token = JsonPath.read(response, "$.data.token");
    }

    // ── 练习记录提交测试 ──

    @Test
    @DisplayName("提交正确的练习记录应返回200")
    void testLogPractice_Correct() throws Exception {
        mockMvc.perform(post("/api/practice/log")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sentenceId\":1,\"correct\":true,\"correctCount\":5,\"totalWords\":5,\"mode\":\"sentry\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("提交错误的练习记录应返回200")
    void testLogPractice_Wrong() throws Exception {
        mockMvc.perform(post("/api/practice/log")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sentenceId\":2,\"correct\":false,\"correctCount\":2,\"totalWords\":5,\"mode\":\"sentry\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("练习记录支持听写模式")
    void testLogPractice_DictationMode() throws Exception {
        mockMvc.perform(post("/api/practice/log")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sentenceId\":3,\"correct\":true,\"correctCount\":4,\"totalWords\":4,\"mode\":\"dictation\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("练习记录支持中译英模式")
    void testLogPractice_SentenceMode() throws Exception {
        mockMvc.perform(post("/api/practice/log")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sentenceId\":4,\"correct\":true,\"correctCount\":3,\"totalWords\":3,\"mode\":\"sentence\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("未登录提交练习记录应返回401")
    void testLogPractice_Unauthorized() throws Exception {
        mockMvc.perform(post("/api/practice/log")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sentenceId\":1,\"correct\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("未登录"));
    }

    @Test
    @DisplayName("提交练习记录时sentenceId为0应返回200（边缘情况）")
    void testLogPractice_ZeroSentenceId() throws Exception {
        mockMvc.perform(post("/api/practice/log")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sentenceId\":0,\"correct\":false,\"correctCount\":0,\"totalWords\":5,\"mode\":\"sentry\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ── 错题本测试 ──

    @Test
    @DisplayName("空错题本返回空列表")
    void testWrongSentences_Empty() throws Exception {
        mockMvc.perform(get("/api/wrong-sentences")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("新错题自动加入错题本")
    void testWrongSentences_WithWrongPractice() throws Exception {
        // 先提交一个错误的练习记录
        mockMvc.perform(post("/api/practice/log")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sentenceId\":10,\"correct\":false,\"correctCount\":2,\"totalWords\":5,\"mode\":\"sentry\"}"));

        // 检查错题本中是否包含该句子
        MvcResult result = mockMvc.perform(get("/api/wrong-sentences")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andReturn();

        String resultBody = result.getResponse().getContentAsString();
        Integer sentenceId = JsonPath.read(resultBody, "$.data[0].sentenceId");
        assertEquals(10, sentenceId, "错题本应包含sentenceId=10的句子");
    }

    @Test
    @DisplayName("正确练习不会加入错题本")
    void testWrongSentences_CorrectPractice() throws Exception {
        // 提交正确的练习记录
        mockMvc.perform(post("/api/practice/log")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sentenceId\":20,\"correct\":true,\"correctCount\":5,\"totalWords\":5,\"mode\":\"sentry\"}"));

        // 检查错题本仍为空
        mockMvc.perform(get("/api/wrong-sentences")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("重复错误不增加新条目（去重）")
    void testWrongSentences_Deduplication() throws Exception {
        // 两次提交同一句子的错误记录
        String body = "{\"sentenceId\":30,\"correct\":false,\"correctCount\":1,\"totalWords\":3,\"mode\":\"sentry\"}";
        mockMvc.perform(post("/api/practice/log")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body));
        mockMvc.perform(post("/api/practice/log")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body));

        // 错题本应只有1条记录（去重）
        MvcResult result = mockMvc.perform(get("/api/wrong-sentences")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andReturn();

        String resultBody = result.getResponse().getContentAsString();
        Integer errorCount = JsonPath.read(resultBody, "$.data[0].errorCount");
        assertTrue(errorCount >= 2, "重复错误应增加errorCount");
    }

    @Test
    @DisplayName("未登录获取错题本应返回401")
    void testWrongSentences_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/wrong-sentences"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("未登录"));
    }

    // ── 错题移除测试 ──

    @Test
    @DisplayName("删除一条错题应返回200")
    void testRemoveWrongSentence() throws Exception {
        // 先创建一个错题
        mockMvc.perform(post("/api/practice/log")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sentenceId\":40,\"correct\":false,\"correctCount\":0,\"totalWords\":5,\"mode\":\"sentry\"}"));

        // 删除该错题
        mockMvc.perform(delete("/api/wrong-sentences/40")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 确认错题本为空
        mockMvc.perform(get("/api/wrong-sentences")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("未登录删除错题应返回401")
    void testRemoveWrongSentence_Unauthorized() throws Exception {
        mockMvc.perform(delete("/api/wrong-sentences/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("未登录"));
    }

    @Test
    @DisplayName("删除不存在的错题应返回200（幂等）")
    void testRemoveWrongSentence_NonExistent() throws Exception {
        mockMvc.perform(delete("/api/wrong-sentences/99999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ── 错题清空测试 ──

    @Test
    @DisplayName("清空错题本应返回200")
    void testClearWrongSentences() throws Exception {
        // 先创建几条错题
        for (int sid = 50; sid < 55; sid++) {
            mockMvc.perform(post("/api/practice/log")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"sentenceId\":" + sid + ",\"correct\":false,\"correctCount\":0,\"totalWords\":3,\"mode\":\"sentry\"}"));
        }

        // 清空错题本
        mockMvc.perform(delete("/api/wrong-sentences")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 确认错题本为空
        mockMvc.perform(get("/api/wrong-sentences")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("未登录清空错题本应返回401")
    void testClearWrongSentences_Unauthorized() throws Exception {
        mockMvc.perform(delete("/api/wrong-sentences"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("未登录"));
    }
}
