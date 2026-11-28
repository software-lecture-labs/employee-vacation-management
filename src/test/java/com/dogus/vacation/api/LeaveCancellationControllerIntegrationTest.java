package com.dogus.vacation.api;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import com.dogus.vacation.AbstractIntegrationTest;
import com.dogus.vacation.domain.model.Department;
import com.dogus.vacation.domain.model.Employee;
import com.dogus.vacation.domain.model.LeaveBalance;
import com.dogus.vacation.domain.model.LeaveRequest;
import com.dogus.vacation.domain.model.LeaveType;
import com.dogus.vacation.domain.model.enums.Role;
import com.dogus.vacation.infrastructure.persistence.DepartmentRepository;
import com.dogus.vacation.infrastructure.persistence.EmployeeRepository;
import com.dogus.vacation.infrastructure.persistence.LeaveBalanceRepository;
import com.dogus.vacation.infrastructure.persistence.LeaveRequestRepository;
import com.dogus.vacation.infrastructure.persistence.LeaveTypeRepository;
import com.dogus.vacation.support.TestData;
import com.jayway.jsonpath.JsonPath;
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
 * İptal uç noktalarını uçtan uca doğrular: gerçek giriş ve JWT, güvenlik filtreleri, mediator,
 * Handler ve PostgreSQL birlikte çalışır. Demo 8'in akışı: çalışan iptal talebi açar, yönetici
 * kabul eder, izin iptal olur ve günler iade edilir.
 */
class LeaveCancellationControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "Sifre123!";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private LeaveTypeRepository leaveTypeRepository;

    @Autowired
    private LeaveBalanceRepository balanceRepository;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private Clock clock;

    private MockMvc mockMvc;
    private Employee manager;
    private Employee employee;
    private LeaveRequest leave;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        LocalDate today = LocalDate.now(clock);
        LeaveType annual = leaveTypeRepository.findByCode("ANNUAL").orElseThrow();
        Department department = departmentRepository.save(TestData.department());
        manager = saveWithPassword(TestData.employee(department, Role.MANAGER));
        Employee newEmployee = TestData.employee(department, Role.EMPLOYEE);
        newEmployee.setManager(manager);
        employee = saveWithPassword(newEmployee);

        LocalDate start = today.plusDays(10);
        LeaveBalance balance = TestData.balance(employee, annual, start.getYear(), "14");
        balance.use(new BigDecimal("3"));
        balanceRepository.save(balance);
        LeaveRequest approved = TestData.leaveRequest(employee, annual, start, start.plusDays(2), "3");
        approved.approve(manager, today, OffsetDateTime.now(clock));
        leave = leaveRequestRepository.save(approved);
    }

    private String url(String suffix) {
        return "/api/leave-requests/" + leave.getId() + suffix;
    }

    @Test
    void theEmployeeRequestsAndTheManagerApprovesTheCancellation() throws Exception {
        mockMvc.perform(post(url("/cancel-request")).header(HttpHeaders.AUTHORIZATION, bearer(employee))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Planlar değişti\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leaveRequest.status").value("CANCEL_REQUESTED"));

        mockMvc.perform(post(url("/cancel-request/approve")).header(HttpHeaders.AUTHORIZATION, bearer(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leaveRequest.status").value("CANCELLED"))
                .andExpect(jsonPath("$.leaveRequest.cancelledBy.id").value(manager.getId()));
    }

    @Test
    void theManagerCancelsDirectlyWithAReason() throws Exception {
        mockMvc.perform(post(url("/cancel")).header(HttpHeaders.AUTHORIZATION, bearer(manager))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Proje teslimi\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leaveRequest.status").value("CANCELLED"))
                .andExpect(jsonPath("$.leaveRequest.cancelReason").value("Proje teslimi"));
    }

    @Test
    void anEmployeeCannotUseTheCompanyCancellation() throws Exception {
        mockMvc.perform(post(url("/cancel")).header(HttpHeaders.AUTHORIZATION, bearer(employee))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Kendim\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACTION_NOT_ALLOWED"));
    }

    @Test
    void cancellingWithoutAReasonIs400() throws Exception {
        mockMvc.perform(post(url("/cancel")).header(HttpHeaders.AUTHORIZATION, bearer(manager))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("reason"));
    }

    @Test
    void rejectingWithoutABodyKeepsTheLeaveApproved() throws Exception {
        mockMvc.perform(post(url("/cancel-request")).header(HttpHeaders.AUTHORIZATION, bearer(employee))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Planlar değişti\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post(url("/cancel-request/reject")).header(HttpHeaders.AUTHORIZATION, bearer(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leaveRequest.status").value("APPROVED"));
    }

    @Test
    void withdrawingWhenThereIsNoCancellationRequestIs409() throws Exception {
        mockMvc.perform(post(url("/cancel-request/withdraw")).header(HttpHeaders.AUTHORIZATION, bearer(employee)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
    }

    @Test
    void withoutATokenIs401() throws Exception {
        mockMvc.perform(post(url("/cancel-request/withdraw")))
                .andExpect(status().isUnauthorized());
    }

    private Employee saveWithPassword(Employee newEmployee) {
        newEmployee.setPasswordHash(passwordEncoder.encode(PASSWORD));
        return employeeRepository.save(newEmployee);
    }

    private String bearer(Employee who) throws Exception {
        String body = "{\"email\":\"" + who.getEmail() + "\",\"password\":\"" + PASSWORD + "\"}";
        String json = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(json, "$.accessToken");
    }
}
