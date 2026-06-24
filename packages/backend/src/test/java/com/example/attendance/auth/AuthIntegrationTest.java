package com.example.attendance.auth;

import com.example.attendance.department.entity.Department;
import com.example.attendance.employee.entity.Employee;
import com.example.attendance.employee.entity.Role;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String ADMIN_EMAIL = "admin@example.com";
    private static final String EMPLOYEE_EMAIL = "employee@example.com";
    private static final String PASSWORD = "password123";

    @BeforeEach
    void setUp() {
        var department = Department.builder()
            .id(UUID.randomUUID())
            .name("開発部")
            .build();
        entityManager.persist(department);

        var admin = Employee.builder()
            .id(UUID.randomUUID())
            .name("管理者")
            .email(ADMIN_EMAIL)
            .password(passwordEncoder.encode(PASSWORD))
            .department(department)
            .role(Role.ADMIN)
            .isManager(false)
            .hireDate(LocalDate.of(2024, 1, 1))
            .build();
        entityManager.persist(admin);

        var employee = Employee.builder()
            .id(UUID.randomUUID())
            .name("一般社員")
            .email(EMPLOYEE_EMAIL)
            .password(passwordEncoder.encode(PASSWORD))
            .department(department)
            .role(Role.EMPLOYEE)
            .isManager(true)
            .hireDate(LocalDate.of(2024, 4, 1))
            .build();
        entityManager.persist(employee);

        var retiredEmployee = Employee.builder()
            .id(UUID.randomUUID())
            .name("退職者")
            .email("retired@example.com")
            .password(passwordEncoder.encode(PASSWORD))
            .department(department)
            .role(Role.EMPLOYEE)
            .isManager(false)
            .hireDate(LocalDate.of(2020, 4, 1))
            .retireDate(LocalDate.of(2024, 12, 31))
            .build();
        entityManager.persist(retiredEmployee);
        entityManager.flush();
    }

    @Test
    @DisplayName("正しい認証情報でログインすると200とユーザー情報が返される")
    void login_validCredentials_returns200() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson(ADMIN_EMAIL, PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("管理者"))
            .andExpect(jsonPath("$.email").value(ADMIN_EMAIL))
            .andExpect(jsonPath("$.role").value("ADMIN"))
            .andExpect(jsonPath("$.isManager").value(false));
    }

    @Test
    @DisplayName("EMPLOYEE（マネージャー）でログインするとmanager=trueが返される")
    void login_manager_returnsManagerTrue() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson(EMPLOYEE_EMAIL, PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("EMPLOYEE"))
            .andExpect(jsonPath("$.isManager").value(true));
    }

    @Test
    @DisplayName("誤ったパスワードでログインすると401が返される")
    void login_wrongPassword_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson(ADMIN_EMAIL, "wrongpassword")))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("存在しないメールアドレスでログインすると401が返される")
    void login_nonExistentEmail_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson("nobody@example.com", PASSWORD)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("退職済み社員はログインできない（401が返される）")
    void login_retiredEmployee_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson("retired@example.com", PASSWORD)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("ログイン後に/api/auth/meで自分の情報を取得できる")
    void loginThenGetMe_returnsUserInfo() throws Exception {
        var loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson(ADMIN_EMAIL, PASSWORD)))
            .andExpect(status().isOk())
            .andReturn();

        var session = (MockHttpSession) Objects.requireNonNull(
            loginResult.getRequest().getSession());

        mockMvc.perform(get("/api/auth/me").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("管理者"))
            .andExpect(jsonPath("$.email").value(ADMIN_EMAIL));
    }

    @Test
    @DisplayName("未認証で/api/auth/meにアクセスすると401が返される")
    void getMe_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("ログアウト後にセッションが無効化される")
    void logout_invalidatesSession() throws Exception {
        var loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson(ADMIN_EMAIL, PASSWORD)))
            .andExpect(status().isOk())
            .andReturn();

        var session = (MockHttpSession) Objects.requireNonNull(
            loginResult.getRequest().getSession());

        mockMvc.perform(post("/api/auth/logout").session(session).with(csrf()))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/auth/me").session(session))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("ログイン→保護エンドポイント→ログアウト→再アクセス拒否の完全フロー")
    void fullAuthFlow_loginAccessLogoutDeny() throws Exception {
        var loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson(ADMIN_EMAIL, PASSWORD)))
            .andExpect(status().isOk())
            .andReturn();

        var session = (MockHttpSession) Objects.requireNonNull(
            loginResult.getRequest().getSession());

        mockMvc.perform(get("/api/departments").session(session))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/logout").session(session).with(csrf()))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/departments").session(session))
            .andExpect(status().isUnauthorized());
    }

    private String loginJson(String email, String password) {
        return "{\"email\": \"%s\", \"password\": \"%s\"}".formatted(email, password);
    }
}
