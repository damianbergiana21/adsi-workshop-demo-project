package com.example.attendance.employee;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EmployeeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockHttpSession adminSession;
    private MockHttpSession employeeSession;
    private UUID departmentId;
    private UUID adminId;
    private UUID employeeId;

    @BeforeEach
    void setUp() throws Exception {
        departmentId = UUID.randomUUID();
        var department = Department.builder()
            .id(departmentId)
            .name("開発部")
            .build();
        entityManager.persist(department);

        adminId = UUID.randomUUID();
        var admin = Employee.builder()
            .id(adminId)
            .name("管理者")
            .email("admin@example.com")
            .password(passwordEncoder.encode("password123"))
            .department(department)
            .role(Role.ADMIN)
            .isManager(false)
            .hireDate(LocalDate.of(2024, 1, 1))
            .build();
        entityManager.persist(admin);

        employeeId = UUID.randomUUID();
        var employee = Employee.builder()
            .id(employeeId)
            .name("一般社員")
            .email("employee@example.com")
            .password(passwordEncoder.encode("password123"))
            .department(department)
            .role(Role.EMPLOYEE)
            .isManager(false)
            .hireDate(LocalDate.of(2024, 4, 1))
            .build();
        entityManager.persist(employee);
        entityManager.flush();

        adminSession = login("admin@example.com", "password123");
        employeeSession = login("employee@example.com", "password123");
    }

    @Test
    @DisplayName("ADMIN: 社員一覧を取得すると社員が返される")
    void findAll_asAdmin_returnsEmployees() throws Exception {
        mockMvc.perform(get("/api/employees").session(adminSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("ADMIN: 社員IDで検索すると社員情報が返される")
    void findById_asAdmin_returnsEmployee() throws Exception {
        mockMvc.perform(get("/api/employees/{id}", employeeId).session(adminSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("一般社員"))
            .andExpect(jsonPath("$.email").value("employee@example.com"))
            .andExpect(jsonPath("$.departmentName").value("開発部"))
            .andExpect(jsonPath("$.role").value("EMPLOYEE"));
    }

    @Test
    @DisplayName("ADMIN: 新しい社員を作成すると201が返されパスワードはハッシュ化される")
    void create_asAdmin_returns201() throws Exception {
        var body = """
            {
                "name": "新入社員",
                "email": "new@example.com",
                "password": "password123",
                "departmentId": "%s",
                "role": "EMPLOYEE",
                "hireDate": "2024-04-01"
            }
            """.replace("%s", departmentId.toString());

        mockMvc.perform(post("/api/employees")
                .session(adminSession)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("新入社員"))
            .andExpect(jsonPath("$.email").value("new@example.com"))
            .andExpect(jsonPath("$.departmentId").value(departmentId.toString()))
            .andExpect(jsonPath("$.role").value("EMPLOYEE"));
    }

    @Test
    @DisplayName("ADMIN: 重複メールアドレスで社員を作成すると409が返される")
    void create_duplicateEmail_returns409() throws Exception {
        var body = """
            {
                "name": "別の人",
                "email": "employee@example.com",
                "password": "password123",
                "departmentId": "%s",
                "role": "EMPLOYEE",
                "hireDate": "2024-04-01"
            }
            """.replace("%s", departmentId.toString());

        mockMvc.perform(post("/api/employees")
                .session(adminSession)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("ADMIN: 存在しない部署IDで社員を作成すると404が返される")
    void create_nonExistentDepartment_returns404() throws Exception {
        var body = """
            {
                "name": "新入社員",
                "email": "new@example.com",
                "password": "password123",
                "departmentId": "%s",
                "role": "EMPLOYEE",
                "hireDate": "2024-04-01"
            }
            """.replace("%s", UUID.randomUUID().toString());

        mockMvc.perform(post("/api/employees")
                .session(adminSession)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("ADMIN: 社員情報を更新すると更新後の情報が返される")
    void update_asAdmin_returnsUpdated() throws Exception {
        var body = """
            {
                "name": "更新社員",
                "email": "updated@example.com",
                "departmentId": "%s",
                "role": "EMPLOYEE",
                "hireDate": "2024-04-01"
            }
            """.replace("%s", departmentId.toString());

        mockMvc.perform(put("/api/employees/{id}", employeeId)
                .session(adminSession)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("更新社員"))
            .andExpect(jsonPath("$.email").value("updated@example.com"));
    }

    @Test
    @DisplayName("ADMIN: 他の社員と同じメールアドレスに更新すると409が返される")
    void update_duplicateEmail_returns409() throws Exception {
        var body = """
            {
                "name": "一般社員",
                "email": "admin@example.com",
                "departmentId": "%s",
                "role": "EMPLOYEE",
                "hireDate": "2024-04-01"
            }
            """.replace("%s", departmentId.toString());

        mockMvc.perform(put("/api/employees/{id}", employeeId)
                .session(adminSession)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("ADMIN: 社員を退職させると退職日が設定される")
    void retire_asAdmin_setsRetireDate() throws Exception {
        mockMvc.perform(patch("/api/employees/{id}/retire", employeeId)
                .session(adminSession)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"retireDate\": \"2025-03-31\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.retireDate").value("2025-03-31"));
    }

    @Test
    @DisplayName("ADMIN: 既に退職済みの社員を再度退職させると409が返される")
    void retire_alreadyRetired_returns409() throws Exception {
        mockMvc.perform(patch("/api/employees/{id}/retire", employeeId)
                .session(adminSession)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"retireDate\": \"2025-03-31\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(patch("/api/employees/{id}/retire", employeeId)
                .session(adminSession)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"retireDate\": \"2025-06-30\"}"))
            .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("ADMIN: 社員をマネージャーに設定できる")
    void setManager_asAdmin_setsManagerTrue() throws Exception {
        mockMvc.perform(patch("/api/employees/{id}/manager", employeeId)
                .session(adminSession)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"isManager\": true}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isManager").value(true));
    }

    @Test
    @DisplayName("ADMIN: 同じ部署に既にマネージャーがいる場合409が返される")
    void setManager_departmentAlreadyHasManager_returns409() throws Exception {
        mockMvc.perform(patch("/api/employees/{id}/manager", employeeId)
                .session(adminSession)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"isManager\": true}"))
            .andExpect(status().isOk());

        mockMvc.perform(patch("/api/employees/{id}/manager", adminId)
                .session(adminSession)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"isManager\": true}"))
            .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("EMPLOYEE: 社員一覧にアクセスすると403が返される")
    void findAll_asEmployee_returns403() throws Exception {
        mockMvc.perform(get("/api/employees").session(employeeSession))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("EMPLOYEE: 社員の作成は403が返される")
    void create_asEmployee_returns403() throws Exception {
        var body = """
            {
                "name": "新入社員",
                "email": "new@example.com",
                "password": "password123",
                "departmentId": "%s",
                "role": "EMPLOYEE",
                "hireDate": "2024-04-01"
            }
            """.replace("%s", departmentId.toString());

        mockMvc.perform(post("/api/employees")
                .session(employeeSession)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN: バリデーションエラー（メール形式不正）で400が返される")
    void create_invalidEmail_returns400() throws Exception {
        var body = """
            {
                "name": "新入社員",
                "email": "not-an-email",
                "password": "password123",
                "departmentId": "%s",
                "role": "EMPLOYEE",
                "hireDate": "2024-04-01"
            }
            """.replace("%s", departmentId.toString());

        mockMvc.perform(post("/api/employees")
                .session(adminSession)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
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
