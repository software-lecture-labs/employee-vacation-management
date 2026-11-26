package com.dogus.vacation.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import com.dogus.vacation.AbstractIntegrationTest;
import com.dogus.vacation.domain.model.AnnualAccrualRun;
import com.dogus.vacation.domain.model.Department;
import com.dogus.vacation.domain.model.Employee;
import com.dogus.vacation.domain.model.LeaveBalance;
import com.dogus.vacation.domain.model.enums.Role;
import com.dogus.vacation.infrastructure.persistence.AnnualAccrualRunRepository;
import com.dogus.vacation.infrastructure.persistence.DepartmentRepository;
import com.dogus.vacation.infrastructure.persistence.EmployeeRepository;
import com.dogus.vacation.infrastructure.persistence.LeaveBalanceRepository;
import com.dogus.vacation.support.TestData;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Bakiye uç noktalarını uçtan uca doğrular (FR-32, FR-34): gerçek giriş ve JWT, güvenlik
 * filtreleri, mediator, Handler ve PostgreSQL birlikte çalışır.
 *
 * <p>Kurulum: A departmanında İK, yönetici ve yöneticiye bağlı bir çalışan; B departmanında
 * yöneticisi olmayan başka bir çalışan. Bağlı çalışanın bu yıl için yıllık (14 gün, 3'ü
 * kullanılmış) ve mazeret (5 gün) bakiyesi, geçen yıl için de bir yıllık bakiyesi var.
 */
class LeaveBalanceControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "Sifre123!";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private LeaveBalanceRepository balanceRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private Clock clock;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private AnnualAccrualRunRepository runRepository;

    private MockMvc mockMvc;
    private int thisYear;
    private Employee hr;
    private Employee manager;
    private Employee teamMember;
    private Employee outsider;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        thisYear = LocalDate.now(clock).getYear();

        Department departmentA = departmentRepository.save(TestData.department());
        Department departmentB = departmentRepository.save(TestData.department());
        hr = saveWithPassword(TestData.employee(departmentA, Role.HR));
        manager = saveWithPassword(TestData.employee(departmentA, Role.MANAGER));
        Employee member = TestData.employee(departmentA, Role.EMPLOYEE);
        member.setManager(manager);
        teamMember = saveWithPassword(member);
        outsider = saveWithPassword(TestData.employee(departmentB, Role.EMPLOYEE));

        LeaveBalance annual = TestData.balance(teamMember, leaveType("ANNUAL"), thisYear, "14");
        annual.use(new BigDecimal("3"));
        balanceRepository.save(annual);
        balanceRepository.save(TestData.balance(teamMember, leaveType("EXCUSE"), thisYear, "5"));
        balanceRepository.save(TestData.balance(teamMember, leaveType("ANNUAL"), thisYear - 1, "14"));
    }

    // ---- GET /api/leave-balances/mine ----

    @Test
    void mineReturnsTheCallersBalancesForThisYearByDefault() throws Exception {
        mockMvc.perform(get("/api/leave-balances/mine").header(HttpHeaders.AUTHORIZATION, bearer(teamMember)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(thisYear))
                .andExpect(jsonPath("$.balances.length()").value(2))
                .andExpect(jsonPath("$.balances[0].leaveType.code").value("ANNUAL"))
                .andExpect(jsonPath("$.balances[0].entitledDays").value(14))
                .andExpect(jsonPath("$.balances[0].usedDays").value(3))
                .andExpect(jsonPath("$.balances[0].remainingDays").value(11))
                .andExpect(jsonPath("$.balances[1].leaveType.code").value("EXCUSE"))
                .andExpect(jsonPath("$.balances[1].remainingDays").value(5));
    }

    @Test
    void mineFiltersByYear() throws Exception {
        mockMvc.perform(get("/api/leave-balances/mine").param("year", String.valueOf(thisYear - 1))
                        .header(HttpHeaders.AUTHORIZATION, bearer(teamMember)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(thisYear - 1))
                .andExpect(jsonPath("$.balances.length()").value(1));
    }

    @Test
    void mineIsEmptyForSomeoneWithoutBalances() throws Exception {
        mockMvc.perform(get("/api/leave-balances/mine").header(HttpHeaders.AUTHORIZATION, bearer(outsider)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balances").isEmpty());
    }

    @Test
    void mineRejectsAYearOutOfRange() throws Exception {
        mockMvc.perform(get("/api/leave-balances/mine").param("year", "1999")
                        .header(HttpHeaders.AUTHORIZATION, bearer(teamMember)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("year"));
    }

    @Test
    void mineRequiresAToken() throws Exception {
        mockMvc.perform(get("/api/leave-balances/mine"))
                .andExpect(status().isUnauthorized());
    }

    // ---- GET /api/employees/{id}/leave-balances ----

    @Test
    void hrSeesAnyEmployeesBalances() throws Exception {
        mockMvc.perform(get("/api/employees/{id}/leave-balances", teamMember.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(hr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employee.id").value(teamMember.getId()))
                .andExpect(jsonPath("$.year").value(thisYear))
                .andExpect(jsonPath("$.balances.length()").value(2));
    }

    @Test
    void managerSeesTheirOwnTeamMember() throws Exception {
        mockMvc.perform(get("/api/employees/{id}/leave-balances", teamMember.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balances[0].remainingDays").value(11));
    }

    @Test
    void managerCannotSeeSomeoneOutsideTheirTeam() throws Exception {
        mockMvc.perform(get("/api/employees/{id}/leave-balances", outsider.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(manager)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACTION_NOT_ALLOWED"));
    }

    @Test
    void employeeRoleIsForbiddenEvenForThemselves() throws Exception {
        mockMvc.perform(get("/api/employees/{id}/leave-balances", teamMember.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(teamMember)))
                .andExpect(status().isForbidden());
    }

    @Test
    void unknownEmployeeIs404() throws Exception {
        mockMvc.perform(get("/api/employees/{id}/leave-balances", 999_999)
                        .header(HttpHeaders.AUTHORIZATION, bearer(hr)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EMPLOYEE_NOT_FOUND"));
    }

    // ---- sorgu sayısı (N+1 koruması) ----

    /**
     * Kimlik token'dan okunur, çalışan kaydı yüklenmez; bakiyeler izin türleriyle tek sorguda
     * gelir. Bakiye sayısı artsa da sorgu sayısı 1 kalmalı.
     */
    @Test
    void mineRunsASingleQueryWhateverTheNumberOfBalances() throws Exception {
        String token = bearer(teamMember);

        long statements = statementsDuring(() ->
                mockMvc.perform(get("/api/leave-balances/mine").header(HttpHeaders.AUTHORIZATION, token))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.balances.length()").value(2)));

        assertThat(statements).isEqualTo(1);
    }

    /** Hedef çalışan, işlemi yapan ve bakiyeler: en fazla 3 sorgu; bakiye başına sorgu yok. */
    @Test
    void employeeBalancesRunAtMostThreeQueries() throws Exception {
        String token = bearer(manager);

        long statements = statementsDuring(() ->
                mockMvc.perform(get("/api/employees/{id}/leave-balances", teamMember.getId())
                                .header(HttpHeaders.AUTHORIZATION, token))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.balances.length()").value(2)));

        assertThat(statements).isLessThanOrEqualTo(3);
    }

    // ---- POST /api/leave-balances/accruals ----

    /** Bağlı çalışanın bu yılın iki bakiyesi zaten açık (T4 gibi): o atlanır, diğer üçüne açılır. */
    @Test
    void hrRunsTheAnnualAccrualAndEmployeesWithBalancesAreSkipped() throws Exception {
        mockMvc.perform(post("/api/leave-balances/accruals").param("year", String.valueOf(thisYear))
                        .header(HttpHeaders.AUTHORIZATION, bearer(hr)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.year").value(thisYear))
                .andExpect(jsonPath("$.accruedEmployees").value(3))
                .andExpect(jsonPath("$.skippedEmployees").value(1))
                .andExpect(jsonPath("$.openedBalances").value(6))
                .andExpect(jsonPath("$.runAt").isNotEmpty());
        assertThat(countRows("annual_accrual_runs")).isEqualTo(1);
        assertThat(countRows("leave_balances")).isEqualTo(9);
    }

    @Test
    void managerCannotRunTheAccrual() throws Exception {
        mockMvc.perform(post("/api/leave-balances/accruals").param("year", String.valueOf(thisYear))
                        .header(HttpHeaders.AUTHORIZATION, bearer(manager)))
                .andExpect(status().isForbidden());
        assertThat(countRows("annual_accrual_runs")).isZero();
        assertThat(countRows("leave_balances")).isEqualTo(3);
    }

    @Test
    void runningTheSameYearTwiceIs409() throws Exception {
        String token = bearer(hr);
        mockMvc.perform(post("/api/leave-balances/accruals").param("year", String.valueOf(thisYear + 1))
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/leave-balances/accruals").param("year", String.valueOf(thisYear + 1))
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ACCRUAL_ALREADY_RUN"));
        assertThat(countRows("annual_accrual_runs")).isEqualTo(1);
    }

    @Test
    void accrualRejectsAYearOtherThanThisOrNext() throws Exception {
        mockMvc.perform(post("/api/leave-balances/accruals").param("year", String.valueOf(thisYear + 2))
                        .header(HttpHeaders.AUTHORIZATION, bearer(hr)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("year"));
    }

    @Test
    void accrualRequiresAYear() throws Exception {
        mockMvc.perform(post("/api/leave-balances/accruals").header(HttpHeaders.AUTHORIZATION, bearer(hr)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("year"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("Yıl zorunludur."));
    }

    // ---- POST /api/leave-balances/carry-overs (BR-12) ----

    /** Bağlı çalışanın geçen yılki 14 günü kullanılmamış: bu yılın 14 gününe eklenir. */
    @Test
    void hrCarriesLastYearsUnusedAnnualDaysToThisYear() throws Exception {
        runRepository.save(new AnnualAccrualRun(thisYear, OffsetDateTime.now(clock), hr, 4));

        mockMvc.perform(post("/api/leave-balances/carry-overs").param("fromYear", String.valueOf(thisYear - 1))
                        .header(HttpHeaders.AUTHORIZATION, bearer(hr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fromYear").value(thisYear - 1))
                .andExpect(jsonPath("$.toYear").value(thisYear))
                .andExpect(jsonPath("$.carriedEmployees").value(1))
                .andExpect(jsonPath("$.carriedDays").value(14))
                .andExpect(jsonPath("$.skippedEmployees").value(0));

        mockMvc.perform(get("/api/leave-balances/mine").header(HttpHeaders.AUTHORIZATION, bearer(teamMember)))
                .andExpect(jsonPath("$.balances[0].leaveType.code").value("ANNUAL"))
                .andExpect(jsonPath("$.balances[0].entitledDays").value(28))
                .andExpect(jsonPath("$.balances[0].remainingDays").value(25));
    }

    @Test
    void managerCannotCarryOver() throws Exception {
        runRepository.save(new AnnualAccrualRun(thisYear, OffsetDateTime.now(clock), hr, 4));

        mockMvc.perform(post("/api/leave-balances/carry-overs").param("fromYear", String.valueOf(thisYear - 1))
                        .header(HttpHeaders.AUTHORIZATION, bearer(manager)))
                .andExpect(status().isForbidden());
        assertThat(countRows("balance_transactions")).isZero();
    }

    @Test
    void carryOverNeedsThisYearsAccrual() throws Exception {
        mockMvc.perform(post("/api/leave-balances/carry-overs").param("fromYear", String.valueOf(thisYear - 1))
                        .header(HttpHeaders.AUTHORIZATION, bearer(hr)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ACCRUAL_NOT_RUN"));
    }

    @Test
    void carryOverRejectsAYearOtherThanLastYear() throws Exception {
        mockMvc.perform(post("/api/leave-balances/carry-overs").param("fromYear", String.valueOf(thisYear))
                        .header(HttpHeaders.AUTHORIZATION, bearer(hr)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("fromYear"));
    }

    // ---- yardımcılar ----

    /** Ölçülecek istek; MockMvc çağrıları checked exception fırlatabilir. */
    @FunctionalInterface
    private interface Call {
        void run() throws Exception;
    }

    /** İstek sırasında Hibernate'in hazırladığı SQL ifadesi sayısı (giriş isteği ölçüme dahil değil). */
    private long statementsDuring(Call call) throws Exception {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
        try {
            call.run();
            return statistics.getPrepareStatementCount();
        } finally {
            statistics.setStatisticsEnabled(false);
        }
    }

    private Employee saveWithPassword(Employee newEmployee) {
        newEmployee.setPasswordHash(passwordEncoder.encode(PASSWORD));
        return employeeRepository.save(newEmployee);
    }

    /** @return giriş yapıp aldığı token'la {@code Authorization} başlığının değeri */
    private String bearer(Employee who) throws Exception {
        String body = "{\"email\":\"" + who.getEmail() + "\",\"password\":\"" + PASSWORD + "\"}";
        String json = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(json, "$.accessToken");
    }
}
