package com.dapcode.backend_core.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.dapcode.backend_core.config.JwtTokenProvider;
import com.dapcode.backend_core.entity.Users;
import com.dapcode.backend_core.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Kiểm thử tích hợp luồng đăng nhập quản trị và xác thực JWT, chạy qua toàn bộ stack
 * (Security filter → AuthController → AuthService → BCrypt → JPA → PostgreSQL):
 *
 * <pre>
 *   A. Đăng nhập          POST /api/v1/auth/admin-login
 *   B. Dùng token         GET  /api/v1/users/me, GET /api/v1/admin/users
 * </pre>
 *
 * Tạo riêng 3 tài khoản test (admin, admin bị khóa, học viên) với mật khẩu băm BCrypt,
 * tên ngẫu nhiên, và xóa sau khi chạy để không đụng dữ liệu có sẵn.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Luồng tích hợp: Đăng nhập quản trị và xác thực JWT")
class AdminLoginFlowIntegrationTest {

    private static final String LOGIN_URL = "/api/v1/auth/admin-login";
    private static final String GOOGLE_URL = "/api/v1/auth/google";
    private static final String ME_URL = "/api/v1/users/me";
    private static final String ADMIN_API = "/api/v1/admin/users";
    private static final String PASSWORD = "Test@Dapcode2026";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Value("${jwt.secret}")
    private String jwtSecret;

    private final String prefix = "itlogin" + UUID.randomUUID().toString().substring(0, 8);

    private Users admin;
    private Users lockedAdmin;
    private Users student;

    @BeforeAll
    void seedAccounts() {
        String hash = passwordEncoder.encode(PASSWORD);
        admin = userRepository.save(Users.builder()
                .username(prefix + "_admin").email(prefix + "_admin@test.dapcode.io")
                .passwordHash(hash).role("ROLE_ADMIN").isActive(true).build());
        lockedAdmin = userRepository.save(Users.builder()
                .username(prefix + "_locked").email(prefix + "_locked@test.dapcode.io")
                .passwordHash(hash).role("ROLE_ADMIN").isActive(false).bannedReason("integration test").build());
        student = userRepository.save(Users.builder()
                .username(prefix + "_student").email(prefix + "_student@test.dapcode.io")
                .passwordHash(hash).role("ROLE_USER").isActive(true).build());
    }

    @AfterAll
    void cleanUp() {
        userRepository.deleteAllById(List.of(admin.getId(), lockedAdmin.getId(), student.getId()));
    }

    // ===================================================================
    // A. ĐĂNG NHẬP
    // ===================================================================

    @Test
    @Order(1)
    @DisplayName("L-01 Đăng nhập bằng username + mật khẩu đúng → 200, JWT chứa đúng id, role ROLE_ADMIN")
    void login_byUsername_success() throws Exception {
        MvcResult result = mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody(admin.getUsername(), PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value(notNullValue()))
                .andExpect(jsonPath("$.data.user.id").value(admin.getId().toString()))
                .andExpect(jsonPath("$.data.user.role").value("ROLE_ADMIN"))
                .andReturn();

        String token = JsonPath.read(result.getResponse().getContentAsString(), "$.data.token");
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getUserIdFromToken(token)).isEqualTo(admin.getId());
    }

    @Test
    @Order(2)
    @DisplayName("L-02 Đăng nhập bằng email → 200")
    void login_byEmail_success() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody(admin.getEmail(), PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.username").value(admin.getUsername()));
    }

    @Test
    @Order(3)
    @DisplayName("L-03 Username có khoảng trắng thừa ở hai đầu → vẫn đăng nhập được")
    void login_usernameWithSpaces_trimmed() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody("   " + admin.getUsername() + "  ", PASSWORD)))
                .andExpect(status().isOk());
    }

    @Test
    @Order(4)
    @DisplayName("L-04 Sai mật khẩu → 401, mã 1010")
    void login_wrongPassword_rejected() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody(admin.getUsername(), PASSWORD + "x")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1010));
    }

    @Test
    @Order(5)
    @DisplayName("L-05 Tài khoản không tồn tại → 401, cùng mã lỗi với sai mật khẩu (không lộ tài khoản)")
    void login_unknownUser_sameErrorAsWrongPassword() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody(prefix + "_khongtontai", PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1010));
    }

    @Test
    @Order(6)
    @DisplayName("L-06 Mật khẩu mặc định admin123 không còn là cửa sau → 401")
    void login_defaultPasswordBackdoor_rejected() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody(admin.getUsername(), "admin123")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1010));
    }

    @Test
    @Order(7)
    @DisplayName("L-07 Học viên (ROLE_USER) đúng mật khẩu đăng nhập cổng quản trị → 401")
    void login_studentAccount_rejected() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody(student.getUsername(), PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1010));
    }

    @Test
    @Order(8)
    @DisplayName("L-08 Admin bị khóa, đúng mật khẩu → 403, mã 1008 (USER_LOCKED)")
    void login_lockedAdmin_correctPassword_locked() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody(lockedAdmin.getUsername(), PASSWORD)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(1008));
    }

    @Test
    @Order(9)
    @DisplayName("L-09 Admin bị khóa, sai mật khẩu → 401 (không được lộ trạng thái khóa khi chưa biết mật khẩu)")
    void login_lockedAdmin_wrongPassword_noStatusLeak() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody(lockedAdmin.getUsername(), "sai-mat-khau")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1010));
    }

    @Test
    @Order(10)
    @DisplayName("L-10 Thiếu mật khẩu → 400 kèm thông báo validation")
    void login_missingPassword_validationError() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"usernameOrEmail": "%s"}
                        """.formatted(admin.getUsername())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001))
                .andExpect(jsonPath("$.message").value("Mật khẩu không được để trống"));
    }

    @Test
    @Order(11)
    @DisplayName("L-11 Body JSON sai cú pháp → 400")
    void login_malformedJson_badRequest() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"usernameOrEmail\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    @Order(12)
    @DisplayName("L-12 Đăng nhập Google với ID token không hợp lệ → 401, mã 1002")
    void googleLogin_invalidIdToken_rejected() throws Exception {
        mockMvc.perform(post(GOOGLE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"idToken": "day-khong-phai-id-token-cua-google"}
                        """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1002));
    }

    // ===================================================================
    // B. XÁC THỰC JWT Ở CÁC REQUEST SAU ĐĂNG NHẬP
    // ===================================================================

    @Test
    @Order(20)
    @DisplayName("L-13 Dùng token vừa đăng nhập: /users/me trả đúng người dùng, gọi được API quản trị")
    void token_fromLogin_grantsAccess() throws Exception {
        String token = loginAndGetToken(admin.getUsername(), PASSWORD);

        mockMvc.perform(get(ME_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(admin.getId().toString()))
                .andExpect(jsonPath("$.data.role").value("ROLE_ADMIN"));

        mockMvc.perform(get(ADMIN_API).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @Order(21)
    @DisplayName("L-14 Sửa payload token của học viên thành ROLE_ADMIN → chữ ký sai, bị chặn 401")
    void token_tamperedPayload_rejected() throws Exception {
        String studentToken = jwtTokenProvider.generateToken(student);
        String[] parts = studentToken.split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        assertThat(payload).contains("ROLE_USER");
        String forgedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.replace("ROLE_USER", "ROLE_ADMIN").getBytes(StandardCharsets.UTF_8));
        String tampered = parts[0] + "." + forgedPayload + "." + parts[2];

        mockMvc.perform(get(ADMIN_API).header(HttpHeaders.AUTHORIZATION, "Bearer " + tampered))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(22)
    @DisplayName("L-15 Token ký bằng khóa bí mật khác (giả mạo) → 401")
    void token_signedWithOtherKey_rejected() throws Exception {
        byte[] otherKey = new byte[64];
        new SecureRandom().nextBytes(otherKey);
        String forged = Jwts.builder()
                .subject(admin.getId().toString())
                .claim("role", "ROLE_ADMIN")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(Keys.hmacShaKeyFor(otherKey))
                .compact();

        mockMvc.perform(get(ADMIN_API).header(HttpHeaders.AUTHORIZATION, "Bearer " + forged))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(23)
    @DisplayName("L-16 Token đã hết hạn → 401")
    void token_expired_rejected() throws Exception {
        long now = System.currentTimeMillis();
        String expired = Jwts.builder()
                .subject(admin.getId().toString())
                .issuedAt(new Date(now - 7_200_000))
                .expiration(new Date(now - 3_600_000))
                .signWith(serverKey())
                .compact();

        mockMvc.perform(get(ME_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + expired))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(24)
    @DisplayName("L-17 Token hợp lệ nhưng người dùng không còn tồn tại → 401")
    void token_ofDeletedUser_rejected() throws Exception {
        String ghost = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(serverKey())
                .compact();

        mockMvc.perform(get(ME_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + ghost))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(25)
    @DisplayName("L-18 Header Authorization sai định dạng (thiếu tiền tố Bearer) → 401")
    void token_wrongHeaderScheme_rejected() throws Exception {
        String token = jwtTokenProvider.generateToken(admin);

        mockMvc.perform(get(ME_URL).header(HttpHeaders.AUTHORIZATION, "Token " + token))
                .andExpect(status().isUnauthorized());
    }

    // ===================================================================
    // Helpers
    // ===================================================================

    private SecretKey serverKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    private String loginAndGetToken(String usernameOrEmail, String password) throws Exception {
        MvcResult result = mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody(usernameOrEmail, password)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.token");
    }

    private static String loginBody(String usernameOrEmail, String password) {
        return """
                {"usernameOrEmail": "%s", "password": "%s"}
                """.formatted(usernameOrEmail, password);
    }
}
