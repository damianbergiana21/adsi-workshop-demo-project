package com.example.attendance.attendance;

import com.example.attendance.department.entity.Department;
import com.example.attendance.employee.entity.Employee;
import com.example.attendance.employee.entity.Role;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Bug #1: 出勤二重打刻")
class DoubleClockInBugTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockHttpSession session;
    private UUID employeeId;

    @BeforeEach
    void setUp() throws Exception {
        var department = Department.builder()
                .id(UUID.randomUUID())
                .name("テスト部")
                .build();
        entityManager.persist(department);

        employeeId = UUID.randomUUID();
        var employee = Employee.builder()
                .id(employeeId)
                .name("テスト社員")
                .email("test-double-clockin@example.com")
                .password(passwordEncoder.encode("password123"))
                .department(department)
                .role(Role.EMPLOYEE)
                .isManager(false)
                .hireDate(LocalDate.of(2024, 4, 1))
                .build();
        entityManager.persist(employee);
        entityManager.flush();

        session = login("test-double-clockin@example.com", "password123");
    }

    @Nested
    @DisplayName("二重出勤の防止")
    class PreventDoubleClockIn {

        @Test
        @DisplayName("出勤済みの状態で再度出勤すると409 Conflictが返される")
        void clockIn_alreadyClockedIn_shouldReturn409() throws Exception {
            mockMvc.perform(post("/api/attendance/clock-in")
                    .contentType(APPLICATION_JSON)
                    .session(session)
                    .with(csrf())
                    .content("{\"employeeId\": \"" + employeeId + "\"}"))
                .andExpect(status().isCreated());

            mockMvc.perform(post("/api/attendance/clock-in")
                    .contentType(APPLICATION_JSON)
                    .session(session)
                    .with(csrf())
                    .content("{\"employeeId\": \"" + employeeId + "\"}"))
                .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("二重出勤が拒否された後の退勤")
    class ClockOutAfterRejectedDoubleClockIn {

        @Test
        @DisplayName("二重出勤が409で拒否された後も正常に退勤できる")
        void clockOut_afterRejectedDoubleClockIn_succeeds() throws Exception {
            mockMvc.perform(post("/api/attendance/clock-in")
                    .contentType(APPLICATION_JSON)
                    .session(session)
                    .with(csrf())
                    .content("{\"employeeId\": \"" + employeeId + "\"}"))
                .andExpect(status().isCreated());

            mockMvc.perform(post("/api/attendance/clock-in")
                    .contentType(APPLICATION_JSON)
                    .session(session)
                    .with(csrf())
                    .content("{\"employeeId\": \"" + employeeId + "\"}"))
                .andExpect(status().isConflict());

            mockMvc.perform(post("/api/attendance/clock-out")
                    .contentType(APPLICATION_JSON)
                    .session(session)
                    .with(csrf())
                    .content("{\"employeeId\": \"" + employeeId + "\"}"))
                .andExpect(status().isOk());
        }
    }

    private MockHttpSession login(String email, String password) throws Exception {
        var result = mockMvc.perform(post("/api/auth/login")
                .contentType(APPLICATION_JSON)
                .content("{\"email\": \"%s\", \"password\": \"%s\"}".formatted(email, password)))
            .andExpect(status().isOk())
            .andReturn();
        return (MockHttpSession) Objects.requireNonNull(result.getRequest().getSession());
    }
}
