package com.example.attendance.department;

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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DepartmentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockHttpSession adminSession;
    private MockHttpSession employeeSession;
    private UUID departmentId;

    @BeforeEach
    void setUp() throws Exception {
        departmentId = UUID.randomUUID();
        var department = Department.builder()
            .id(departmentId)
            .name("開発部")
            .build();
        entityManager.persist(department);

        var admin = Employee.builder()
            .id(UUID.randomUUID())
            .name("管理者")
            .email("admin@example.com")
            .password(passwordEncoder.encode("password123"))
            .department(department)
            .role(Role.ADMIN)
            .isManager(false)
            .hireDate(LocalDate.of(2024, 1, 1))
            .build();
        entityManager.persist(admin);

        var employee = Employee.builder()
            .id(UUID.randomUUID())
            .name("一般社員")
            .email("employee@example.com")
            .password(passwordEncoder.encode("password123"))
            .department(department)
            .role(Role.EMPLOYEE)
            .isManager(false)
            .hireDate(LocalDate.of(2024, 1, 1))
            .build();
        entityManager.persist(employee);
        entityManager.flush();

        adminSession = login("admin@example.com", "password123");
        employeeSession = login("employee@example.com", "password123");
    }

    @Test
    @DisplayName("ADMIN: 部署一覧を取得すると初期データが返される")
    void findAll_asAdmin_returnsDepartments() throws Exception {
        mockMvc.perform(get("/api/departments").session(adminSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].name").value("開発部"));
    }

    @Test
    @DisplayName("EMPLOYEE: 部署一覧を取得できる（GETは認証済みなら誰でもOK）")
    void findAll_asEmployee_returnsDepartments() throws Exception {
        mockMvc.perform(get("/api/departments").session(employeeSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("ADMIN: 新しい部署を作成すると201とレスポンスが返される")
    void create_asAdmin_returns201() throws Exception {
        mockMvc.perform(post("/api/departments")
                .session(adminSession)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"営業部\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("営業部"))
            .andExpect(jsonPath("$.id").exists());
    }

    @Test
    @DisplayName("ADMIN: 部署を作成→一覧取得で2件確認できる（E2Eフロー）")
    void createAndFindAll_asAdmin_showsNewDepartment() throws Exception {
        mockMvc.perform(post("/api/departments")
                .session(adminSession)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"人事部\"}"))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/departments").session(adminSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("ADMIN: 既存の部署名で作成すると409が返される")
    void create_duplicateName_returns409() throws Exception {
        mockMvc.perform(post("/api/departments")
                .session(adminSession)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"開発部\"}"))
            .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("ADMIN: 部署名を更新すると更新後の名前が返される")
    void update_asAdmin_returnsUpdated() throws Exception {
        mockMvc.perform(put("/api/departments/{id}", departmentId)
                .session(adminSession)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"開発推進部\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("開発推進部"));
    }

    @Test
    @DisplayName("ADMIN: 存在しない部署を更新すると404が返される")
    void update_nonExistentId_returns404() throws Exception {
        mockMvc.perform(put("/api/departments/{id}", UUID.randomUUID())
                .session(adminSession)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"新部署\"}"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("ADMIN: 他の部署と同じ名前に更新すると409が返される")
    void update_duplicateName_returns409() throws Exception {
        var otherDept = Department.builder()
            .id(UUID.randomUUID())
            .name("営業部")
            .build();
        entityManager.persist(otherDept);
        entityManager.flush();

        mockMvc.perform(put("/api/departments/{id}", departmentId)
                .session(adminSession)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"営業部\"}"))
            .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("ADMIN: 部署名が空の場合バリデーションエラーが返される")
    void create_emptyName_returns400() throws Exception {
        mockMvc.perform(post("/api/departments")
                .session(adminSession)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("EMPLOYEE: 部署の作成は403が返される")
    void create_asEmployee_returns403() throws Exception {
        mockMvc.perform(post("/api/departments")
                .session(employeeSession)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"営業部\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("EMPLOYEE: 部署の更新は403が返される")
    void update_asEmployee_returns403() throws Exception {
        mockMvc.perform(put("/api/departments/{id}", departmentId)
                .session(employeeSession)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"営業部\"}"))
            .andExpect(status().isForbidden());
    }

    private MockHttpSession login(String email, String password) throws Exception {
        var result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"%s\", \"password\": \"%s\"}".formatted(email, password)))
            .andExpect(status().isOk())
            .andReturn();
        return (MockHttpSession) Objects.requireNonNull(result.getRequest().getSession());
    }
}
