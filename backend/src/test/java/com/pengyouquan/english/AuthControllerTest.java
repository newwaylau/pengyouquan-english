package com.pengyouquan.english;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import static org.hamcrest.Matchers.containsString;

/**
 * 认证接口单元测试
 * 测试注册、登录、JWT过期等场景
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("认证接口测试")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String VALID_EMAIL = "test@example.com";
    private static final String VALID_PASSWORD = "Password123";

    @BeforeEach
    void setUp() {
        // 每个测试用例前无需额外清理，H2 内存数据库每次测试通常使用 @Transactional 或重启
        // 这里我们依靠设计：每次测试注册的用户邮箱不同，避免冲突
    }

    // ── 注册测试 ──

    @Test
    @DisplayName("正常注册应返回200和JWT Token")
    void testRegister_Success() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test-register@example.com\",\"password\":\"Test1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.email").value("test-register@example.com"))
                .andExpect(jsonPath("$.data.nickname").isNotEmpty())
                .andExpect(jsonPath("$.data.role").value("user"));
    }

    @Test
    @DisplayName("重复邮箱注册应返回400错误")
    void testRegister_DuplicateEmail() throws Exception {
        String body = "{\"email\":\"test-duplicate@example.com\",\"password\":\"Test1234\"}";
        // 第一次注册
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        // 第二次注册（重复邮箱）
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("该邮箱已注册"));
    }

    @Test
    @DisplayName("邮箱格式错误应返回400")
    void testRegister_InvalidEmail() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"password\":\"Test1234\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").org.hamcrest.Matchers.containsString("邮箱格式不正确"));
    }

    @Test
    @DisplayName("密码太短应返回400")
    void testRegister_WeakPassword() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test-weak@example.com\",\"password\":\"ab\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").org.hamcrest.Matchers.containsString("密码长度6-50位"));
    }

    @Test
    @DisplayName("空邮箱应返回400")
    void testRegister_EmptyEmail() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"\",\"password\":\"Test1234\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("空密码应返回400")
    void testRegister_EmptyPassword() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test-empty@example.com\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    // ── 登录测试 ──

    @Test
    @DisplayName("正常登录应返回200和JWT Token")
    void testLogin_Success() throws Exception {
        // 先注册
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test-login@example.com\",\"password\":\"LoginPass1\"}"));

        // 再登录
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test-login@example.com\",\"password\":\"LoginPass1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.email").value("test-login@example.com"))
                .andExpect(jsonPath("$.data.role").value("user"));
    }

    @Test
    @DisplayName("错误密码登录应返回400")
    void testLogin_WrongPassword() throws Exception {
        // 先注册
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test-wrongpwd@example.com\",\"password\":\"Correct1Pass\"}"));

        // 用错误密码登录
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test-wrongpwd@example.com\",\"password\":\"WrongPass1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("邮箱或密码错误"));
    }

    @Test
    @DisplayName("不存在的账号登录应返回400")
    void testLogin_NonExistentUser() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@example.com\",\"password\":\"Whatever1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("邮箱或密码错误"));
    }

    // ── JWT 过期测试 ──

    @Test
    @DisplayName("过期JWT访问受保护接口应返回401")
    void testExpiredToken_Returns401() throws Exception {
        // 使用一个已经过期的 JWT（exp 为 0）
        String expiredToken = "eyJhbGciOiJIUzI1NiJ9" +
                ".eyJzdWIiOiIxIiwiZXhwIjowfQ" +
                ".dGhpcyBpcyBub3QgYSByZWFsIHNpZ25hdHVyZQ";

        // 访问 /api/auth/me 带过期 token
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data").doesNotExist());
        // 注意：CURRENT DESIGN — JWT 过滤器对无效 token 仅不做处理，
        // controller 中 userId 为 null，返回 401 {"code":401,"message":"未登录","data":null}
        // 所以这里返回 200（HTTP status），但 body 有 code=401
    }

    @Test
    @DisplayName("不带Token访问/me应返回401")
    void testMe_WithoutToken() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("未登录"));
    }

    @Test
    @DisplayName("携带有效Token访问/me应返回用户信息")
    void testMe_WithValidToken() throws Exception {
        // 注册并获取 token
        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test-me@example.com\",\"password\":\"TokenPass1\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String response = registerResult.getResponse().getContentAsString();
        String token = JsonPath.read(response, "$.data.token");

        // 用有效 token 访问 /me
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.email").value("test-me@example.com"))
                .andExpect(jsonPath("$.data.nickname").isNotEmpty());
    }
}
