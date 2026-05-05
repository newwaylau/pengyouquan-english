package com.pengyouquan.english;

import com.jayway.jsonpath.JsonPath;
import com.pengyouquan.english.model.Show;
import com.pengyouquan.english.model.Sentence;
import com.pengyouquan.english.repository.ShowRepository;
import com.pengyouquan.english.repository.SentenceRepository;
import com.pengyouquan.english.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 后端 E2E 集成测试
 * <p>
 * 测试完整流程：注册 → 登录 → 获取剧集列表 → 获取随机句子 → 提交练习记录 → 获取错题本
 * 使用 TestRestTemplate 直接发送 HTTP 请求，与真实启动的应用交互。
 * <p>
 * 注意：需要 H2 内存数据库，测试前会插入初始数据（如剧集和句子）。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.MethodName.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("E2E 集成测试")
class E2eIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ShowRepository showRepository;

    @Autowired
    private SentenceRepository sentenceRepository;

    @Autowired
    private UserRepository userRepository;

    /** 测试用邮箱（唯一，避免冲突） */
    private static final String TEST_EMAIL = "e2e-test@" + System.currentTimeMillis() + ".com";
    private static final String TEST_PASSWORD = "E2eTest123";

    /** 登录后获取的 JWT Token */
    private static String jwtToken;

    /** 测试剧集 ID */
    private static Long showId;

    /** 测试句子 ID */
    private static Long sentenceId;

    /** 基础 URL */
    private String baseUrl() {
        return "http://localhost:" + port;
    }

    // ============================================================
    // 测试前置：插入测试数据（剧集 + 句子）
    // ============================================================

    @BeforeAll
    void setUp(@Autowired ShowRepository showRepo,
               @Autowired SentenceRepository sentenceRepo,
               @Autowired UserRepository userRepo) {
        // 清理旧数据（防止上一次运行的残留）
        userRepo.deleteAll();
        sentenceRepo.deleteAll();
        showRepo.deleteAll();

        // 创建测试剧集
        Show show = new Show();
        show.setName("测试剧集 - Friends");
        showRepo.save(show);
        showId = show.getId();

        // 创建测试句子
        Sentence sentence = new Sentence();
        sentence.setShowId(showId);
        sentence.setText("How you doin'?");
        sentence.setStartTime(1.0);
        sentence.setEndTime(3.5);
        sentenceRepo.save(sentence);
        sentenceId = sentence.getId();

        // 创建第二条句子（用于练习记录测试）
        Sentence sentence2 = new Sentence();
        sentence2.setShowId(showId);
        sentence2.setText("We were on a break!");
        sentence2.setStartTime(5.0);
        sentence2.setEndTime(7.0);
        sentenceRepo.save(sentence2);
    }

    // ============================================================
    // A) 注册测试用户
    // ============================================================

    @Test
    @DisplayName("A - 注册测试用户")
    void stepA_Register() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"email\":\"" + TEST_EMAIL + "\",\"password\":\"" + TEST_PASSWORD + "\"}";
        HttpEntity<String> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl() + "/api/auth/register", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Integer code = JsonPath.read(response.getBody(), "$.code");
        assertThat(code).isEqualTo(200);

        String token = JsonPath.read(response.getBody(), "$.data.token");
        assertThat(token).isNotNull().isNotEmpty();

        String email = JsonPath.read(response.getBody(), "$.data.email");
        assertThat(email).isEqualTo(TEST_EMAIL);

        jwtToken = token;
    }

    // ============================================================
    // B) 登录测试用户
    // ============================================================

    @Test
    @DisplayName("B - 登录获取 Token")
    void stepB_Login() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"email\":\"" + TEST_EMAIL + "\",\"password\":\"" + TEST_PASSWORD + "\"}";
        HttpEntity<String> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl() + "/api/auth/login", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Integer code = JsonPath.read(response.getBody(), "$.code");
        assertThat(code).isEqualTo(200);

        String token = JsonPath.read(response.getBody(), "$.data.token");
        assertThat(token).isNotNull().isNotEmpty();

        // 更新 Token（登录可能会签发新 Token）
        jwtToken = token;
    }

    // ============================================================
    // C) 获取剧集列表（需 Token）
    // ============================================================

    @Test
    @DisplayName("C - 获取剧集列表")
    void stepC_ListShows() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl() + "/api/shows", HttpMethod.GET, request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Integer code = JsonPath.read(response.getBody(), "$.code");
        assertThat(code).isEqualTo(200);

        // 应包含我们预注入的剧集
        List<String> names = JsonPath.read(response.getBody(), "$.data[*].name");
        assertThat(names).contains("测试剧集 - Friends");
    }

    // ============================================================
    // D) 获取随机句子
    // ============================================================

    @Test
    @DisplayName("D - 获取随机句子")
    void stepD_RandomSentence() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl() + "/api/random?limit=5", HttpMethod.GET, request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Integer code = JsonPath.read(response.getBody(), "$.code");
        assertThat(code).isEqualTo(200);

        // 应有句子返回
        List<Object> sentences = JsonPath.read(response.getBody(), "$.data");
        assertThat(sentences).isNotEmpty();
    }

    // ============================================================
    // E) 提交练习记录
    // ============================================================

    @Test
    @DisplayName("E - 提交练习记录（预期绿色-正确；红色-错误入错题本）")
    void stepE_SubmitPracticeLog() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(jwtToken);

        // 提交一条正确练习记录
        String correctBody = "{\"sentenceId\":" + sentenceId
                + ",\"correct\":true,\"correctCount\":3,\"totalWords\":3,\"mode\":\"sentry\"}";
        HttpEntity<String> correctRequest = new HttpEntity<>(correctBody, headers);

        ResponseEntity<String> correctResponse = restTemplate.postForEntity(
                baseUrl() + "/api/practice/log", correctRequest, String.class);

        assertThat(correctResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Integer code = JsonPath.read(correctResponse.getBody(), "$.code");
        assertThat(code).isEqualTo(200);

        // 提交一条错误练习记录（会加入错题本）
        String wrongBody = "{\"sentenceId\":" + sentenceId
                + ",\"correct\":false,\"correctCount\":1,\"totalWords\":3,\"mode\":\"sentry\"}";
        HttpEntity<String> wrongRequest = new HttpEntity<>(wrongBody, headers);

        ResponseEntity<String> wrongResponse = restTemplate.postForEntity(
                baseUrl() + "/api/practice/log", wrongRequest, String.class);

        assertThat(wrongResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Integer wrongCode = JsonPath.read(wrongResponse.getBody(), "$.code");
        assertThat(wrongCode).isEqualTo(200);
    }

    // ============================================================
    // F) 获取错题本（需 Token）
    // ============================================================

    @Test
    @DisplayName("F - 获取错题本")
    void stepF_GetWrongSentences() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl() + "/api/wrong-sentences", HttpMethod.GET, request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Integer code = JsonPath.read(response.getBody(), "$.code");
        assertThat(code).isEqualTo(200);

        // 错题本应包含之前提交的错误练习
        List<Object> wrongs = JsonPath.read(response.getBody(), "$.data");
        assertThat(wrongs).isNotEmpty();

        // 验证错误次数
        List<Integer> errorCounts = JsonPath.read(response.getBody(), "$.data[*].errorCount");
        assertThat(errorCounts).anyMatch(count -> count >= 1);
    }
}
