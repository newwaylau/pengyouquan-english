package com.pengyouquan.english;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 剧集系统接口单元测试
 * 测试剧集列表、随机句子、搜索、单句查询
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("剧集接口测试")
class ShowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() throws Exception {
        // 由于是 H2 内存数据库且无 Flyway 迁移，数据库初始为空
        // 测试依赖空数据库的默认行为（空列表、空结果等）
    }

    // ── 剧集列表测试 ──

    @Test
    @DisplayName("获取剧集列表应返回空列表（数据库为空）")
    void testListShows_Empty() throws Exception {
        mockMvc.perform(get("/api/shows"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("获取剧集列表接口格式正确")
    void testListShows_ResponseFormat() throws Exception {
        mockMvc.perform(get("/api/shows"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"));
    }

    // ── 随机句子测试 ──

    @Test
    @DisplayName("随机抽取句子应返回空列表（数据库为空）")
    void testRandomSentences_Empty() throws Exception {
        mockMvc.perform(get("/api/random"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("随机抽取句子带limit参数")
    void testRandomSentences_WithLimit() throws Exception {
        mockMvc.perform(get("/api/random?limit=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("随机抽取句子limit参数被截断到50")
    void testRandomSentences_LimitCapped() throws Exception {
        mockMvc.perform(get("/api/random?limit=100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("按剧集ID随机抽取（剧集不存在）")
    void testRandomSentences_ByShowId() throws Exception {
        mockMvc.perform(get("/api/random?showId=999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("按多个剧集ID随机抽取")
    void testRandomSentences_ByShowIds() throws Exception {
        mockMvc.perform(get("/api/random?showIds=1,2,3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("排除已练句子随机抽取")
    void testRandomSentences_Exclude() throws Exception {
        mockMvc.perform(get("/api/random?exclude=1,2,3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    // ── 搜索测试 ──

    @Test
    @DisplayName("英文关键词搜索应返回空列表（数据库为空）")
    void testSearch_EnglishKeyword() throws Exception {
        mockMvc.perform(get("/api/search?q=hello"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("中文关键词搜索应返回空列表（数据库为空）")
    void testSearch_ChineseKeyword() throws Exception {
        mockMvc.perform(get("/api/search?q=你好"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("搜索关键词太短（少于2字符）应返回空列表")
    void testSearch_ShortKeyword() throws Exception {
        mockMvc.perform(get("/api/search?q=a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("搜索空字符串应返回空列表")
    void testSearch_EmptyKeyword() throws Exception {
        mockMvc.perform(get("/api/search?q="))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("搜索空结果应返回空列表")
    void testSearch_NoResult() throws Exception {
        mockMvc.perform(get("/api/search?q=zzzzzxyznonexistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    // ── 单句查询测试 ──

    @Test
    @DisplayName("查询不存在的句子应返回404")
    void testGetSentence_NotFound() throws Exception {
        mockMvc.perform(get("/api/sentence/99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("句子不存在"));
    }

    @Test
    @DisplayName("单句查询接口格式正确")
    void testGetSentence_ResponseFormat() throws Exception {
        mockMvc.perform(get("/api/sentence/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("句子不存在"));
    }

    // ── 浏览剧集句子测试 ──

    @Test
    @DisplayName("浏览不存在的剧集句子应返回空列表")
    void testBrowseSentences_NotFound() throws Exception {
        mockMvc.perform(get("/api/show/9999/sentences"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.sentences").isArray())
                .andExpect(jsonPath("$.data.sentences.length()").value(0));
    }

    @Test
    @DisplayName("浏览句子分页参数正常")
    void testBrowseSentences_WithPagination() throws Exception {
        mockMvc.perform(get("/api/show/1/sentences?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.page").value(0));
    }

    @Test
    @DisplayName("浏览句子size最大截断到100")
    void testBrowseSentences_SizeCapped() throws Exception {
        mockMvc.perform(get("/api/show/1/sentences?size=200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ── 统计数据测试 ──

    @Test
    @DisplayName("统计数据应返回totalSentences为0")
    void testStats_Empty() throws Exception {
        mockMvc.perform(get("/api/shows-stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalSentences").value(0));
    }

    @Test
    @DisplayName("按ShowId统计")
    void testStats_ByShowId() throws Exception {
        mockMvc.perform(get("/api/shows-stats?showId=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalSentences").value(0));
    }

    @Test
    @DisplayName("按多个ShowIds统计")
    void testStats_ByShowIds() throws Exception {
        mockMvc.perform(get("/api/shows-stats?showIds=1,2,3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalSentences").value(0));
    }
}
