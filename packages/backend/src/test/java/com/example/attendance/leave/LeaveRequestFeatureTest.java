package com.example.attendance.leave;

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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Feature #6: 有給休暇申請機能 — Red テスト。
 * 受け入れ基準をテストとして記述。API・テーブル・サービスが未実装のため全て FAIL する。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Feature #6: 有給休暇申請")
class LeaveRequestFeatureTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockHttpSession employeeSession;
    private MockHttpSession managerSession;
    private UUID employeeId;
    private UUID managerId;

    @BeforeEach
    void setUp() throws Exception {
        var department = Department.builder()
                .id(UUID.randomUUID())
                .name("開発部")
                .build();
        entityManager.persist(department);

        employeeId = UUID.randomUUID();
        var employee = Employee.builder()
                .id(employeeId)
                .name("申請者")
                .email("leave-emp@example.com")
                .password(passwordEncoder.encode("password123"))
                .department(department)
                .role(Role.EMPLOYEE)
                .isManager(false)
                .hireDate(LocalDate.of(2023, 4, 1))
                .build();
        entityManager.persist(employee);

        managerId = UUID.randomUUID();
        var manager = Employee.builder()
                .id(managerId)
                .name("承認者")
                .email("leave-mgr@example.com")
                .password(passwordEncoder.encode("password123"))
                .department(department)
                .role(Role.EMPLOYEE)
                .isManager(true)
                .hireDate(LocalDate.of(2020, 4, 1))
                .build();
        entityManager.persist(manager);
        entityManager.flush();

        employeeSession = login("leave-emp@example.com", "password123");
        managerSession = login("leave-mgr@example.com", "password123");
    }

    // =========================================================================
    // AC1: 有給申請の作成
    // =========================================================================

    @Nested
    @DisplayName("AC1: 有給申請の作成")
    class CreateLeaveRequest {

        @Test
        @DisplayName("1日単位の有給申請が作成できる")
        void create_fullDay_returns201() throws Exception {
            mockMvc.perform(post("/api/leave-requests")
                    .contentType(APPLICATION_JSON)
                    .session(employeeSession)
                    .with(csrf())
                    .content("""
                        {
                            "startDate": "2026-08-01",
                            "endDate": "2026-08-01",
                            "leaveType": "FULL_DAY",
                            "reason": "私用"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.startDate").value("2026-08-01"))
                .andExpect(jsonPath("$.endDate").value("2026-08-01"))
                .andExpect(jsonPath("$.leaveType").value("FULL_DAY"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.reason").value("私用"));
        }

        @Test
        @DisplayName("半日（午前休）の有給申請が作成できる")
        void create_amHalf_returns201() throws Exception {
            mockMvc.perform(post("/api/leave-requests")
                    .contentType(APPLICATION_JSON)
                    .session(employeeSession)
                    .with(csrf())
                    .content("""
                        {
                            "startDate": "2026-08-01",
                            "endDate": "2026-08-01",
                            "leaveType": "AM_HALF",
                            "reason": "通院"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.leaveType").value("AM_HALF"));
        }

        @Test
        @DisplayName("複数日にまたがる連続申請ができる")
        void create_multipleDays_returns201() throws Exception {
            mockMvc.perform(post("/api/leave-requests")
                    .contentType(APPLICATION_JSON)
                    .session(employeeSession)
                    .with(csrf())
                    .content("""
                        {
                            "startDate": "2026-08-01",
                            "endDate": "2026-08-03",
                            "leaveType": "FULL_DAY",
                            "reason": "家族旅行"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.startDate").value("2026-08-01"))
                .andExpect(jsonPath("$.endDate").value("2026-08-03"));
        }

        @Test
        @DisplayName("理由なし（任意）でも申請できる")
        void create_noReason_returns201() throws Exception {
            mockMvc.perform(post("/api/leave-requests")
                    .contentType(APPLICATION_JSON)
                    .session(employeeSession)
                    .with(csrf())
                    .content("""
                        {
                            "startDate": "2026-08-01",
                            "endDate": "2026-08-01",
                            "leaveType": "FULL_DAY"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reason").doesNotExist());
        }
    }

    // =========================================================================
    // AC2: 残日数不足の拒否
    // =========================================================================

    @Nested
    @DisplayName("AC2: 残日数バリデーション")
    class LeaveBalanceValidation {

        @Test
        @DisplayName("残日数取得APIが動作する")
        void getBalance_returnsBalance() throws Exception {
            mockMvc.perform(get("/api/leave-balance")
                    .session(employeeSession)
                    .param("employeeId", employeeId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDays").isNumber())
                .andExpect(jsonPath("$.usedDays").isNumber())
                .andExpect(jsonPath("$.remainingDays").isNumber());
        }
    }

    // =========================================================================
    // AC3: 承認・却下フロー
    // =========================================================================

    @Nested
    @DisplayName("AC3: 承認・却下フロー")
    class ApprovalFlow {

        @Test
        @DisplayName("マネージャーが有給申請を承認できる")
        void approve_byManager_returns200() throws Exception {
            // Arrange: create a leave request
            var result = createLeaveRequest("2026-09-01", "2026-09-01", "FULL_DAY");
            var requestId = extractId(result);

            // Act: approve
            mockMvc.perform(patch("/api/leave-requests/" + requestId + "/approve")
                    .contentType(APPLICATION_JSON)
                    .session(managerSession)
                    .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
        }

        @Test
        @DisplayName("マネージャーが有給申請を却下できる（理由必須）")
        void reject_withReason_returns200() throws Exception {
            // Arrange
            var result = createLeaveRequest("2026-09-01", "2026-09-01", "FULL_DAY");
            var requestId = extractId(result);

            // Act: reject with reason
            mockMvc.perform(patch("/api/leave-requests/" + requestId + "/reject")
                    .contentType(APPLICATION_JSON)
                    .session(managerSession)
                    .with(csrf())
                    .content("""
                        {"rejectReason": "繁忙期のため"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectReason").value("繁忙期のため"));
        }

        @Test
        @DisplayName("却下理由なしは400エラー")
        void reject_noReason_returns400() throws Exception {
            var result = createLeaveRequest("2026-09-01", "2026-09-01", "FULL_DAY");
            var requestId = extractId(result);

            mockMvc.perform(patch("/api/leave-requests/" + requestId + "/reject")
                    .contentType(APPLICATION_JSON)
                    .session(managerSession)
                    .with(csrf())
                    .content("{}"))
                .andExpect(status().isBadRequest());
        }
    }

    // =========================================================================
    // AC4: 一覧取得
    // =========================================================================

    @Nested
    @DisplayName("AC4: 一覧取得")
    class ListRequests {

        @Test
        @DisplayName("申請者が自分の有給申請一覧を取得できる")
        void list_byEmployee_returnsRequests() throws Exception {
            createLeaveRequest("2026-09-01", "2026-09-01", "FULL_DAY");

            mockMvc.perform(get("/api/leave-requests")
                    .session(employeeSession)
                    .param("employeeId", employeeId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        }

        @Test
        @DisplayName("マネージャーが承認待ち一覧を取得できる")
        void pending_byManager_returnsPendingRequests() throws Exception {
            createLeaveRequest("2026-09-01", "2026-09-01", "FULL_DAY");

            mockMvc.perform(get("/api/leave-requests/pending")
                    .session(managerSession)
                    .param("managerId", managerId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
        }
    }

    // =========================================================================
    // AC5: バリデーション（重複・過去日・土日祝）
    // =========================================================================

    @Nested
    @DisplayName("AC5: バリデーション")
    class Validation {

        @Test
        @DisplayName("過去日の申請は拒否される")
        void create_pastDate_returns400() throws Exception {
            mockMvc.perform(post("/api/leave-requests")
                    .contentType(APPLICATION_JSON)
                    .session(employeeSession)
                    .with(csrf())
                    .content("""
                        {
                            "startDate": "2020-01-01",
                            "endDate": "2020-01-01",
                            "leaveType": "FULL_DAY"
                        }
                        """))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("同一期間の重複申請は拒否される")
        void create_duplicate_returns409() throws Exception {
            // First request
            createLeaveRequest("2026-10-01", "2026-10-01", "FULL_DAY");

            // Duplicate
            mockMvc.perform(post("/api/leave-requests")
                    .contentType(APPLICATION_JSON)
                    .session(employeeSession)
                    .with(csrf())
                    .content("""
                        {
                            "startDate": "2026-10-01",
                            "endDate": "2026-10-01",
                            "leaveType": "FULL_DAY"
                        }
                        """))
                .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("土曜日の申請は拒否される")
        void create_saturday_returns400() throws Exception {
            // 2026-08-01 is Saturday
            mockMvc.perform(post("/api/leave-requests")
                    .contentType(APPLICATION_JSON)
                    .session(employeeSession)
                    .with(csrf())
                    .content("""
                        {
                            "startDate": "2026-08-08",
                            "endDate": "2026-08-08",
                            "leaveType": "FULL_DAY"
                        }
                        """))
                .andExpect(status().isBadRequest());
        }
    }

    // =========================================================================
    // AC6: 申請キャンセル
    // =========================================================================

    @Nested
    @DisplayName("AC6: 申請キャンセル")
    class CancelRequest {

        @Test
        @DisplayName("PENDING状態の申請をキャンセルできる")
        void cancel_pending_returns200() throws Exception {
            var result = createLeaveRequest("2026-09-15", "2026-09-15", "FULL_DAY");
            var requestId = extractId(result);

            mockMvc.perform(patch("/api/leave-requests/" + requestId + "/cancel")
                    .contentType(APPLICATION_JSON)
                    .session(employeeSession)
                    .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private MvcResult createLeaveRequest(String startDate, String endDate, String leaveType) throws Exception {
        return mockMvc.perform(post("/api/leave-requests")
                .contentType(APPLICATION_JSON)
                .session(employeeSession)
                .with(csrf())
                .content("""
                    {
                        "startDate": "%s",
                        "endDate": "%s",
                        "leaveType": "%s"
                    }
                    """.formatted(startDate, endDate, leaveType)))
            .andExpect(status().isCreated())
            .andReturn();
    }

    private String extractId(MvcResult result) throws Exception {
        var body = result.getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(body, "$.id");
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
