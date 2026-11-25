package com.dapcode.backend_core.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.dapcode.backend_core.config.JwtTokenProvider;
import com.dapcode.backend_core.entity.Users;
import com.dapcode.backend_core.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Luồng tích hợp: Đăng nhập → Tìm kiếm học viên → Khóa → Mở khóa")
class AdminUserManagementFlowIntegrationTest {

    private static final String LOGIN_URL = "/api/v1/auth/admin-login";
    private static final String USERS_URL = "/api/v1/admin/users";
    private static final String ME_URL = "/api/v1/users/me";
    private static final String BAN_REASON = "Gian lận trong bài kiểm tra (integration test)";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Value("${app.admin.username}")
    private String adminUsername;

    @Value("${app.admin.password}")
    private String adminPassword;

    private final String keyword = "itflow" + UUID.randomUUID().toString().substring(0, 8);

    private UUID studentId;
    private String studentToken;
    private String adminToken;
    private UUID adminId;

    @BeforeAll
    void seedStudent() {
        Users student = userRepository.save(Users.builder()
                .username(keyword + "_student")
                .email(keyword + "@student.dapcode.io")
                .role("ROLE_USER")
                .isActive(true)
                .build());
        studentId = student.getId();
        studentToken = jwtTokenProvider.generateToken(student);
    }

    @AfterAll
    void cleanUp() {
        if (studentId != null) {
            userRepository.deleteById(studentId);
        }
    }

    @Test
    @Order(10)
    @DisplayName("1.1 Đăng nhập sai mật khẩu → 401 INVALID_CREDENTIALS")
    void login_wrongPassword_rejected() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody(adminUsername, "sai-mat-khau-" + keyword)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(1010));
    }

    @Test
    @Order(11)
    @DisplayName("1.2 Đăng nhập để trống thông tin → 400")
    void login_blankFields_rejected() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody("", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @Order(12)
    @DisplayName("1.3 Gọi API quản trị khi chưa đăng nhập → 401")
    void adminApi_withoutToken_unauthorized() throws Exception {
        mockMvc.perform(get(USERS_URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(13)
    @DisplayName("1.4 Học viên (ROLE_USER) gọi API quản trị → 403")
    void adminApi_withStudentToken_forbidden() throws Exception {
        mockMvc.perform(get(USERS_URL).header(HttpHeaders.AUTHORIZATION, bearer(studentToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(14)
    @DisplayName("1.5 Đăng nhập admin đúng → 200, nhận JWT và role ROLE_ADMIN")
    void login_validAdmin_returnsToken() throws Exception {
        MvcResult result = mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody(adminUsername, adminPassword)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value(notNullValue()))
                .andExpect(jsonPath("$.data.user.role").value("ROLE_ADMIN"))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        adminToken = JsonPath.read(body, "$.data.token");
        adminId = UUID.fromString(JsonPath.read(body, "$.data.user.id"));
        assertThat(adminToken).isNotBlank();
    }

    @Test
    @Order(20)
    @DisplayName("2.1 Tìm theo username → trả về đúng học viên, đang hoạt động")
    void search_byUsername_findsStudent() throws Exception {
        mockMvc.perform(get(USERS_URL)
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                .param("search", keyword + "_student")
                .param("role", "ROLE_USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(studentId.toString()))
                .andExpect(jsonPath("$.data.content[0].username").value(keyword + "_student"))
                .andExpect(jsonPath("$.data.content[0].active").value(true));
    }

    @Test
    @Order(21)
    @DisplayName("2.2 Tìm theo một phần email, không phân biệt hoa thường")
    void search_byEmailCaseInsensitive_findsStudent() throws Exception {
        mockMvc.perform(get(USERS_URL)
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                .param("search", (keyword + "@student").toUpperCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].email").value(keyword + "@student.dapcode.io"));
    }

    @Test
    @Order(22)
    @DisplayName("2.3 Lọc trạng thái 'đã khóa' khi học viên chưa bị khóa → rỗng")
    void search_lockedFilter_beforeLock_empty() throws Exception {
        mockMvc.perform(get(USERS_URL)
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                .param("search", keyword)
                .param("isActive", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    @Order(23)
    @DisplayName("2.4 Từ khóa không tồn tại → danh sách rỗng")
    void search_unknownKeyword_empty() throws Exception {
        mockMvc.perform(get(USERS_URL)
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                .param("search", "khong-ton-tai-" + UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0))
                .andExpect(jsonPath("$.data.content").isEmpty());
    }

    @Test
    @Order(30)
    @DisplayName("3.0 Trước khi khóa: học viên truy cập /users/me bình thường")
    void beforeLock_studentCanAccess() throws Exception {
        mockMvc.perform(get(ME_URL).header(HttpHeaders.AUTHORIZATION, bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(studentId.toString()));
    }

    @Test
    @Order(31)
    @DisplayName("3.1 Admin không được tự khóa chính mình → 400")
    void lock_self_rejected() throws Exception {
        mockMvc.perform(put(USERS_URL + "/" + adminId + "/status")
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(statusBody(false, "tu khoa")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001));

        assertThat(userRepository.findById(adminId)).get().extracting(Users::isActive).isEqualTo(true);
    }

    @Test
    @Order(32)
    @DisplayName("3.2 Khóa tài khoản không tồn tại → 404")
    void lock_unknownUser_notFound() throws Exception {
        mockMvc.perform(put(USERS_URL + "/" + UUID.randomUUID() + "/status")
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(statusBody(false, BAN_REASON)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(1004));
    }

    @Test
    @Order(321)
    @DisplayName("3.2b Body thiếu trạng thái isActive → 400, học viên không bị khóa")
    void lock_missingStatus_badRequest() throws Exception {
        mockMvc.perform(put(USERS_URL + "/" + studentId + "/status")
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"bannedReason": "thieu trang thai"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001));

        assertThat(userRepository.findById(studentId)).get().extracting(Users::isActive).isEqualTo(true);
    }

    @Test
    @Order(322)
    @DisplayName("3.2c Body JSON sai cú pháp → 400 (không phải 500)")
    void lock_malformedJson_badRequest() throws Exception {
        mockMvc.perform(put(USERS_URL + "/" + studentId + "/status")
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"isActive\": fals"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    @Order(323)
    @DisplayName("3.2d Id không phải UUID → 400 (không phải 500)")
    void lock_invalidId_badRequest() throws Exception {
        mockMvc.perform(put(USERS_URL + "/khong-phai-uuid/status")
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(statusBody(false, BAN_REASON)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    @Order(33)
    @DisplayName("3.3 Khóa học viên kèm lý do (body giống frontend gửi) → active=false, lưu lý do vào DB")
    void lock_student_success() throws Exception {
        mockMvc.perform(put(USERS_URL + "/" + studentId + "/status")
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"active": false, "isActive": false, "bannedReason": "%s"}
                        """.formatted(BAN_REASON)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(studentId.toString()))
                .andExpect(jsonPath("$.data.active").value(false))
                .andExpect(jsonPath("$.data.bannedReason").value(BAN_REASON));

        Users inDb = userRepository.findById(studentId).orElseThrow();
        assertThat(inDb.isActive()).isFalse();
        assertThat(inDb.getBannedReason()).isEqualTo(BAN_REASON);
    }

    @Test
    @Order(34)
    @DisplayName("3.4 Tìm kiếm với bộ lọc 'đã khóa' → thấy học viên vừa khóa")
    void search_lockedFilter_afterLock_findsStudent() throws Exception {
        mockMvc.perform(get(USERS_URL)
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                .param("search", keyword)
                .param("isActive", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(studentId.toString()))
                .andExpect(jsonPath("$.data.content[0].active").value(false));
    }

    @Test
    @Order(35)
    @DisplayName("3.5 Học viên bị khóa dùng token cũ → bị từ chối (401)")
    void afterLock_studentTokenRejected() throws Exception {
        mockMvc.perform(get(ME_URL).header(HttpHeaders.AUTHORIZATION, bearer(studentToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(40)
    @DisplayName("4.1 Mở khóa học viên (body dùng khóa \"active\" như frontend) → active=true, xóa lý do khóa")
    void unlock_student_success() throws Exception {
        mockMvc.perform(put(USERS_URL + "/" + studentId + "/status")
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"active": true}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(true))
                .andExpect(jsonPath("$.data.bannedReason").value(nullValue()));

        Users inDb = userRepository.findById(studentId).orElseThrow();
        assertThat(inDb.isActive()).isTrue();
        assertThat(inDb.getBannedReason()).isNull();
    }

    @Test
    @Order(41)
    @DisplayName("4.2 Tìm kiếm với bộ lọc 'đang hoạt động' → thấy lại học viên")
    void search_activeFilter_afterUnlock_findsStudent() throws Exception {
        mockMvc.perform(get(USERS_URL)
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                .param("search", keyword)
                .param("isActive", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].active").value(true));
    }

    @Test
    @Order(42)
    @DisplayName("4.3 Sau khi mở khóa: học viên truy cập lại được bằng token cũ")
    void afterUnlock_studentCanAccessAgain() throws Exception {
        mockMvc.perform(get(ME_URL).header(HttpHeaders.AUTHORIZATION, bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(studentId.toString()));
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static String loginBody(String usernameOrEmail, String password) {
        return """
                {"usernameOrEmail": "%s", "password": "%s"}
                """.formatted(usernameOrEmail, password);
    }

    private static String statusBody(boolean active, String bannedReason) {
        String reason = bannedReason == null ? "null" : "\"" + bannedReason + "\"";
        return """
                {"isActive": %s, "bannedReason": %s}
                """.formatted(active, reason);
    }
}
