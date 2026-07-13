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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Feature #4: 備考 (memo) フィールド — Red テスト。
 * 受け入れ基準をテストとして記述。現在は全て FAIL する。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Feature #4: 備考 (memo) フィールド")
class MemoFeatureTest {

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
                .name("メモテスト社員")
                .email("memo-test@example.com")
                .password(passwordEncoder.encode("password123"))
                .department(department)
                .role(Role.EMPLOYEE)
                .isManager(false)
                .hireDate(LocalDate.of(2024, 4, 1))
                .build();
        entityManager.persist(employee);
        entityManager.flush();

        session = login("memo-test@example.com", "password123");
    }

    @Nested
    @DisplayName("AC1: 出勤・退勤打刻時に備考を保存できる")
    class ClockInOutWithMemo {

        @Test
        @DisplayName("出勤時に備考付きリクエストボディで打刻すると、レスポンスにmemoが含まれる")
        void clockIn_withMemo_returnsMemoInResponse() throws Exception {
            mockMvc.perform(post("/api/attendance/clock-in")
                    .session(session)
                    .with(csrf())
                    .contentType(APPLICATION_JSON)
                    .content("""
                        {"employeeId": "%s", "memo": "遅刻:電車遅延"}
                        """.formatted(employeeId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.memo").value("遅刻:電車遅延"));
        }

        @Test
        @DisplayName("退勤時に備考付きリクエストボディで打刻すると、レスポンスにmemoが含まれる")
        void clockOut_withMemo_returnsMemoInResponse() throws Exception {
            // Arrange: clock-in first
            mockMvc.perform(post("/api/attendance/clock-in")
                    .session(session)
                    .with(csrf())
                    .contentType(APPLICATION_JSON)
                    .content("""
                        {"employeeId": "%s"}
                        """.formatted(employeeId)))
                .andExpect(status().isCreated());

            // Act: clock-out with memo
            mockMvc.perform(post("/api/attendance/clock-out")
                    .session(session)
                    .with(csrf())
                    .contentType(APPLICATION_JSON)
                    .content("""
                        {"employeeId": "%s", "memo": "直帰:客先から帰宅"}
                        """.formatted(employeeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memo").value("直帰:客先から帰宅"));
        }

        @Test
        @DisplayName("備考なしで打刻するとmemoがnullで返される")
        void clockIn_withoutMemo_returnsNullMemo() throws Exception {
            mockMvc.perform(post("/api/attendance/clock-in")
                    .session(session)
                    .with(csrf())
                    .contentType(APPLICATION_JSON)
                    .content("""
                        {"employeeId": "%s"}
                        """.formatted(employeeId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.memo").value(org.hamcrest.Matchers.nullValue()));
        }
    }

    @Nested
    @DisplayName("AC2: 空白のみの備考はNULLとして保存される")
    class BlankMemoTrimming {

        @Test
        @DisplayName("空白のみの備考はtrimされてnullになる")
        void clockIn_blankMemo_savedAsNull() throws Exception {
            mockMvc.perform(post("/api/attendance/clock-in")
                    .session(session)
                    .with(csrf())
                    .contentType(APPLICATION_JSON)
                    .content("""
                        {"employeeId": "%s", "memo": "   "}
                        """.formatted(employeeId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.memo").value(org.hamcrest.Matchers.nullValue()));
        }
    }

    @Nested
    @DisplayName("AC4: HTMLタグのサニタイズ")
    class HtmlSanitization {

        @Test
        @DisplayName("HTMLタグを含む備考はサニタイズされて保存される")
        void clockIn_htmlInMemo_sanitized() throws Exception {
            mockMvc.perform(post("/api/attendance/clock-in")
                    .session(session)
                    .with(csrf())
                    .contentType(APPLICATION_JSON)
                    .content("""
                        {"employeeId": "%s", "memo": "<script>alert(1)</script>遅刻"}
                        """.formatted(employeeId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.memo").value("遅刻"));
        }
    }

    @Nested
    @DisplayName("バリデーション: 文字数制限")
    class MemoValidation {

        @Test
        @DisplayName("200文字の備考は受け入れられる")
        void clockIn_200chars_accepted() throws Exception {
            var memo200 = "あ".repeat(200);
            mockMvc.perform(post("/api/attendance/clock-in")
                    .session(session)
                    .with(csrf())
                    .contentType(APPLICATION_JSON)
                    .content("""
                        {"employeeId": "%s", "memo": "%s"}
                        """.formatted(employeeId, memo200)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.memo").value(memo200));
        }

        @Test
        @DisplayName("201文字の備考は400エラーになる")
        void clockIn_201chars_returns400() throws Exception {
            var memo201 = "あ".repeat(201);
            mockMvc.perform(post("/api/attendance/clock-in")
                    .session(session)
                    .with(csrf())
                    .contentType(APPLICATION_JSON)
                    .content("""
                        {"employeeId": "%s", "memo": "%s"}
                        """.formatted(employeeId, memo201)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.memo").exists());
        }
    }

    @Nested
    @DisplayName("AC5: 履歴レスポンスにmemoが含まれる")
    class HistoryContainsMemo {

        @Test
        @DisplayName("勤怠履歴のレコードにmemoフィールドが含まれる")
        void history_containsMemoField() throws Exception {
            // Arrange: clock-in with memo
            mockMvc.perform(post("/api/attendance/clock-in")
                    .session(session)
                    .with(csrf())
                    .contentType(APPLICATION_JSON)
                    .content("""
                        {"employeeId": "%s", "memo": "直行:客先"}
                        """.formatted(employeeId)))
                .andExpect(status().isCreated());

            // clock-out
            mockMvc.perform(post("/api/attendance/clock-out")
                    .session(session)
                    .with(csrf())
                    .contentType(APPLICATION_JSON)
                    .content("""
                        {"employeeId": "%s"}
                        """.formatted(employeeId)))
                .andExpect(status().isOk());

            // Act: get today status
            mockMvc.perform(get("/api/attendance/today")
                    .session(session)
                    .param("employeeId", employeeId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].memo").value("直行:客先"));
        }
    }

    @Nested
    @DisplayName("Edge case: 絵文字・改行")
    class SpecialCharacters {

        @Test
        @DisplayName("絵文字を含む備考が正常に保存される")
        void clockIn_emojiMemo_accepted() throws Exception {
            mockMvc.perform(post("/api/attendance/clock-in")
                    .session(session)
                    .with(csrf())
                    .contentType(APPLICATION_JSON)
                    .content("""
                        {"employeeId": "%s", "memo": "🚃電車遅延で遅刻"}
                        """.formatted(employeeId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.memo").value("🚃電車遅延で遅刻"));
        }

        @Test
        @DisplayName("改行を含む備考が正常に保存される")
        void clockIn_newlineMemo_accepted() throws Exception {
            mockMvc.perform(post("/api/attendance/clock-in")
                    .session(session)
                    .with(csrf())
                    .contentType(APPLICATION_JSON)
                    .content("{\"employeeId\": \"%s\", \"memo\": \"理由:\\n電車遅延\"}".formatted(employeeId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.memo").value("理由:\n電車遅延"));
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
